# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
"""Disposable live UI tour: test sign-in, deterministic acceptance and optional caller-owned Ollama."""
import argparse
import datetime
import hashlib
import http.cookies
import http.cookiejar
import http.server
import json
import ipaddress
import mimetypes
import os
import pathlib
import secrets
import ssl
import socket
import shutil
import signal
import subprocess
import threading
import urllib.error
import urllib.parse
import urllib.request
import uuid
from unittest.mock import patch
from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.x509.oid import ExtendedKeyUsageOID, NameOID

import qualification_stack
from qualification_incident import Incident
from qualification_stack import ROOT, docker, http as api_http, run

MONO = ROOT.parent
SESSIONS = {}
PASSWORDS = {}
STOP = threading.Event()


def refresh_service_credentials(directory,control):
    """Renew the fixture's short-lived service JWTs; real Control still verifies every credential."""
    for groups,subject,target in [(['toolgate-olo-token-exchange'],'olo-backend',directory/'pipeline/exchange.token'),
        (['toolgate-service-olo-memory'],'olo-memory',directory/'memory/control-token'),
        (['toolgate-service-olo-rag'],'olo-rag',directory/'rag/control-token')]:
        code="import sys,json;sys.path.insert(0,'/opt/quickstart');import supervisor as s;print(s.jwt(json.loads(sys.argv[1]),sys.argv[2],directory_bound=False))"
        token=docker('exec',control,'/opt/quickstart-python/bin/python','-c',code,json.dumps(groups),subject)
        temporary=target.with_suffix('.renewing');temporary.write_text(token,encoding='utf-8');os.replace(temporary,target)


def credential_loop(directory,control):
    while not STOP.is_set():
        try:refresh_service_credentials(directory,control)
        except (OSError,subprocess.SubprocessError):print('Fixture service credential renewal unavailable; access continues to fail closed on expiry.',flush=True)
        if STOP.wait(180):break
PAGE = '''<!doctype html><html lang="en"><meta charset="utf-8"><title>OLO test sign-in gateway</title>
<style>body{margin:0;background:#111827;color:#edf2f7;font:17px system-ui;display:grid;place-items:center;min-height:100vh}
main{max-width:540px;padding:42px;border:1px solid #364152;border-radius:18px;background:#192234}h1{font-size:32px}
label{display:grid;gap:8px;margin:20px 0}input,button{padding:14px;border-radius:8px;border:1px solid #65758a;font:inherit}
button{background:#86d4bb;color:#132b25;width:100%;font-weight:700}small{color:#b5c3d7}p{line-height:1.6}</style>
<main><small>LOCAL DEMONSTRATION · TEST SIGN-IN GATEWAY</small><h1>Sign in to OLO</h1>
<p>This gateway belongs to the disposable test environment. It is not a native OLO Chat sign-in screen.</p>
<form method="post" action="/tour/login"><label>Demo username<input name="username" autocomplete="username" required></label>
<label>Password<input name="password" type="password" autocomplete="off" required></label><button>Sign in</button></form>
<p><small>Real ToolGate account verification, server-bound test identity and governed access. The identity provider and model are scripted fixtures. Fictional data only.</small></p></main></html>'''


