"""Real backend/worker/Temporal qualification with an explicitly scripted test model."""
import base64
import json
import os
import pathlib
import secrets
import ssl
import subprocess
import time
import urllib.request
import uuid
from concurrent.futures import ThreadPoolExecutor

from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import padding, rsa
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.serialization import pkcs12

from qualification_stack import ROOT, Stack, docker, http, run

TEMPORAL = 'temporalio/temporal@sha256:ad4c82c97bd12b417d1ea942610dbcd511afb250c4d5ed26c694009533df447e'


def protected_history(raw,canary):
    """Inspect encoded payload data too; JSON/base64 is not encryption."""
    needle=canary.encode()
    if canary in raw:
        return False
    def clear(value):
        if isinstance(value,dict):
            for key,item in value.items():
                if key=='data' and isinstance(item,str):
                    try:
                        if needle in base64.b64decode(item,validate=True):return True
                    except (ValueError,base64.binascii.Error):pass
                if clear(item):return True
        if isinstance(value,list):return any(clear(item) for item in value)
        return False
    return not clear(json.loads(raw)) and (base64.b64encode(b'binary/olo-aes256gcm').decode() in raw or 'binary/olo-aes256gcm' in raw)


def workflow():
    def port(name, direction):
        return dict(id=name, schema='message', type='message', direction=direction, label=name)
    return dict(id='qualification-chat', enabled=True, isDefault=True, label='Qualification chat',
        queue='olo-qualification', workflowType='olo', version='1.0.0',
        runtime=dict(contractVersion='1.0', executionModel='INLINE'),
        capability=dict(id='qualification-chat',name='Qualification chat',
                        description='Reply using user-authorized context',required_inputs=['message'],required_outputs=['ReturnValue']),
        inputs=dict(message=dict(schema='string', required=True, populateState=True)),
        nodes=[dict(id='start', type='START', ports=[port('out','OUTPUT')],
                    configuration=dict(inputVariableMappings=['message'])),
               dict(id='answer', type='AGENT', ports=[port('in','INPUT'),port('out','OUTPUT')],
                    execution=dict(executionKind='ACTIVITY',executionModel='INLINE',
                                   workflowRef=dict(workflowId='qualification-chat',version='1.0.0',inputMapping={},outputMapping={})),
                    configuration=dict(maxToolRounds=0,promptTemplate='Answer using the authorized context. {message}')),
               dict(id='end', type='END', ports=[port('in','INPUT')])],
        edges=[dict(id='start-answer',sourceNodeId='start',sourcePortId='out',targetNodeId='answer',targetPortId='in'),
               dict(id='answer-end',sourceNodeId='answer',sourcePortId='out',targetNodeId='end',targetPortId='in')],
        variables=[dict(name='message',type='string',required=True,scope='READONLY_EXTERNAL'),
                   dict(name='ReturnValue',type='string',scope='LOCAL',metadata=dict(role='return'))],
        hooks=[], agents=[], childWorkflows=[], availableAgents=[],
        modelProviders=[dict(id='fixture-model',provider='local',model='qualification-model',
                             configuration=dict(baseUrl='http://fixture-idp:11434'))],
        modelRouting=[dict(id='default',defaultProviderId='fixture-model',rules=[])],
        metadata=dict(returnOutputKey='answer',returnVariable='ReturnValue'))


