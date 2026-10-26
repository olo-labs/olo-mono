"""Disposable knowledge-store restore and database-credential rollover drill; not full M10 signoff."""
import hashlib
import json
import os
import secrets
import subprocess
import uuid

from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from qualification_stack import ROOT, Stack, docker, run


def sql(stack, statement, database='postgres'):
    return run(['docker', 'exec', '-i', stack.postgres, 'psql', '-U', 'postgres', '-d', database,
                '-v', 'ON_ERROR_STOP=1'], input=statement, text=True).stdout


def restart_service(stack, name):
    container = docker('run', '--label', 'io.olo.qualification.owner=olo-knowledge', '--rm', '-d',
        '--network', stack.network, '--network-alias', 'olo-'+name, '--user', '10001:10001',
        '--read-only', '--tmpfs', '/tmp:rw,noexec,nosuid,size=64m',
        '--env-file', stack.path/name/'service.env', '-v', str(stack.path/name)+':/run/knowledge:ro',
        '-v', str(stack.install_paths[name])+':/app:ro', 'eclipse-temurin:21-jre', '/app/bin/olo-'+name)
    stack.containers.append(container)
    stack.services[name] = container
    stack.wait(lambda: stack.script("import socket,sys\ntry:\n s=socket.create_connection((sys.argv[1],8443),timeout=1);s.close();print('ready')\nexcept OSError:\n print('waiting')", 'olo-'+name) == 'ready',
               'Restored '+name+' listener', 60)


def unknown_invocation(response):
    data=(response.get('error') or {}).get('data') or {}
    if data.get('outcome')=='OUTCOME_UNKNOWN':
        return data.get('requestId')
    content=(response.get('result') or {}).get('structuredContent') or {}
    return content.get('invocationId') if content.get('state')=='OUTCOME_UNKNOWN' else None


def unknown_outcome(stack):
    """Fail SQL after the real service permit is consumed, then repair SQL and replay the exact operation."""
    message,turn,operation=str(uuid.uuid4()),str(uuid.uuid4()),str(uuid.uuid4())
    body=dict(messageId=message,turnId=turn,role='user',content='Uncertain write probe',dependencies=[])
    sql(stack,'REVOKE INSERT ON memory_message FROM memory_app;','memory')
    try:
        status,response=stack.call('reviewer-1','memory.append',stack.conversation,body,operation)
        invocation=unknown_invocation(response)
        stack.require(invocation is not None,'Faulted downstream write did not report an explicit unknown invocation')
    finally:
        sql(stack,'GRANT INSERT ON memory_message TO memory_app;','memory')
    before=stack.api('/access/invocations/'+invocation)
    stack.require(before['state']=='OUTCOME_UNKNOWN','Control did not durably retain the unknown outcome')
    for _ in range(2):
        status,response=stack.call('reviewer-1','memory.append',stack.conversation,body,operation)
        stack.require(unknown_invocation(response)==invocation,'Replay replaced or redispatched the unknown invocation')
    after=stack.api('/access/invocations/'+invocation)
    stack.require(after['state']=='OUTCOME_UNKNOWN' and after['revision']==before['revision'],
                  'Unknown invocation changed execution state during replay')
    status,receipt=stack.result('reviewer-1','memory.read',stack.conversation,{'type':'receipt','invocationId':invocation})
    stack.require(status==200 and receipt.get('state')=='NOT_FOUND','Rolled-back fault unexpectedly has a committed receipt')
    status,history=stack.result('reviewer-1','memory.read',stack.conversation,{'type':'turn','turnId':turn})
    stack.require(status==200 and history.get('messages')==[],'Repair plus replay repeated the uncertain write')
    print('PASS real consumed-permit write failure remains OUTCOME_UNKNOWN and exact replays do not redispatch',flush=True)
    return dict(invocationId=invocation,exactReplayCount=2,redispatched=False,receipt='NOT_FOUND')