class GalleryIncident(Incident):
    def workflow_definitions(self):
        definitions=super().workflow_definitions()
        if not getattr(self,'demo_model',None):return definitions
        origin,model=self.demo_model
        for definition in definitions:
            definition['modelProviders']=[dict(id='tour-ollama',provider='local',model=model,configuration=dict(baseUrl=origin))]
            definition['modelRouting']=[dict(id='default',defaultProviderId='tour-ollama',rules=[])]
        commander=definitions[0]['nodes'][1]['configuration']
        commander['promptTemplate']=('Available authorized agents:\n{availableAgentsJson}\n'
            'You coordinate fictional incident INC-5001. Select only agents necessary for the user request. '
            'For identification or diagnosis only, delegate only to triage-agent; do not request remediation or reporting. '
            'For recovery, delegate triage-agent, remediation-agent, then incident-report-agent in that order. '
            'Triage lists incidents and inspects host status and crash reports. Remediation consults the runbook, '
            'requests independently approved rollback, and verifies using the returned remediationId. Reporting updates the incident. '
            'Keep each delegated task under 40 words. Use only agent IDs in the catalog and give each a specific task. Respond with JSON only, shaped as '
            '{"agentCalls":[{"agentId":"<catalog agent ID>","message":"<delegated task>"}],"directResponse":null}. '
            'Never invent successful results. User request: {message}')
        # These instructions constrain the fictional scenario; the model still selects real catalog calls.
        for definition in definitions[1:]:
            definition['nodes'][1]['configuration']['promptTemplate']=(
                'You are '+definition['id']+'. Complete the delegated fictional incident stage using your authorized tools. '
                'Use actual tool results and exact schema argument names. Never invent a successful result or a remediationId. '
                'INC-5001 concerns payments-api on app-02. Rollback requires independent approval. '
                'For diagnosis, inspect incidents, host-status and crash-report. Do not invent a rollback version, '
                'remediation ID or pending approval. State that approval is required unless an actual approval was requested. '
                'Once all necessary calls return, explain the evidence and outcome in at most 100 words. {message}')
        definitions[0]['nodes'][2]['configuration']['promptTemplate']=(
            'Summarize the actual child-agent results in at most 140 words. Include incident, cause, evidence, '
            'approval and verification status only when present in the results. Never invent a rollback version '
            'or claim approval is pending during diagnosis. If no recovery was requested, state diagnosis only '
            'and independent approval required before remediation. Do not invent missing results. {agentResultsJson}')
        return definitions

    def activate_ollama(self,origin,model):
        parsed=urllib.parse.urlsplit(origin)
        if parsed.scheme not in ('http','https') or not parsed.hostname or parsed.username or parsed.password or parsed.query or parsed.fragment:
            raise ValueError('Use an HTTP(S) Ollama origin without embedded credentials')
        if parsed.path not in ('','/'):raise ValueError('Use the Ollama origin, without an API path')
        self.demo_model=(origin.rstrip('/'),model)
        # Preserve the deterministic acceptance evidence before switching the interactive demo.
        audit=self.path/'studio-audit';audit.mkdir()
        docker('cp',self.worker+':/app/olo-configuration/log/.',str(audit))
        for definition in self.workflow_definitions():
            (self.pipeline_directory/'workflows'/(definition['id']+'.json')).write_text(json.dumps(definition),encoding='utf-8')
        environment=self.pipeline_directory/'worker.env'
        values=environment.read_text().splitlines()
        environment.write_text('\n'.join(('OLO_LLM_BASE_URL='+origin.rstrip('/')) if line.startswith('OLO_LLM_BASE_URL=') else line for line in values)+'\n')
        docker('stop',self.worker)
        self.worker=self.own('olo-worker:olo-qualification','worker','--env-file',str(environment),
            '-v',str(self.pipeline_directory)+':/run/olo:ro','-v',str(audit)+':/app/olo-configuration/log')
        self.ready(self.worker,lambda:'Step 5/5 complete: Temporal workers started' in
            (lambda result:result.stdout+result.stderr)(run(['docker','logs',self.worker],text=True)),
            'Ollama interactive worker readiness',120)
        print('Interactive model switched to the requested Ollama server; deterministic acceptance remains separately labelled.',flush=True)

    def mcp(self,user,method='tools/call',params=None,operation=None,token=None,actor=None,gateway=None):
        operation=operation or str(uuid.uuid4())
        # Caller-declared correlation only; it never supplies authorization.
        meta={'io.modelcontextprotocol/protocolVersion':'2026-07-28','io.modelcontextprotocol/clientCapabilities':{},
              'io.ololabs.toolgate/sessionId':'ui-tour-fixture-checks','io.ololabs.toolgate/agentRunId':self.name}
        body={'jsonrpc':'2.0','id':operation,'method':method,'params':{**(params or {}),'_meta':meta}}
        headers={'Accept':'application/json, text/event-stream','MCP-Protocol-Version':'2026-07-28','MCP-Method':method}
        if method=='tools/call':headers['MCP-Name']=params['name']
        if actor:headers['X-ToolGate-Actor-Credential']=actor
        return api_http((gateway or self.gateway)+'/mcp',token or self.credential(user),body,'POST',headers)

    def control_environment(self):
        # Separate ephemeral operator CA. Tenant and executor credentials cannot use this listener.
        now=datetime.datetime.now(datetime.timezone.utc)
        ca_key=rsa.generate_private_key(public_exponent=65537,key_size=2048)
        subject=x509.Name([x509.NameAttribute(NameOID.COMMON_NAME,'Disposable UI tour operator CA')])
        ca=x509.CertificateBuilder().subject_name(subject).issuer_name(subject).public_key(ca_key.public_key()).serial_number(x509.random_serial_number()).not_valid_before(now-datetime.timedelta(minutes=1)).not_valid_after(now+datetime.timedelta(days=1)).add_extension(x509.BasicConstraints(ca=True,path_length=0),True).sign(ca_key,hashes.SHA256())
        trust=self.path/'trust';client=self.path/'operator-client';client.mkdir()
        pem=serialization.Encoding.PEM
        (trust/'operator-ca.crt').write_bytes(ca.public_bytes(pem))
        (client/'ca.crt').write_bytes(ca.public_bytes(pem))
        for name,folder,usage in [('operator-server',trust,ExtendedKeyUsageOID.SERVER_AUTH),('operator-client',client,ExtendedKeyUsageOID.CLIENT_AUTH)]:
            key=rsa.generate_private_key(public_exponent=65537,key_size=2048)
            builder=x509.CertificateBuilder().subject_name(x509.Name([x509.NameAttribute(NameOID.COMMON_NAME,name)])).issuer_name(subject).public_key(key.public_key()).serial_number(x509.random_serial_number()).not_valid_before(now-datetime.timedelta(minutes=1)).not_valid_after(now+datetime.timedelta(hours=8)).add_extension(x509.ExtendedKeyUsage([usage]),False)
            if usage==ExtendedKeyUsageOID.SERVER_AUTH:builder=builder.add_extension(x509.SubjectAlternativeName([x509.DNSName('control'),x509.DNSName('localhost'),x509.IPAddress(ipaddress.ip_address('127.0.0.1'))]),False)
            cert=builder.sign(ca_key,hashes.SHA256())
            (folder/(name+'.crt')).write_bytes(cert.public_bytes(pem))
            (folder/(name+'.key')).write_bytes(key.private_bytes(pem,serialization.PrivateFormat.PKCS8,serialization.NoEncryption()))
        return dict(TOOLGATE_INSIGHTS_MODE='DETAILED',TOOLGATE_INSIGHTS_SPAN_SAMPLE_RATIO='1.0',
            TOOLGATE_INSIGHTS_OPERATOR_PORT='8446',TOOLGATE_INSIGHTS_OPERATOR_HOST='0.0.0.0',
            TOOLGATE_INSIGHTS_OPERATOR_CERTIFICATE_PATH='/trust/operator-server.crt',
            TOOLGATE_INSIGHTS_OPERATOR_PRIVATE_KEY_PATH='/trust/operator-server.key',
            TOOLGATE_INSIGHTS_OPERATOR_CLIENT_CA_PATH='/trust/operator-ca.crt')

    def control_run_options(self):return ['-p','127.0.0.1::8446']