class Pipeline(Stack):
    workflow_id = 'qualification-chat'
    report_name = 'pipeline.json'

    def workflow_definitions(self):
        return [workflow()]

    def configure_worker(self, config):
        return config

    def start(self):
        # Validate against the actual target image before creating Control, databases or keys.
        folder=self.path/'workflow-preflight';folder.mkdir()
        definitions=self.workflow_definitions()
        for definition in definitions:
            (folder/(definition['id']+'.json')).write_text(json.dumps(definition),encoding='utf-8')
        labels=json.loads(docker('image','inspect','olo-worker:olo-qualification'))[0]['Config'].get('Labels') or {}
        self.require(labels.get('io.olo.qualification.owner')=='olo-knowledge','Workflow preflight requires an owned worker image')
        docker('run','--rm','--label','io.olo.qualification.owner=olo-knowledge','--network','none',
            '--read-only','--tmpfs','/tmp:rw,noexec,nosuid,size=64m','-v',str(folder)+':/workflows:ro',
            '--entrypoint','java','olo-worker:olo-qualification','-cp','/app/lib/*',
            'org.olo.worker.WorkflowDefinitionCheck','/workflows')
        print('PASS offline target-worker workflow preflight ('+str(len(definitions))+' definitions)',flush=True)
        return super().start()

    def __init__(self):
        super().__init__('olo-toolgate-quickstart:olo-m8')
        self.artifact(self.report_name,json.dumps(dict(passed=False,stage='STARTING',runId=self.name))+'\n')
        record=json.loads(self.process_file.read_text())
        record['script']='qualification_pipeline.py'
        self.process_file.write_text(json.dumps(record),encoding='utf-8')

    def own(self, image, alias, *args, command=()):
        if alias in ('olo','worker'):
            labels=json.loads(docker('image','inspect',image))[0]['Config'].get('Labels') or {}
            self.require(labels.get('io.olo.qualification.owner')=='olo-knowledge',
                         'Refusing an unowned pipeline application image')
        ident=docker('run','--label','io.olo.qualification.owner=olo-knowledge','-d',
                     '--network',self.network,'--network-alias',alias,*args,image,*command)
        self.containers.append(ident)
        return ident

    def ready(self,container,probe,label,seconds=60):
        def running():
            state=json.loads(docker('inspect',container))[0]['State']
            if not state['Running']:
                raise RuntimeError(label+' container exited; inspect the sanitized run logs')
            return probe()
        return self.wait(running,label,seconds)

    def start_pipeline(self):
        directory=self.path/'pipeline';directory.mkdir()
        configs=directory/'workflows';configs.mkdir()
        for definition in self.workflow_definitions():
            (configs/(definition['id']+'.json')).write_text(json.dumps(definition),encoding='utf-8')
        idp=directory/'idp';idp.mkdir()
        key,cert=self.certificate('fixture-idp')
        (idp/'server.crt').write_bytes(cert.public_bytes(serialization.Encoding.PEM))
        (idp/'server.key').write_bytes(key.private_bytes(serialization.Encoding.PEM,serialization.PrivateFormat.PKCS8,serialization.NoEncryption()))
        self.idp_key=rsa.generate_private_key(public_exponent=65537,key_size=2048)
        public=self.idp_key.public_key().public_numbers()
        def number(value):return base64.urlsafe_b64encode(value.to_bytes((value.bit_length()+7)//8,'big')).rstrip(b'=').decode()
        (idp/'jwks.json').write_text(json.dumps({'keys':[dict(kty='RSA',kid='fixture-idp',alg='RS256',use='sig',n=number(public.n),e=number(public.e))]}))
        self.canary='qualification-private-'+secrets.token_hex(12)
        (idp/'reply.txt').write_text('Aurora launches in December. '+self.canary)
        (idp/'server.py').write_bytes((ROOT/'tools/pipeline_dependencies.py').read_bytes())
        self.idp=self.own(self.image,'fixture-idp','--entrypoint','/opt/quickstart-python/bin/python',
                          '-v',str(idp)+':/fixture:ro',command=('/fixture/server.py',))
        redis=self.own('redis:7-alpine','redis')
        self.wait(lambda:subprocess.run(['docker','exec',redis,'redis-cli','ping'],capture_output=True).returncode==0,'Identity Redis')
        volume=docker('volume','create','--label','io.olo.qualification.owner=olo-knowledge',self.name+'-temporal')
        self.model_volumes.append(volume)
        docker('run','--rm','--network','none','--user','0',
            '--label','io.olo.qualification.owner=olo-knowledge',
            '-v',volume+':/owned','--entrypoint','/opt/quickstart-python/bin/python',
            self.image,'-c',"import os; os.chown('/owned',1000,1000); os.chmod('/owned',0o700)")
        self.temporal=docker('run','--label','io.olo.qualification.owner=olo-knowledge','-d','--network',self.network,
            '--network-alias','temporal','-v',volume+':/data',TEMPORAL,'server','start-dev',
            '--ip','0.0.0.0','--headless','--db-filename','/data/history.db')
        self.containers.append(self.temporal)
        self.ready(self.temporal,lambda:subprocess.run(['docker','exec',self.temporal,'temporal','operator','cluster','health'],capture_output=True).returncode==0,'Temporal readiness',90)
        (directory/'payload-keys.json').write_text(json.dumps(dict(
            keys={'tenant-1':base64.b64encode(secrets.token_bytes(32)).decode(),'platform-1':base64.b64encode(secrets.token_bytes(32)).decode()},
            tenants={self.tenant:'tenant-1'},platform='platform-1')))
        (directory/'assertion.key').write_bytes(self.assertion_key.private_bytes(serialization.Encoding.PEM,serialization.PrivateFormat.PKCS8,serialization.NoEncryption()))
        (directory/'exchange.token').write_text(self.exchange_token)
        (directory/'ca.crt').write_bytes(self.ca_pem)
        key,cert=self.certificate('olo',extra=['localhost'],ip='127.0.0.1')
        (directory/'server.p12').write_bytes(pkcs12.serialize_key_and_certificates(b'olo',key,cert,[self.ca],serialization.BestAvailableEncryption(self.password.encode())))
        executable=pathlib.Path(os.environ['JAVA_HOME'])/'bin/keytool.exe' if os.name=='nt' else 'keytool'
        run([str(executable),'-importcert','-noprompt','-alias','fixture-root','-file',str(directory/'ca.crt'),
             '-keystore',str(directory/'public-trust.p12'),'-storetype','PKCS12','-storepass','changeit'])
        self.callback=secrets.token_urlsafe(40)
        properties={
            'server.port':'7080','server.ssl.enabled':'true','server.ssl.key-store':'file:/run/olo/server.p12',
            'server.ssl.key-store-type':'PKCS12','server.ssl.key-store-password':self.password,
            'olo.security.issuer':'https://fixture-idp:8443','olo.security.audience':'qualification-api',
            'olo.security.jwk-set-uri':'https://fixture-idp:8443/jwks.json','olo.security.store':'redis',
            'olo.security.bootstrap-memberships[0].issuer':'https://fixture-idp:8443',
            'olo.security.bootstrap-memberships[0].subject':'reviewer-1','olo.security.bootstrap-memberships[0].tenants[0]':self.tenant,
            'spring.data.redis.host':'redis','spring.data.redis.port':'6379',
            'olo.cache.host':'redis','olo.cache.port':'6379','olo.temporal.target':'temporal:7233',
            'olo.configuration.dir':'/run/olo/workflows','olo.chat.pipeline':self.workflow_id,
            'olo.chat.callback-base-url':'https://olo:7080','olo.toolgate.gateway-url':'http://control:8084/mcp',
            'olo.toolgate.credentials.enabled':'true','olo.toolgate.credentials.control-url':'https://control:8443',
            'olo.toolgate.credentials.service-token-path':'/run/olo/exchange.token',
            'olo.toolgate.credentials.assertion-issuer':'https://quickstart.local',
            'olo.toolgate.credentials.assertion-audience':'toolgate-user-exchange',
            'olo.toolgate.credentials.key-id':'qualification-backend','olo.toolgate.credentials.private-key-path':'/run/olo/assertion.key',
            'olo.temporal.payload-keys-file':'/run/olo/payload-keys.json',
            'olo.worker-callback.keys[0].id':'fixture-callback','olo.worker-callback.keys[0].secret':self.callback,
        }
        (directory/'fixture.properties').write_text('\n'.join(k+'='+v for k,v in properties.items())+'\n')
        common=dict(JAVA_TOOL_OPTIONS='-Djavax.net.ssl.trustStore=/run/olo/public-trust.p12 -Djavax.net.ssl.trustStorePassword=changeit',
                    OLO_PAYLOAD_KEYS_FILE='/run/olo/payload-keys.json',OLO_CALLBACK_KEY_ID='fixture-callback',OLO_CALLBACK_KEY=self.callback)
        backend={**common,'SPRING_CONFIG_ADDITIONAL_LOCATION':'file:/run/olo/fixture.properties',
                 'OLO_CONFIGURATION_DIR':'/run/olo/workflows'}
        envfile=directory/'backend.env';envfile.write_text('\n'.join(k+'='+v for k,v in backend.items())+'\n')
        self.backend=self.own('olo-backend:olo-qualification','olo','-p','127.0.0.1::7080','--env-file',str(envfile),'-v',str(directory)+':/run/olo:ro')
        info=json.loads(docker('inspect',self.backend))[0]
        self.api_origin='https://127.0.0.1:'+info['NetworkSettings']['Ports']['7080/tcp'][0]['HostPort']
        self.api_ssl=ssl.create_default_context(cafile=str(directory/'ca.crt'))
        def health():
            with urllib.request.urlopen(self.api_origin+'/api/health',context=self.api_ssl,timeout=5) as response:
                return response.status==200
        self.ready(self.backend,health,'Backend readiness',120)
        status,identity=self.api_call('/api/auth/me')
        self.require(status==200,'Verified OLO sign-in failed HTTP '+str(status))
        original=next(v for v in self.snapshot['identityBindings'] if v['subject']=='reviewer-1')
        self.api('/identity-bindings',{**original,'id':'olo-qualification-user','subject':identity['userId'],'revision':1},'POST')
        config=dict(id='qualification-worker',name='Owned qualification worker',server=dict(host='0.0.0.0',port=8080),
            workflowDefinitions=dict(scanFolder='/run/olo/workflows',recursive=False),
            temporal=dict(namespace='default',target='temporal:7233'),cache=dict(enabled=False),
            toolgate=dict(enabled=True,mcpUrl='http://control:8084/mcp',userCredentialsUrl='https://olo:7080',
                          delegatedAgents=True,agentMappings=[],reservedPrincipals=['olo-platform-service'],timeoutMs=20000,maxToolRounds=8))
        (directory/'worker.json').write_text(json.dumps(self.configure_worker(config)))
        worker={**common,'OLO_WORKER_CONFIG_PATH':'/run/olo/worker.json','OLO_LLM_BASE_URL':'http://fixture-idp:11434'}
        envfile=directory/'worker.env';envfile.write_text('\n'.join(k+'='+v for k,v in worker.items())+'\n')
        self.worker=self.own('olo-worker:olo-qualification','worker','--env-file',str(envfile),'-v',str(directory)+':/run/olo:ro')
        self.ready(self.worker,lambda:'Step 5/5 complete: Temporal workers started' in
                   (lambda result:result.stdout+result.stderr)(run(['docker','logs',self.worker],text=True)),
                   'Worker poller readiness',60)
        self.pipeline_directory=directory
        print('PASS real OLO IdP/JWKS/session binding and fresh backend/worker/Temporal startup',flush=True)
        return self

    def api_token(self):
        import time
        now=int(time.time())
        def encoded(value):return base64.urlsafe_b64encode(json.dumps(value,separators=(',',':')).encode()).rstrip(b'=')
        unsigned=encoded(dict(alg='RS256',kid='fixture-idp',typ='JWT'))+b'.'+encoded(dict(
            iss='https://fixture-idp:8443',aud='qualification-api',sub='reviewer-1',sid='fixture-session',iat=now,exp=now+600))
        return (unsigned+b'.'+base64.urlsafe_b64encode(self.idp_key.sign(unsigned,padding.PKCS1v15(),hashes.SHA256())).rstrip(b'=')).decode()

    def api_call(self,path,body=None,method='GET',key=None):
        headers={'Idempotency-Key':key or str(uuid.uuid4())}
        return http(self.api_origin+path,self.api_token(),body,method,headers,self.api_ssl)

    def model_calls(self):
        return json.loads(self.script("import urllib.request;print(urllib.request.urlopen('http://fixture-idp:11434/counts',timeout=5).read().decode())"))['modelCalls']

    def turn_history(self,root,turn):
        def completed():
            status,history=self.api_call(root+'/turns/'+turn)
            self.require(status==200,'Governed turn read failed HTTP '+str(status))
            messages=history.get('messages',[])
            self.require(not any(v['role'] in ('failure','cancellation') for v in messages),
                         'Governed turn failed; inspect the sanitized run logs')
            return history if any(v['role']=='assistant' for v in messages) else None
        return self.wait(completed,'Fresh governed Temporal turn',120)

    def recovered_replay(self,root,key,turn):
        original=self.execution(turn)['execution']['runId']
        before=self.model_calls()
        fresh_workflow=self.pipeline_directory/'workflows/qualification-chat.json'
        changed=json.loads(fresh_workflow.read_text())
        changed['nodes'][1]['configuration']['agentName']='another-unmapped-agent'
        fresh_workflow.write_text(json.dumps(changed),encoding='utf-8')
        docker('kill',self.worker)
        docker('start',self.worker)
        self.ready(self.worker,lambda:json.loads(docker('inspect',self.worker))[0]['State']['Running'],
                   'Restarted worker')
        docker('restart',self.backend)
        info=json.loads(docker('inspect',self.backend))[0]
        self.api_origin='https://127.0.0.1:'+info['NetworkSettings']['Ports']['7080/tcp'][0]['HostPort']
        def health():
            with urllib.request.urlopen(self.api_origin+'/api/health',context=self.api_ssl,timeout=5) as response:
                return response.status==200
        self.ready(self.backend,health,'Restarted backend',60)
        status,replayed=self.api_call(root+'/turns',{'message':self.canary},'POST',key)
        self.require(status==202 and replayed.get('runId')==turn,'Original request replay failed HTTP '+str(status))
        self.require(self.execution(turn)['execution']['runId']==original,'Replay created a new Temporal execution for a completed turn')
        history=self.turn_history(root,turn)
        self.require(len(history['messages'])==2,'Recovery duplicated durable messages')
        self.require(self.model_calls()==before,'Recovery repeated a completed model activity')
        print('PASS hard worker stop, backend restart and original-request replay preserve one turn and two messages',flush=True)

    def execution(self,turn):
        return json.loads(docker('exec',self.temporal,'temporal','workflow','describe',
            '--workflow-id','run-'+self.tenant+'~'+turn,'--output','json'))['workflowExecutionInfo']

    def concurrent_turns(self,root):
        def submit(_):
            status,value=self.api_call(root+'/turns',{'message':'Concurrent qualification request.'},'POST')
            self.require(status==202 and 'runId' in value,'Concurrent turn admission failed HTTP '+str(status))
            return value['runId']
        before=self.model_calls()
        with ThreadPoolExecutor(max_workers=2) as pool:
            turns=list(pool.map(submit,range(2)))
        self.require(len(set(turns))==2,'Distinct requests shared a turn identity')
        for turn in turns:
            history=self.turn_history(root,turn)
            self.require(sorted(v['role'] for v in history['messages'])==['assistant','user'],
                         'Concurrent turn did not retain exactly one accepted message and one response')
        status,history=self.api_call(root+'/messages')
        self.require(status==200 and len(history.get('messages',[]))==6,
                     'Concurrent turns created duplicate or missing conversation records')
        ids=[v['message_id'] for v in history['messages']]
        self.require(len(set(ids))==6 and self.model_calls()==before+2,'Concurrent turns duplicated a message or model call')
        print('PASS two concurrent distinct backend turns retain unique messages and one model call per turn',flush=True)

    def terminal(self,turn):
        def closed():
            status=self.execution(turn)['status']
            return status if 'RUNNING' not in str(status) else None
        return self.wait(closed,'Terminal governed workflow',120)

    def derived_revocation(self,root):
        doc=str(uuid.uuid4())
        status,result=self.result('reviewer-1','rag.ingest',self.kb,dict(
            docId=doc,create=True,content='The fictional Aurora project launches in December.',
            mediaType='text/plain',expectedVersion=0))
        self.require(status==200 and result.get('version')==1,'Governed source ingest did not publish version one')
        status,accepted=self.api_call(root+'/turns',{'message':'When does Aurora launch?','kbId':self.kb},'POST')
        self.require(status==202 and 'runId' in accepted,'RAG turn was not accepted')
        derived_turn=accepted['runId']
        history=self.turn_history(root,derived_turn)
        sources=[dep for message in history['messages'] if message['role']=='assistant'
                 for dep in message.get('dependencies',[]) if dep.get('docId')==doc]
        self.require(sources,'Actual assistant history did not retain its retrieved source dependency')
        held=self.pipeline_directory/'idp/hold-model'
        held.touch()
        before=self.model_calls()
        status,accepted=self.api_call(root+'/turns',{'message':'Summarize Aurora again.','kbId':self.kb},'POST')
        self.require(status==202 and 'runId' in accepted,'In-flight turn was not accepted')
        inflight=accepted['runId']
        self.wait(lambda:self.model_calls()>before,'Model held after authorized context loading',60)
        began=time.monotonic()
        status,result=self.result('reviewer-1','rag.delete',self.kb,{'docId':doc})
        self.require(status==200 and result.get('deleted') is True,'Governed source deletion failed')
        status,proof=self.result('reviewer-1','rag.query',self.kb,{
            'type':'check','sources':[{'docId':doc,'version':1}]})
        self.require(status==200 and proof.get('valid')==[],
                     'Deleted source was not explicitly excluded by the live RAG authorization check')
        held.unlink()
        outcome=self.terminal(inflight)
        self.require('FAILED' in str(outcome),'In-flight turn released a response derived from a revoked source')
        failures=docker('exec',self.temporal,'temporal','workflow','show',
            '--workflow-id','run-'+self.tenant+'~'+inflight,'--output','json')
        self.require('SecurityException' in failures,'Expected explicit authorization failure for in-flight source revocation')
        elapsed=time.monotonic()-began
        self.require(elapsed<=30,'In-flight revocation exceeded 30 seconds')
        status,stored=self.result('reviewer-1','memory.read',self.conversation,{'type':'turn','turnId':inflight})
        self.require(status==200 and not any(v['role']=='assistant' for v in stored.get('messages',[])),
                     'A revoked in-flight response was persisted as assistant history')
        status,_=self.api_call(root+'/turns/'+derived_turn)
        self.require(status==403,'Backend released an older response derived from a deleted source')
        calls=self.model_calls()
        status,accepted=self.api_call(root+'/turns',{'message':'Continue the conversation.'},'POST')
        self.require(status==202 and 'runId' in accepted,'Post-revocation turn was not accepted')
        self.require('FAILED' in str(self.terminal(accepted['runId'])),'Historical derived context was not rejected')
        self.require(self.model_calls()==calls,'Revoked historical context reached a new model call')
        print('PASS source deletion blocks in-flight release, stored derived response and next model context',flush=True)
        return dict(inflightRevocationSeconds=elapsed,derivedResponseDenied=True,nextModelBlocked=True)

    def qualify_pipeline(self):
        root='/api/v1/conversations/'+self.conversation
        status,_=self.api_call(root,method='POST')
        self.require(status==200,'Governed backend conversation creation failed HTTP '+str(status))
        key=str(uuid.uuid4())
        status,accepted=self.api_call(root+'/turns',{'message':self.canary},'POST',key)
        self.require(status==202,'Fresh turn was not accepted HTTP '+str(status))
        turn=accepted['runId']
        history=self.turn_history(root,turn)
        self.require(len(history['messages'])==2,'Turn did not record exactly one user and one assistant message')
        print('PASS fresh real backend -> Temporal worker -> governed memory -> scripted model -> governed final result',flush=True)
        raw=docker('exec',self.temporal,'temporal','workflow','show','--workflow-id','run-'+self.tenant+'~'+turn,'--output','json')
        self.require(protected_history(raw,self.canary),'Temporal payload protection failed (including decoded payload inspection)')
        self.recovered_replay(root,key,turn)
        self.concurrent_turns(root)
        revocation=self.derived_revocation(root)
        for container in (self.backend,self.worker):
            logs=run(['docker','logs',container],text=True)
            self.require(self.canary not in logs.stdout+logs.stderr,'Private turn text leaked into application logs')
        images={name:dict(imageId=info['Id'],sourceRevision=(info['Config'].get('Labels') or {}).get('io.olo.qualification.source-revision',
                            (info['Config'].get('Labels') or {}).get('org.opencontainers.image.revision')))
                for name in ('olo-backend:olo-qualification','olo-worker:olo-qualification',self.image)
                for info in json.loads(docker('image','inspect',name))}
        self.artifact('pipeline.json',json.dumps(dict(passed=True,runId=self.name,
            scope='Real backend/worker/Temporal; scripted IdP and model. Development gRPC and Gateway-Control HTTP; no M10 transport/load signoff.',
            turnId=turn,tenant=self.tenant,historyEncrypted=True,completedTurnRestartReplay=True,
            agentChangePreservesUserAuthority=True,
            concurrentDistinctTurns=True,
            revocation=revocation,images=images,
            knowledgeRevision=run(['git','rev-parse','HEAD'],cwd=ROOT,text=True).stdout.strip(),
            knowledgeWorktreeDirty=bool(run(['git','status','--porcelain'],cwd=ROOT,text=True).stdout.strip())),indent=2)+'\n')


if __name__ == '__main__':
    pipeline=Pipeline()
    try:
        pipeline.start().start_services()
        pipeline.start_pipeline().qualify_pipeline()
    except BaseException as failure:
        pipeline.artifact('pipeline.json',json.dumps(dict(passed=False,runId=pipeline.name,failureType=type(failure).__name__))+'\n')
        raise
    finally:
        pipeline.close()