def qualification(stack):
    stack.artifact('operations.json', json.dumps(dict(passed=False, runId=stack.name, stage='STARTING'))+'\n')
    def success(value):
        status, result = value
        stack.require(status == 200 and isinstance(result, dict) and 'error' not in result and 'result' not in result,
                      'Operational governed request failed HTTP '+str(status))
        return result
    success(stack.result('reviewer-1', 'memory.append', stack.conversation, dict(type='create', create=True)))
    message, turn = str(uuid.uuid4()), str(uuid.uuid4())
    success(stack.result('reviewer-1', 'memory.append', stack.conversation,
        dict(messageId=message, turnId=turn, role='user', content='Operational restore canary', dependencies=[])))
    kept, deleted = str(uuid.uuid4()), str(uuid.uuid4())
    for doc in [kept, deleted]:
        success(stack.result('reviewer-1', 'rag.ingest', stack.kb, dict(docId=doc, create=True,
            content='The fictional Aurora project launches in December.', mediaType='text/plain', expectedVersion=0)))
    stack.require(success(stack.result('reviewer-1', 'rag.delete', stack.kb, {'docId': deleted})).get('deleted') is True,
                  'Deletion before backup did not commit')
    # Quiesce the only service writers. This is a local isolated drill, not a distributed recovery-point claim.
    for container in stack.services.values():
        docker('stop', container)
    cipher = AESGCM(AESGCM.generate_key(bit_length=256))  # Process-only drill key; never written to disk.
    archives = {}
    hashes = {}
    for name in ['memory', 'rag']:
        dump = run(['docker', 'exec', stack.postgres, 'pg_dump', '-U', 'postgres', '-Fc', name]).stdout
        nonce = secrets.token_bytes(12)
        archive = stack.path/(name+'.backup.aesgcm')
        archive.write_bytes(nonce+cipher.encrypt(nonce, dump, name.encode()))
        archives[name] = archive
        hashes[name] = hashlib.sha256(archive.read_bytes()).hexdigest()
    # Destroy only the databases in this fixture's newly owned PostgreSQL container.
    owned = json.loads(docker('inspect', stack.postgres))[0]
    stack.require((owned['Config'].get('Labels') or {}).get('io.olo.qualification.owner') == 'olo-knowledge',
                  'Refusing restore into an unowned database server')
    for name in ['memory', 'rag']:
        sql(stack, 'DROP DATABASE '+name+' WITH (FORCE); CREATE DATABASE '+name+';')
        encrypted = archives[name].read_bytes()
        dump = cipher.decrypt(encrypted[:12], encrypted[12:], name.encode())
        run(['docker', 'exec', '-i', stack.postgres, 'pg_restore', '--exit-on-error', '--no-owner', '--no-acl',
             '-U', 'postgres', '-d', name], input=dump)
        sql(stack, 'GRANT USAGE ON SCHEMA public TO '+name+'_app; GRANT SELECT,INSERT,UPDATE,DELETE ON ALL TABLES IN SCHEMA public TO '+name+'_app;', name)
        replacement = secrets.token_urlsafe(32)
        sql(stack, "ALTER ROLE "+name+"_app PASSWORD '"+replacement+"';")
        def login(password):
            return subprocess.run(['docker', 'run', '--rm', '--label', 'io.olo.qualification.owner=olo-knowledge',
                '--network', stack.network, '-e', 'PGPASSWORD', 'postgres:16', 'psql', '-h', 'postgres',
                '-U', name+'_app', '-d', name, '-v', 'ON_ERROR_STOP=1', '-c', 'SELECT 1'],
                env=dict(os.environ, PGPASSWORD=password), capture_output=True)
        old = login(stack.password)
        stack.require(old.returncode != 0 and b'password authentication failed' in old.stderr,
                      'Old database credential was not explicitly rejected')
        stack.require(login(replacement).returncode == 0, 'Replacement database credential failed')
        env = stack.path/name/'service.env'
        lines = env.read_text().splitlines()
        env.write_text('\n'.join('DATABASE_PASSWORD='+replacement if line.startswith('DATABASE_PASSWORD=') else line
                                 for line in lines)+'\n')
        restart_service(stack, name)
    history = success(stack.result('reviewer-1', 'memory.read', stack.conversation, {'type': 'turn', 'turnId': turn}))
    stack.require([item['message_id'] for item in history.get('messages', [])] == [message], 'Restored memory differs from backup')
    def scope_denials():
        log = run(['docker', 'logs', stack.services['memory']], text=True)
        return (log.stdout+log.stderr).count('authorization_denied source=org.olo.knowledge.core.Scope#denied')
    before = scope_denials()
    status, denied = stack.result('reviewer-2', 'memory.read', stack.conversation, {'type': 'turn', 'turnId': turn})
    stack.require(status != 200 or 'messages' not in denied, 'Restore widened the private conversation ACL')
    stack.require(scope_denials() > before, 'Private ACL check requires explicit service denial, not dependency failure')
    reindex = success(stack.result('reviewer-1', 'rag.update', stack.kb, {'type': 'reindex', 'docId': kept, 'force': True}))
    stack.require(reindex.get('version') == 2, 'Restored SQL source was not rebuilt as a new vector version')
    chunks = success(stack.result('reviewer-1', 'rag.query', stack.kb, {'query': 'When does Aurora launch?'}))['chunks']
    stack.require(any(chunk['doc_id'] == kept and chunk['version'] == 2 for chunk in chunks), 'Rebuilt source missing')
    stack.require(all(chunk['doc_id'] != deleted for chunk in chunks), 'Restore resurrected a tombstoned source')
    proof = success(stack.result('reviewer-1', 'rag.query', stack.kb,
                               {'type': 'check', 'sources': [{'docId': deleted, 'version': 1}]}))
    stack.require(proof.get('valid') == [], 'Restored tombstone was not enforced')
    uncertain=unknown_outcome(stack)
    stack.artifact('operations.json', json.dumps(dict(passed=True, runId=stack.name,
        encryptedBackupSha256=hashes, memoryRestored=True, privateAclPreserved=True,
        sourceReindexed=True, tombstonePreserved=True, oldDatabasePasswordsDenied=True, newDatabasePasswordsAccepted=True,
        unknownOutcome=uncertain,
        knowledgeRevision=run(['git','rev-parse','HEAD'],cwd=ROOT,text=True).stdout.strip(),
        knowledgeWorktreeDirty=bool(run(['git','status','--porcelain'],cwd=ROOT,text=True).stdout.strip()),
        scope='Quiesced knowledge databases restored into empty owned stores; SQL source rebuild and database password rollover. Excludes coordinated ToolGate/Temporal restore, other key rotations, production encryption and load; not M10 signoff.'), indent=2)+'\n')
    print('PASS encrypted knowledge backups, empty-store restore, ACL/tombstone preservation, reindex and database credential rollover', flush=True)


if __name__ == '__main__':
    stack = Stack('olo-toolgate-quickstart:olo-m8')
    record = json.loads(stack.process_file.read_text())
    record['script'] = 'qualification_operations.py'
    stack.process_file.write_text(json.dumps(record))
    try:
        stack.start().start_services()
        qualification(stack)
    except BaseException as error:
        stack.artifact('operations.json',json.dumps(dict(passed=False,runId=stack.name,failureType=type(error).__name__))+'\n')
        raise
    finally:
        stack.close()