class Gateway(http.server.BaseHTTPRequestHandler):
    def log_message(self, *_):
        pass  # Never log cookies, credentials, prompts or request paths.

    def respond(self, status, body=b'', content='text/html; charset=utf-8', headers=()):
        if isinstance(body, str): body=body.encode()
        self.send_response(status)
        self.send_header('Content-Type', content)
        self.send_header('Content-Length', str(len(body)))
        self.send_header('Cache-Control', 'no-store')
        self.send_header('X-Content-Type-Options', 'nosniff')
        for key, value in headers: self.send_header(key, value)
        self.end_headers()
        try: self.wfile.write(body)
        except (BrokenPipeError, ConnectionResetError): pass

    def signed_in(self):
        cookie=http.cookies.SimpleCookie()
        try: cookie.load(self.headers.get('Cookie',''))
        except http.cookies.CookieError: return False
        value=cookie.get('olo_tour_operator' if self.server.kind=='operator' else 'olo_tour')
        expected='admin' if self.server.kind=='operator' else 'reviewer-1'
        return bool(value and SESSIONS.get(value.value)==expected)

    def do_POST(self): self.handle_request()
    def do_GET(self): self.handle_request()
    def do_PUT(self): self.handle_request()
    def do_DELETE(self): self.handle_request()

    def handle_request(self):
        stack=self.server.stack
        if self.path=='/tour/stop' and self.command=='POST' and self.signed_in():
            STOP.set()
            return self.respond(200,'Disposable UI tour is stopping.','text/plain')
        if self.path=='/tour/login':
            if self.command=='GET':
                page=PAGE
                if getattr(stack,'demo_model',None):page=page.replace('The identity provider and model are scripted fixtures.','The identity provider is a test fixture. Interactive responses use Ollama; acceptance tests use a separately labelled scripted model.')
                if self.server.kind=='operator':page=page.replace('OLO test sign-in gateway','ToolGate operator test gateway').replace('Sign in to OLO','Operator test gateway').replace('It is not a native OLO Chat sign-in screen.','It holds an ephemeral operator client certificate for the protected mTLS listener. Tenant credentials cannot access that listener directly.')
                return self.respond(200,page)
            length=int(self.headers.get('Content-Length','0'))
            if not 0<length<=4096: return self.respond(400,'Invalid sign-in request')
            form=urllib.parse.parse_qs(self.rfile.read(length).decode())
            username=form.get('username',[''])[0]
            expected='admin' if self.server.kind=='operator' else 'reviewer-1'
            if username!=expected: return self.respond(403,'Use the dedicated demo account for this gateway.')
            status,value=api_http(stack.origin+'/api/quickstart/v1/login',body=dict(username=username,password=form.get('password',[''])[0]),method='POST')
            if status!=200 or not value.get('accessToken'): return self.respond(401,'Sign-in failed. Return to the sign-in page.')
            session=secrets.token_urlsafe(32);SESSIONS[session]=username
            name='olo_tour_operator' if self.server.kind=='operator' else 'olo_tour'
            return self.respond(303,headers=[('Location','/'),('Set-Cookie',name+'='+session+'; HttpOnly; SameSite=Strict; Path=/')])
        if not self.signed_in():
            if self.path.startswith(('/api/','/runtime-api/')): return self.respond(401,'Authentication required','text/plain')
            return self.respond(303,headers=[('Location','/tour/login')])
        if self.server.kind=='operator':
            if self.command!='GET':return self.respond(405,'Read-only operator tour gateway','text/plain')
            request=urllib.request.Request(stack.operator_origin+self.path)
            try:
                with urllib.request.urlopen(request,context=stack.operator_ssl,timeout=30) as response:
                    return self.respond(response.status,response.read(),response.headers.get('Content-Type','application/json'))
            except urllib.error.HTTPError as error:
                return self.respond(error.code,error.read(),error.headers.get('Content-Type','application/json'))
        if self.path.startswith(('/api/','/runtime-api/')):
            studio=self.server.kind=='studio' and not self.path.startswith('/runtime-api/')
            target=('http://127.0.0.1:'+str(self.server.studio_port) if studio else stack.api_origin)
            path=self.path.replace('/runtime-api/','/api/',1)
            headers={'Authorization':'Bearer '+stack.api_token()}
            for key in ('Content-Type','Idempotency-Key','Accept'):
                if self.headers.get(key): headers[key]=self.headers[key]
            length=int(self.headers.get('Content-Length','0'))
            if length>6*1024*1024:return self.respond(413,'Request too large')
            body=self.rfile.read(length) if length else None
            request=urllib.request.Request(target+path,body,headers,method=self.command)
            try:
                with urllib.request.urlopen(request,context=None if studio else stack.api_ssl,timeout=45) as response:
                    return self.respond(response.status,response.read(),response.headers.get('Content-Type','application/json'))
            except urllib.error.HTTPError as error:
                return self.respond(error.code,error.read(),error.headers.get('Content-Type','application/json'))
            except (OSError,TimeoutError): return self.respond(502,'Test service unavailable','text/plain')
        root=(MONO/'olo-chat/dist' if self.server.kind=='chat' else MONO/'olo-ui/olo-ui/dist').resolve()
        candidate=(root/urllib.parse.unquote(self.path.split('?',1)[0]).lstrip('/')).resolve()
        if not candidate.is_relative_to(root):return self.respond(403,'Invalid path')
        if not candidate.is_file(): candidate=root/'index.html'
        return self.respond(200,candidate.read_bytes(),mimetypes.guess_type(str(candidate))[0] or 'application/octet-stream')


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--package',type=pathlib.Path)
    parser.add_argument('--stop',action='store_true',help='Sign in to the owned loopback gateway and gracefully remove its runtime resources')
    parser.add_argument('--renew',action='store_true',help=argparse.SUPPRESS)
    parser.add_argument('--ollama-url',help='Ollama origin reachable from the worker container; caller owns this server')
    parser.add_argument('--ollama-model',help='Installed open-source model for the interactive demonstration')
    args=parser.parse_args()
    if args.renew:
        manifest_path=ROOT/'build/qualification/runtime/gallery-session.json'
        manifest=json.loads(manifest_path.read_text(encoding='utf-8'))
        if manifest['workspace']!=str(ROOT):raise ValueError('Tour ownership mismatch')
        control=manifest['runId']
        info=json.loads(docker('inspect',control))[0]
        if (info['Config'].get('Labels') or {}).get('io.olo.qualification.owner')!='olo-knowledge':raise ValueError('Container ownership mismatch')
        folders=list(manifest_path.parent.glob(control+'-*'))
        if len(folders)!=1:raise ValueError('Tour private directory mismatch')
        record=manifest_path.parent/('fixture-process-'+str(os.getpid())+'.json')
        record.write_text(json.dumps(dict(workspace=str(ROOT),pid=os.getpid(),session=control,script='qualification_gallery.py')))
        try:
            while manifest_path.exists() and not STOP.is_set():
                refresh_service_credentials(folders[0],control)
                STOP.wait(180)
        finally:record.unlink(missing_ok=True)
        return
    if args.stop:
        manifest_path=ROOT/'build/qualification/runtime/gallery-session.json'
        manifest=json.loads(manifest_path.read_text(encoding='utf-8'))
        if manifest['workspace']!=str(ROOT) or manifest['chat']!='http://localhost:58000':
            raise ValueError('Tour ownership mismatch')
        opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
        body=urllib.parse.urlencode(dict(username='reviewer-1',password=manifest['passwords']['reviewer-1'])).encode()
        with opener.open(manifest['chat']+'/tour/login',body,timeout=30) as response:
            if response.status!=200:raise RuntimeError('Tour stop sign-in failed')
        with opener.open(manifest['chat']+'/tour/stop',b'',timeout=30) as response:
            if response.read()!=b'Disposable UI tour is stopping.':raise RuntimeError('Tour stop was not acknowledged')
        print('Owned UI tour shutdown requested; wait for its process to exit before running qualification.ps1 stop.',flush=True)
        return
    if not args.package:parser.error('--package is required when starting the tour')
    if (ROOT/'build/qualification/runtime/gallery-session.json').exists():
        raise RuntimeError('A UI tour session already exists; stop it before starting a fresh one')
    for port in (58000,58001,58002,58082):
        with socket.socket() as probe:probe.bind(('127.0.0.1',port))
    stack=GalleryIncident(args.package)
    record=json.loads(stack.process_file.read_text());record['script']='qualification_gallery.py'
    stack.process_file.write_text(json.dumps(record))
    original=qualification_stack.http
    def recording_login(url, token=None, body=None, method='GET', headers=None, context=None):
        result=original(url,token,body,method,headers,context)
        if url.endswith('/api/quickstart/v1/login') and result[0]==200 and body and body.get('newPassword'):
            PASSWORDS[body['username']]=body['newPassword']
        return result
    process=None;log=None;servers=[]
    try:
        with patch.object(qualification_stack,'http',recording_login): stack.start().start_services()
        stack.provision_incident().start_pipeline()
        threading.Thread(target=credential_loop,args=(stack.path,stack.control),daemon=True).start()
        info=json.loads(docker('inspect',stack.control))[0]
        stack.operator_origin='https://127.0.0.1:'+info['NetworkSettings']['Ports']['8446/tcp'][0]['HostPort']
        stack.operator_ssl=ssl.create_default_context(cafile=str(stack.path/'operator-client/ca.crt'))
        stack.operator_ssl.load_cert_chain(stack.path/'operator-client/operator-client.crt',stack.path/'operator-client/operator-client.key')
        with urllib.request.urlopen(stack.operator_origin+'/insights/v1/status',context=stack.operator_ssl,timeout=10) as response:
            stack.require(json.load(response)['mode']=='DETAILED','Operator Insights is not enabled')
        try:
            without_certificate=ssl.create_default_context(cafile=str(stack.path/'operator-client/ca.crt'))
            urllib.request.urlopen(stack.operator_origin+'/insights/v1/status',context=without_certificate,timeout=10)
        except OSError:
            pass
        else:
            raise AssertionError('Operator listener accepted a request without an operator certificate')
        properties=stack.pipeline_directory/'fixture.properties'
        with properties.open('a',encoding='utf-8') as output:
            output.write('\nolo.ui.user-display-name=Demo Reviewer\nolo.ui.tenant-display-name=Aurora Demo\n')
        docker('restart',stack.backend)
        backend_info=json.loads(docker('inspect',stack.backend))[0]
        stack.api_origin='https://127.0.0.1:'+backend_info['NetworkSettings']['Ports']['7080/tcp'][0]['HostPort']
        stack.wait(lambda:stack.api_call('/api/auth/me')[0]==200,'Restarted labelled demo backend',90)
        status,_=stack.api_call('/api/v1/conversations/'+stack.conversation,method='POST')
        stack.require(status==200,'Preprovisioned exact-resource demo conversation failed')
        # Only fictional, publishable replies appear in screenshots; no private test canary.
        (stack.pipeline_directory/'idp/reply.txt').write_text('Fictional INC-5001: triage, rollback and verification completed. The incident report is resolved.',encoding='utf-8')
        stack.canary='fictional-demo-acceptance'
        stack.qualify_incident()
        if bool(args.ollama_url)!=bool(args.ollama_model):raise ValueError('Supply both --ollama-url and --ollama-model')
        if args.ollama_url:stack.activate_ollama(args.ollama_url,args.ollama_model)
        config=stack.path/'studio';config.mkdir()
        shutil.copytree(MONO/'olo-definition/olo-configuration',config/'configuration',ignore=shutil.ignore_patterns('log','logs'))
        active=config/'configuration/current-active'
        for file in active.rglob('*.json'):file.unlink()
        for definition in stack.workflow_definitions():(active/(definition['id']+'.json')).write_text(json.dumps(definition),encoding='utf-8')
        studio_port=58082
        jar=next((MONO/'olo-ui/olo-be/build/libs').glob('olo-be-*.jar'))
        environment=dict(os.environ,SERVER_PORT=str(studio_port),OLO_CONFIGURATION_DIRECTORY=str(active),
            OLO_LOG_DIRECTORY=str(config/'audit'),SPRING_AUTOCONFIGURE_EXCLUDE='org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration')
        (config/'audit').mkdir()
        if args.ollama_url:
            (config/'audit').rmdir()
            audit=stack.path/'studio-audit'
            environment['OLO_LOG_DIRECTORY']=str(audit)
        else:docker('cp',stack.worker+':/app/olo-configuration/log/.',str(config/'audit'))
        log=(stack.path/'studio.log').open('w',encoding='utf-8')
        process=subprocess.Popen(['java','-jar',str(jar)],cwd=MONO,env=environment,stdout=log,stderr=log,
            creationflags=subprocess.CREATE_NO_WINDOW if os.name=='nt' else 0)
        def healthy():
            try:return api_http('http://127.0.0.1:'+str(studio_port)+'/api/v1/health')[0]==200
            except OSError:return False
        stack.wait(healthy,'Studio backend',90)
        temporal=stack.own('temporalio/ui:2.47.2','temporal-ui','-p','127.0.0.1::8080',
            '-e','TEMPORAL_ADDRESS=temporal:7233','-e','TEMPORAL_CORS_ORIGINS=http://localhost')
        temporal_port=json.loads(docker('inspect',temporal))[0]['NetworkSettings']['Ports']['8080/tcp'][0]['HostPort']
        for kind,port in [('chat',58000),('studio',58001),('operator',58002)]:
            server=http.server.ThreadingHTTPServer(('127.0.0.1',port),Gateway)
            server.stack=stack;server.kind=kind;server.studio_port=studio_port
            threading.Thread(target=server.serve_forever,daemon=True).start();servers.append(server)
        manifest=dict(runId=stack.name,chat='http://localhost:58000',studio='http://localhost:58001',
            toolgate=stack.origin,operator='http://localhost:58002',temporal='http://localhost:'+temporal_port,conversation=stack.conversation,kb=stack.kb,
            username='reviewer-1',passwords=PASSWORDS,studioPid=process.pid,pid=os.getpid(),workspace=str(ROOT),
            sourceRevision=run(['git','rev-parse','HEAD'],cwd=MONO,text=True).stdout.strip(),
            model=dict(provider='ollama',name=args.ollama_model,origin=args.ollama_url) if args.ollama_url else dict(provider='scripted-fixture'),
            scope='Disposable test sign-in gateway and scripted IdP; real ToolGate, services and Temporal. Fictional data. '
                  +('Interactive responses use Ollama; acceptance uses a separately labelled deterministic model.' if args.ollama_url else 'Scripted model.'))
        images={name:dict(imageId=info['Id'],labels=info['Config'].get('Labels') or {})
            for name in ('olo-backend:olo-qualification','olo-worker:olo-qualification',stack.image)
            for info in json.loads(docker('image','inspect',name))}
        stack.artifact('ui-tour-verification.json',json.dumps(dict(passed=True,runId=stack.name,
            sourceRevision=manifest['sourceRevision'],images=images,incidentAcceptance='incident.json',
            operatorCertificateRequired=True,scope=manifest['scope']),indent=2)+'\n')
        (ROOT/'build/qualification/runtime/gallery-session.json').write_text(json.dumps(manifest,indent=2),encoding='utf-8')
        print('READY: Chat localhost:58000; Studio localhost:58001. Private credentials are in the ignored runtime manifest.',flush=True)
        STOP.wait()
    finally:
        for server in servers:server.shutdown();server.server_close()
        if process:process.terminate();process.wait(timeout=20)
        if log:log.close()
        stack.close()
        manifest_path=ROOT/'build/qualification/runtime/gallery-session.json'
        if manifest_path.exists():manifest_path.unlink()


if __name__=='__main__':
    for name in ('SIGINT','SIGTERM'):signal.signal(getattr(signal,name),lambda *_:STOP.set())
    main()
