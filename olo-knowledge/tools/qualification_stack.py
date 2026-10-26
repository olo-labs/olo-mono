"""Owned fresh real-Control/Gateway/Tool Host fixture. Never attaches to an existing installation."""
import base64, contextlib, datetime, hashlib, importlib.util, json, os, pathlib, secrets, ssl
import re, subprocess, sys, tempfile, time, urllib.error, urllib.request, urllib.parse, uuid
from concurrent.futures import ThreadPoolExecutor
from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding, rsa
from cryptography.hazmat.primitives.serialization import pkcs12

ROOT = pathlib.Path(__file__).resolve().parents[1]
TOOLGATE = pathlib.Path(os.environ.get('TOOLGATE_SOURCE', str(ROOT.parent.parent / 'olo-toolgate')))

def policy_denied(status, response):
    if status in (401,403):return True
    if not isinstance(response,dict):return False
    code=((response.get('error') or {}).get('data') or {}).get('code')
    state=((response.get('result') or {}).get('structuredContent') or {}).get('state')
    return code in ('UNAUTHORIZED','FORBIDDEN','TENANT_MISMATCH') or state=='DENIED'

def run(args, **kwargs):
    if kwargs.get('text'):kwargs.setdefault('encoding','utf-8')
    return subprocess.run(args, check=True, capture_output=True, **kwargs)

def docker(*args):
    return run(['docker', *map(str,args)], text=True).stdout.strip()

def http(url, token=None, body=None, method='GET', headers=None, context=None):
    supplied={'Content-Type':'application/json', **(headers or {})}
    if token: supplied['Authorization']='Bearer '+token
    data=body if isinstance(body,bytes) else json.dumps(body).encode() if body is not None else None
    try:
        with urllib.request.urlopen(urllib.request.Request(url,data,supplied,method=method),timeout=30,context=context) as response:
            raw=response.read(); return response.status,json.loads(raw) if raw else None
    except urllib.error.HTTPError as response:
        raw=response.read(); return response.code,json.loads(raw) if raw else None

class Stack:
    def __init__(self, image):
        self.image=image;self.name='olo-qualification-'+secrets.token_hex(6)
        self.containers=[];self.model_volumes=[];self.backpressure_events=[];self.volume=self.name+'-control';self.network=self.name+'-network';self.trust_volume=self.name+'-trust'
        runtime=ROOT/'build/qualification/runtime';runtime.mkdir(parents=True,exist_ok=True)
        self.directory=tempfile.TemporaryDirectory(prefix=self.name+'-',dir=runtime)
        self.path=pathlib.Path(self.directory.name);self.tokens={};self.password=secrets.token_urlsafe(32)
        self.process_file=runtime/('fixture-process-'+str(os.getpid())+'.json')
        self.process_file.write_text(json.dumps({'workspace':str(ROOT),'pid':os.getpid(),'session':self.name,'script':'qualification_stack.py'}),encoding='utf-8')
        self.artifact('fixture.json',json.dumps({'passed':False,'stage':'STARTING','runId':self.name},indent=2)+'\n')
    def artifact(self,name,value):
        output=ROOT/'build/qualification'
        for folder in [output,output/self.name]:
            folder.mkdir(parents=True,exist_ok=True)
            (folder/name).write_text(value,encoding='utf-8')
    def close(self):
        def sanitized(value):
            return re.sub(r'eyJ[A-Za-z0-9_.-]{40,}', '[redacted credential]', value.replace(self.password,'[redacted]'))
        if self.containers:
            inspected=subprocess.run(['docker','inspect',*self.containers],capture_output=True,text=True,encoding='utf-8')
            try:
                resources=[dict(container=value['Id'],volumes=[mount['Name'] for mount in value.get('Mounts',[]) if mount['Type']=='volume'])
                           for value in json.loads(inspected.stdout or '[]')
                           if (value['Config'].get('Labels') or {}).get('io.olo.qualification.owner')=='olo-knowledge']
                self.artifact('fixture-resources.json',json.dumps(resources,indent=2)+'\n')
            except (ValueError,KeyError):pass
        if hasattr(self,'control'):
            output=ROOT/'build/qualification';output.mkdir(parents=True,exist_ok=True)
            log=subprocess.run(['docker','logs',self.control],capture_output=True,text=True,encoding='utf-8')
            self.artifact('control-fixture.log',sanitized(log.stdout+log.stderr))
            gateway=subprocess.run(['docker','exec',self.control,'cat','/data/run/qualification-gateway.log'],capture_output=True,text=True,encoding='utf-8')
            self.artifact('gateway-fixture.log',sanitized(gateway.stdout))
        for number,container in enumerate(self.containers):
            log=subprocess.run(['docker','logs','--tail','200',container],capture_output=True,text=True,encoding='utf-8')
            safe=sanitized(log.stdout+log.stderr)
            self.artifact('fixture-component-'+str(number)+'.log',safe)
        for container in reversed(self.containers): subprocess.run(['docker','rm','-f','-v',container],capture_output=True)
        subprocess.run(['docker','volume','rm',self.volume,self.trust_volume],capture_output=True)
        for volume in self.model_volumes:subprocess.run(['docker','volume','rm',volume],capture_output=True)
        subprocess.run(['docker','network','rm',self.network],capture_output=True)
        self.directory.cleanup()
        self.process_file.unlink(missing_ok=True)
    def exec(self,*args):return docker('exec',self.control,*args)
    def script(self,source,*args):return self.exec('/opt/quickstart-python/bin/python','-c',source,*args)
    def start(self):
        docker('network','create','--label','io.olo.qualification.owner=olo-knowledge',self.network);docker('volume','create','--label','io.olo.qualification.owner=olo-knowledge',self.volume);docker('volume','create','--label','io.olo.qualification.owner=olo-knowledge',self.trust_volume)
        trust=self.path/'trust';trust.mkdir()
        self.binding='binding-knowledge'
        self.credential_key=rsa.generate_private_key(public_exponent=65537,key_size=2048)
        self.assertion_key=rsa.generate_private_key(public_exponent=65537,key_size=2048)
        (trust/'credential.key').write_bytes(self.credential_key.private_bytes(serialization.Encoding.PEM,serialization.PrivateFormat.PKCS8,serialization.NoEncryption()))
        (trust/'assertion-public.pem').write_bytes(self.assertion_key.public_key().public_bytes(serialization.Encoding.PEM,serialization.PublicFormat.SubjectPublicKeyInfo))
        config={'TOOLGATE_SINGLE_TENANT_HOSTS':'localhost,127.0.0.1,::1,control',
            'TOOLGATE_CONTROL_USER_CREDENTIAL_ENABLED':'true','TOOLGATE_CONTROL_USER_CREDENTIAL_ISSUER':'olo-qualification',
            'TOOLGATE_CONTROL_USER_CREDENTIAL_PRIVATE_KEY_PATH':'/trust/credential.key','TOOLGATE_CONTROL_USER_CREDENTIAL_KEY_ID':'qualification-user',
            'TOOLGATE_CONTROL_USER_CREDENTIAL_BINDING_ID':self.binding,'TOOLGATE_CONTROL_USER_CREDENTIAL_SUBJECT_ISSUER':'https://quickstart.local',
            'TOOLGATE_CONTROL_USER_CREDENTIAL_SUBJECT_AUDIENCE':'toolgate-user-exchange','TOOLGATE_CONTROL_USER_CREDENTIAL_SUBJECT_KEY_ID':'qualification-backend',
            'TOOLGATE_CONTROL_USER_CREDENTIAL_SUBJECT_PUBLIC_KEY_PATH':'/trust/assertion-public.pem'}
        environment=self.path/'control.env';environment.write_text('\n'.join(k+'='+v for k,v in config.items())+'\n',encoding='utf-8')
        now=datetime.datetime.now(datetime.timezone.utc)
        self.ca_key=rsa.generate_private_key(public_exponent=65537,key_size=2048)
        subject=x509.Name([x509.NameAttribute(x509.NameOID.COMMON_NAME,'Owned OLO qualification TLS root')])
        self.ca=x509.CertificateBuilder().subject_name(subject).issuer_name(subject).public_key(self.ca_key.public_key()).serial_number(x509.random_serial_number()).not_valid_before(now-datetime.timedelta(minutes=1)).not_valid_after(now+datetime.timedelta(days=2)).add_extension(x509.BasicConstraints(ca=True,path_length=0),True).sign(self.ca_key,hashes.SHA256())
        self.ca_pem=self.ca.public_bytes(serialization.Encoding.PEM)
        (trust/'device-ca.crt').write_bytes(self.ca_pem)
        (trust/'device-ca.key').write_bytes(self.ca_key.private_bytes(serialization.Encoding.PEM,serialization.PrivateFormat.PKCS8,serialization.NoEncryption()))
        key,certificate=self.certificate('control',extra=['localhost'],ip='127.0.0.1')
        (trust/'server.crt').write_bytes(certificate.public_bytes(serialization.Encoding.PEM))
        (trust/'server.key').write_bytes(key.private_bytes(serialization.Encoding.PEM,serialization.PrivateFormat.PKCS8,serialization.NoEncryption()))
        # The bootstrap checks this set exists; tenant device trust is independently generated by Control.
        self.keytool('-importcert','-noprompt','-alias','tls-root','-file',trust/'device-ca.crt','-keystore',trust/'device-trust.p12','-storetype','PKCS12')
        docker('run','--label','io.olo.qualification.owner=olo-knowledge','--rm','--network','none','--user','0','-v',str(trust)+':/seed:ro','-v',self.trust_volume+':/trust',
            '--entrypoint','/opt/quickstart-python/bin/python',self.image,'-c',
            "import os,shutil,pathlib;p=pathlib.Path('/trust');shutil.copytree('/seed',p,dirs_exist_ok=True);os.chown(p,65532,65532);p.chmod(0o700);[(os.chown(f,65532,65532),f.chmod(0o600)) for f in p.iterdir()]")
        self.control=docker('run','--label','io.olo.qualification.owner=olo-knowledge','-d','--name',self.name,'--network',self.network,'--network-alias','control',
            '-p','127.0.0.1::8080','-p','127.0.0.1::8443','-p','127.0.0.1::8084','-p','127.0.0.1::8085',
            '-e','TOOLGATE_ALLOW_LOOPBACK_MCP_HTTP=true','--env-file',environment,'-v',self.volume+':/data',
            '-v',self.trust_volume+':/trust',self.image)
        self.containers.append(self.control)
        info=json.loads(docker('inspect',self.control))[0]
        self.origin='http://127.0.0.1:'+info['NetworkSettings']['Ports']['8080/tcp'][0]['HostPort']
        self.control_origin='https://127.0.0.1:'+info['NetworkSettings']['Ports']['8443/tcp'][0]['HostPort']
        self.control_ssl=ssl.create_default_context(cafile=str(self.path/'trust/device-ca.crt'))
        self.gateway='http://127.0.0.1:'+info['NetworkSettings']['Ports']['8084/tcp'][0]['HostPort']
        self.wait(lambda:http(self.origin+'/health/ready')[0]==200,'Control readiness',180)
        for user in ['admin','reviewer-1','reviewer-2']:
            filename='bootstrap-password'+('' if user=='admin' else '-'+user)
            password=self.exec('cat','/data/'+filename)
            status,result=http(self.origin+'/api/quickstart/v1/login',body={'username':user,'password':password,'newPassword':secrets.token_urlsafe(32)},method='POST')
            self.require(status==200,'Fixture login refused')
            self.tokens[user]=result['accessToken']
        self.snapshot=self.api('/config/export');self.tenant=self.snapshot['tenantId']
        self.cached_credentials={}
        self.exchange_token=self.script("import sys;sys.path.insert(0,'/opt/quickstart');import supervisor as s;print(s.jwt(['toolgate-olo-token-exchange'],'olo-backend',directory_bound=False))")
        for host in ['tool-host-local','tool-host-remote']:
            def pending():return next((v for v in self.api('/endpoint/enrollments')['items'] if v['deviceId']==host),None)
            enrollment=self.wait(pending,'Tool Host enrollment',60)
            review=self.api('/endpoint/enrollments/review?code='+enrollment['userCode'])
            self.api('/endpoint/enrollments/decision',{'userCode':review['userCode'],'keyFingerprint':review['keyFingerprint'],'choice':'APPROVE'},'POST')
        self.wait(lambda:self.api('/packages/provisioning/fresh')['state']=='READY','Fresh package provisioning',180)
        self.snapshot['identityBindings']=json.loads(self.script("import sys,json;sys.path.insert(0,'/opt/quickstart');import supervisor as s;db=s.Database();print(json.dumps([json.loads(r[0]) if isinstance(r[0],str) else r[0] for r in db.execute(\"SELECT document FROM control_records WHERE kind='IDENTITY_BINDING'\").fetchall()]));db.connection.close()"))
        self.configure_gateway()
        return self
    def keytool(self,*args):
        executable=pathlib.Path(os.environ['JAVA_HOME'])/'bin/keytool.exe' if os.name=='nt' and os.environ.get('JAVA_HOME') else 'keytool'
        run([str(executable),*map(str,args),'-storepass:env','OLO_QUALIFICATION_PASSWORD'],env=dict(os.environ,OLO_QUALIFICATION_PASSWORD=self.password))
    def certificate(self,name,extra=(),ip=None):
        now=datetime.datetime.now(datetime.timezone.utc);key=rsa.generate_private_key(public_exponent=65537,key_size=2048)
        names=[x509.DNSName(name),*[x509.DNSName(n) for n in extra]]
        if ip:
            import ipaddress
            names.append(x509.IPAddress(ipaddress.ip_address(ip)))
        cert=x509.CertificateBuilder().subject_name(x509.Name([x509.NameAttribute(x509.NameOID.COMMON_NAME,name)])).issuer_name(self.ca.subject).public_key(key.public_key()).serial_number(x509.random_serial_number()).not_valid_before(now-datetime.timedelta(minutes=1)).not_valid_after(now+datetime.timedelta(days=2)).add_extension(x509.SubjectAlternativeName(names),False).add_extension(x509.ExtendedKeyUsage([x509.oid.ExtendedKeyUsageOID.SERVER_AUTH]),False).sign(self.ca_key,hashes.SHA256())
        return key,cert
    @staticmethod
    def require(value,message):
        if not value:raise AssertionError(message)
    @staticmethod
    def wait(probe,label,seconds=60):
        end=time.monotonic()+seconds
        while time.monotonic()<end:
            try:
                value=probe()
                if value:return value
            except (OSError,urllib.error.URLError,KeyError):pass
            time.sleep(2)
        raise TimeoutError(label+' deadline exceeded')
    def api(self,path,body=None,method='GET',user='admin',revision=None):
        headers={'Idempotency-Key':str(uuid.uuid4())}
        if revision is not None:headers['If-Match']='"'+str(revision)+'"'
        status,value=http(self.origin+'/api/control/v1'+path,self.tokens[user],body,method,headers)
        if status in (429,503):raise ConnectionError('Control temporarily unavailable: '+path)
        self.require(status in (200,201,202,204),'Control request failed: '+path+' HTTP '+str(status))
        if status==202:
            maker=user;reviewer='reviewer-2' if user=='admin' else 'admin'
            for action,actor in [('SUBMIT',maker),('APPROVE',reviewer),('APPLY',maker)]:
                value=self.api('/configuration-changes/'+value['id']+'/transition',{'action':action,'expectedRevision':value['revision']},'POST',actor)
        return value
    def configure_gateway(self):
        public=self.path/'users.pem';public.write_bytes(self.credential_key.public_key().public_bytes(serialization.Encoding.PEM,serialization.PublicFormat.SubjectPublicKeyInfo))
        docker('cp',public,self.control+':/data/run/qualification-users.pem')
        self.binding='binding-knowledge'
        source="""import json,sys
from pathlib import Path
p=Path('/data/run/gateway.json');v=json.loads(p.read_text());v['listen']='0.0.0.0:8084';v['managementListen']='127.0.0.1:9094';v['trustedTlsProxy']=True
v['tenantHosts']=[sys.argv[1],'control:8084'];v['limits']['maxBodyBytes']=1048576
v['userCredentials']={'issuer':'olo-qualification','audience':'toolgate-gateway','keys':[{'keyId':'qualification-user','publicKeyPath':'/data/run/qualification-users.pem'}]}
Path('/data/run/qualification-gateway.json').write_text(json.dumps(v))
"""
        self.script(source,self.gateway.removeprefix('http://'))
        docker('exec','-d',self.control,'env','TOOLGATE_GATEWAY_CONFIG=/data/run/qualification-gateway.json',
            'TOOLGATE_GATEWAY_CREDENTIALS=/data/run/credentials.json','sh','-c','exec /usr/local/bin/olo-toolgate-gateway > /data/run/qualification-gateway.log 2>&1')
        def ready():
            status,result=self.mcp('reviewer-1','olo/identity')
            (ROOT/'build/qualification/gateway-auth-diagnostic.json').write_text(json.dumps({'status':status,'error':result.get('error')}),encoding='utf-8')
            return status==200
        self.wait(ready,'User Gateway readiness')
    def credential(self,user,tenant=None,epoch=None):
        if tenant is None and epoch is None:
            previous=self.cached_credentials.get(user)
            if previous and time.time()-previous[0]<180:return previous[1]
            now=int(time.time())
            def encode(v):return base64.urlsafe_b64encode(json.dumps(v,separators=(',',':')).encode()).rstrip(b'=')
            unsigned=encode({'alg':'RS256','typ':'olo-session-assertion+jwt','kid':'qualification-backend'})+b'.'+encode({'iss':'https://quickstart.local','aud':'toolgate-user-exchange','sub':user,'iat':now,'exp':now+300})
            assertion=(unsigned+b'.'+base64.urlsafe_b64encode(self.assertion_key.sign(unsigned,padding.PKCS1v15(),hashes.SHA256())).rstrip(b'=')).decode()
            form=urllib.parse.urlencode({'grant_type':'urn:ietf:params:oauth:grant-type:token-exchange','subject_token_type':'urn:ietf:params:oauth:token-type:jwt','subject_token':assertion,'audience':'toolgate-gateway'}).encode()
            status,result=http(self.control_origin+'/api/control/v1/oauth/token-exchange',self.exchange_token,form,'POST',{'Content-Type':'application/x-www-form-urlencoded'},context=self.control_ssl)
            self.require(status==200,'Real Control user token exchange refused HTTP '+str(status))
            self.cached_credentials[user]=(time.time(),result['access_token'])
            return result['access_token']
        identity=next(v for v in self.snapshot['identityBindings'] if v['subject']==user)
        now=int(time.time());claims={'iss':'olo-qualification','aud':'toolgate-gateway','sub':identity['userId'],'tid':tenant or self.tenant,'sep':epoch or identity['sessionEpoch'],'bid':self.binding,'iat':now,'exp':now+300,'jti':str(uuid.uuid4())}
        def encode(v):return base64.urlsafe_b64encode(json.dumps(v,separators=(',',':')).encode()).rstrip(b'=')
        unsigned=encode({'alg':'RS256','typ':'toolgate-user-credential+jws','kid':'qualification-user'})+b'.'+encode(claims)
        return (unsigned+b'.'+base64.urlsafe_b64encode(self.credential_key.sign(unsigned,padding.PKCS1v15(),hashes.SHA256())).rstrip(b'=')).decode()
    def mcp(self,user,method='tools/call',params=None,operation=None,token=None,actor=None,gateway=None):
        operation=operation or str(uuid.uuid4())
        body={'jsonrpc':'2.0','id':operation,'method':method,'params':{**(params or {}),'_meta':{'io.modelcontextprotocol/protocolVersion':'2026-07-28','io.modelcontextprotocol/clientCapabilities':{}}}}
        headers={'Accept':'application/json, text/event-stream','MCP-Protocol-Version':'2026-07-28','MCP-Method':method}
        if method=='tools/call':headers['MCP-Name']=params['name']
        if actor:headers['X-ToolGate-Actor-Credential']=actor
        return http((gateway or self.gateway)+'/mcp',token or self.credential(user),body,'POST',headers)
    def start_services(self):
        # Separate application databases and roles. Bootstrap SQL is sent on stdin, never in argv/logs.
        env=dict(os.environ,POSTGRES_PASSWORD=self.password)
        postgres=run(['docker','run','--label','io.olo.qualification.owner=olo-knowledge','--rm','-d','--network',self.network,'--network-alias','postgres','-e','POSTGRES_PASSWORD','postgres:16'],env=env,text=True).stdout.strip()
        self.postgres=postgres
        self.containers.append(postgres)
        self.wait(lambda:subprocess.run(['docker','exec',postgres,'pg_isready','-U','postgres'],capture_output=True).returncode==0,'Service PostgreSQL readiness')
        sql="CREATE ROLE memory_app LOGIN NOSUPERUSER NOBYPASSRLS PASSWORD '"+self.password+"'; CREATE ROLE rag_app LOGIN NOSUPERUSER NOBYPASSRLS PASSWORD '"+self.password+"'; CREATE DATABASE memory; CREATE DATABASE rag;"
        run(['docker','exec','-i',postgres,'psql','-U','postgres','-v','ON_ERROR_STOP=1'],input=sql,text=True)
        publication=json.loads(self.exec('cat','/data/tenant-identity/publication.json'))
        client_ca=next(v['certificatePem'] for v in publication['publications'][0]['keys'] if v['purpose']=='DEVICE_CA')
        self.install_paths={};self.services={}
        for name in ['memory','rag']:
            ddl=(ROOT/'deploy/common.sql').read_text(encoding='utf-8')+(ROOT/f'deploy/{name}.sql').read_text(encoding='utf-8')
            ddl+='\nGRANT USAGE ON SCHEMA public TO '+name+'_app; GRANT SELECT,INSERT,UPDATE,DELETE ON ALL TABLES IN SCHEMA public TO '+name+'_app;'
            run(['docker','exec','-i',postgres,'psql','-U','postgres','-d',name,'-v','ON_ERROR_STOP=1'],input=ddl,text=True)
            directory=self.path/name;directory.mkdir()
            (directory/'tls-root.crt').write_bytes(self.ca_pem)
            (directory/'executor-ca.crt').write_text(client_ca,encoding='utf-8')
            key,certificate=self.certificate('olo-'+name)
            (directory/'server.p12').write_bytes(pkcs12.serialize_key_and_certificates(b'identity',key,certificate,[self.ca],serialization.BestAvailableEncryption(self.password.encode())))
            for alias in ['tls-root','executor-ca']:
                self.keytool('-importcert','-noprompt','-alias',alias,'-file',directory/(alias+'.crt'),'-keystore',directory/'trust.p12','-storetype','PKCS12')
            credential=self.script("import sys;sys.path.insert(0,'/opt/quickstart');import supervisor as s;print(s.jwt(['toolgate-service-olo-"+name+"'],'olo-"+name+"',directory_bound=False))")
            (directory/'control-token').write_text(credential,encoding='utf-8')
            for endpoint in ['epoch','jwks']:
                status,_=http(self.control_origin+'/api/control/v1/access/service-permits/olo-'+name+'/'+endpoint,
                    credential,headers={'Toolgate-Tenant-ID':self.tenant},context=self.control_ssl)
                self.require(status==200,'Dedicated '+name+' Control '+endpoint+' preflight refused HTTP '+str(status))
                # Test the actual service DNS authority too. A loopback health
                # check cannot prove that Control admits the internal hostname.
                probe="import ssl,urllib.request,urllib.error\n"
                probe+="r=urllib.request.Request("+repr('https://control:8443/api/control/v1/access/service-permits/olo-'+name+'/'+endpoint)+",headers="+repr({'Authorization':'Bearer '+credential,'Toolgate-Tenant-ID':self.tenant})+")\n"
                probe+="try:\n print(urllib.request.urlopen(r,context=ssl.create_default_context(cafile='/trust/device-ca.crt'),timeout=5).status)\nexcept urllib.error.HTTPError as e:\n print(e.code)\n"
                internal=run(['docker','exec','-i',self.control,'/opt/quickstart-python/bin/python','-'],input=probe,text=True).stdout.strip()
                self.require(internal=='200','Internal '+name+' Control '+endpoint+' preflight refused HTTP '+internal)
            print('PASS authenticated '+name+' Control authorization dependencies',flush=True)
            settings=dict(TENANT_ID=self.tenant,DATABASE_URL='jdbc:postgresql://postgres:5432/'+name+'?sslmode=disable',DATABASE_USER=name+'_app',DATABASE_PASSWORD=self.password,
                TLS_KEYSTORE='/run/knowledge/server.p12',TLS_TRUSTSTORE='/run/knowledge/trust.p12',TLS_STORE_PASSWORD=self.password,CONTROL_ORIGIN='https://control:8443',CONTROL_ISSUER='control',CONTROL_CREDENTIAL_FILE='/run/knowledge/control-token')
            if name=='rag':settings.update(self.vector_settings())
            env_file=directory/'service.env';env_file.write_text('\n'.join(k+'='+v for k,v in settings.items())+'\n',encoding='utf-8')
            install=(ROOT/f'olo-{name}/build/install/olo-{name}').resolve();self.install_paths[name]=install
            self.require((install/'bin'/('olo-'+name)).exists(),'Build :olo-'+name+':installDist before qualification')
            container=docker('run','--label','io.olo.qualification.owner=olo-knowledge','--rm','-d','--network',self.network,'--network-alias','olo-'+name,'--user','10001:10001',
                '--read-only','--tmpfs','/tmp:rw,noexec,nosuid,size=64m','--env-file',env_file,'-v',str(directory)+':/run/knowledge:ro',
                '-v',str(install)+':/app:ro','eclipse-temurin:21-jre','/app/bin/olo-'+name)
            self.containers.append(container)
            self.services[name]=container
        self.provision_packages()
    def vector_settings(self):
        if hasattr(self,'vectors'):return self.vectors
        qdrant=docker('run','--label','io.olo.qualification.owner=olo-knowledge','--rm','-d','--network',self.network,'--network-alias','qdrant','qdrant/qdrant:latest');self.containers.append(qdrant)
        models=ROOT/'build/ollama-models';models.mkdir(parents=True,exist_ok=True)
        model_volume=docker('volume','create','--label','io.olo.qualification.owner=olo-knowledge',self.name+'-models');self.model_volumes.append(model_volume)
        docker('run','--rm','--label','io.olo.qualification.owner=olo-knowledge','--network','none',
            '-v',str(models)+':/seed:ro','-v',model_volume+':/target','--entrypoint','sh','ollama/ollama:latest','-c','cp -a /seed/. /target/')
        ollama=docker('run','--label','io.olo.qualification.owner=olo-knowledge','--rm','-d','--network',self.network,'--network-alias','ollama','-v',model_volume+':/root/.ollama','ollama/ollama:latest');self.containers.append(ollama)
        self.wait(lambda:subprocess.run(['docker','exec',ollama,'ollama','list'],capture_output=True).returncode==0,'Embedding readiness')
        docker('exec',ollama,'ollama','pull','all-minilm:latest')
        if not (models/'manifests').exists():
            docker('run','--rm','--label','io.olo.qualification.owner=olo-knowledge','--network','none',
                '-v',model_volume+':/seed:ro','-v',str(models)+':/target','--entrypoint','sh','ollama/ollama:latest','-c','cp -a /seed/. /target/')
        script="""import json,urllib.request
def send(url,body=None,method='GET'):
 r=urllib.request.Request(url,json.dumps(body).encode() if body is not None else None,{'Content-Type':'application/json'},method=method)
 return json.load(urllib.request.urlopen(r,timeout=180))
tag=next(v for v in send('http://ollama:11434/api/tags')['models'] if v['name']=='all-minilm:latest')
dimensions=len(send('http://ollama:11434/api/embed',{'model':'all-minilm:latest','input':['dimension probe'],'truncate':False},'POST')['embeddings'][0])
send('http://qdrant:6333/collections/qualification',{'vectors':{'size':dimensions,'distance':'Cosine'}},'PUT')
print(tag['digest'])
"""
        digest=self.script(script).removeprefix('sha256:')
        self.vectors=dict(QDRANT_ORIGIN='http://qdrant:6333',QDRANT_API_KEY='fixture-key',QDRANT_COLLECTION='qualification',EMBEDDING_ORIGIN='http://ollama:11434',EMBEDDING_MODEL='all-minilm:latest',EMBEDDING_DIGEST='sha256:'+digest)
        return self.vectors
    def provision_packages(self):
        digests=[]
        network=json.loads(docker('network','inspect',self.network))[0]['IPAM']['Config'][0]['Subnet']
        for name in ['memory','rag']:
            archive=ROOT/f'packages/{name}/build/toolgate/olo.{name}-0.1.0.tgpkg'
            self.require(archive.is_file(),'Build the '+name+' package first')
            status,uploaded=http(self.origin+'/api/control/v1/packages/uploads',self.tokens['admin'],archive.read_bytes(),'POST',{'Content-Type':'application/vnd.ololabs.toolgate.package+zip','Idempotency-Key':str(uuid.uuid4())})
            self.require(status in (200,201),'Package upload refused HTTP '+str(status))
            digest=uploaded['packageDigest'];digests.append(digest.removeprefix('sha256:'))
            deployment=self.api('/packages/deployments',{'packageDigest':digest,'globals':{'ServiceHost':'olo-'+name},'hostIds':['tool-host-local'],
                'privateRanges':[{'host':'olo-'+name,'cidr':network,'ports':[8443]}],
                'destinationTls':{'olo-'+name:{'caBundle':self.ca_pem.decode(),'spkiPins':[]}}},'POST')
            for tool in deployment['toolIds']:
                entry=self.api('/tools/'+tool);self.api('/tools/'+tool,{**entry,'enabled':True},'PUT',revision=entry['revision'])
        original=self.api('/bindings/binding-default')
        self.api('/bindings',{**original,'id':self.binding,'name':'Scoped knowledge execution','revision':1,
            'actions':['memory.read','memory.append','memory.delete','memory.share','memory.export','rag.query','rag.ingest','rag.update','rag.delete','rag.share'],
            'allowedPackageDigests':digests},'POST')
        self.kb=str(uuid.uuid4());self.conversation=str(uuid.uuid4())
        conditions=dict(notBeforeUnixMs=0,expiresAtUnixMs=0,networkCidrs=[],devicePosture=[],regions=[],hoursUtc=[],requireOnline=True,highRisk=False)
        # Explicit root resources and operations; no wildcard resource grants.
        scope=dict(toolGroups=dict(ids=['default-tools'],all=False),deviceGroups=dict(ids=['default-devices'],all=False),
            actions=['memory.read','memory.append','memory.delete','memory.share','memory.export','rag.query','rag.ingest','rag.update','rag.delete','rag.share'],allActions=False,
            resources=[dict(kind='CUSTOM',locator='olo://'+self.tenant+'/conversation/'+self.conversation,match='EXACT'),dict(kind='CUSTOM',locator='olo://'+self.tenant+'/kb/'+self.kb,match='EXACT')],conditions=conditions)
        self.api('/grants',dict(id='knowledge-human',name='Explicit qualification resources',enabled=True,revision=1,sourceType='TEAM',sourceId='installation-administrators',purpose='HUMAN',scope=scope),'POST')
    def call(self,user,operation,resource,request,logical_id=None):
        kind='conversationId' if operation.startswith('memory.') else 'kbId'
        status,response=self.mcp(user,params={'name':'olo.'+operation.replace('.','/'),'arguments':{kind:resource,'request':json.dumps(request,separators=(',',':'))}},operation=logical_id)
        return status,response
    def result(self,user,operation,resource,request,logical_id=None):
        logical_id=logical_id or str(uuid.uuid4())
        end=time.monotonic()+45
        while time.monotonic()<end:
            status,response=self.call(user,operation,resource,request,logical_id)
            data=(((response or {}).get('error') or {}).get('data') or {})
            unknown=data.get('outcome')=='OUTCOME_UNKNOWN' or (data.get('error') or {}).get('code')=='OUTCOME_UNKNOWN'
            if status in (429,503) and not unknown:
                self.backpressure_events.append(status)
                time.sleep(.5);continue
            if status!=200 or 'error' in response:return status,response
            result=response['result'];structured=result.get('structuredContent',{})
            if structured.get('state') in ['QUEUED','ADMITTED','EXECUTING']:
                time.sleep(.25);continue
            if result.get('isError'):return status,response
            if 'json' in structured:return 200,json.loads(structured['json'])
            if 'content' in result:
                for block in result['content']:
                    if block.get('type')=='text':
                        decoded=json.loads(block['text'])
                        if 'json' in decoded:return 200,json.loads(decoded['json'])
            return status,response
        raise TimeoutError('Governed operation did not settle')
    def qualify(self):
        gates=[];timings={}
        def mark(name):gates.append(name);print('PASS '+name,flush=True)
        def successful(value):
            status,result=value
            self.require(status==200 and 'error' not in result and 'result' not in result,'Governed operation failed: '+json.dumps(result))
            return result
        def scope_denials(name):
            logs=run(['docker','logs',self.services[name]],text=True)
            return (logs.stdout+logs.stderr).count('authorization_denied source=org.olo.knowledge.core.Scope#denied')
        # Resource IDs are selected before authorization and grants bind those exact roots.
        self.wait(lambda:'olo.memory/append' in {v['name'] for v in self.mcp('reviewer-1','tools/list')[1].get('result',{}).get('tools',[])},'Governed tool placement')
        successful(self.result('reviewer-1','memory.append',self.conversation,{'type':'create','create':True}))
        mark('real Gateway -> Control -> Tool Host Java package -> mTLS memory -> PostgreSQL')
        message=str(uuid.uuid4());turn=str(uuid.uuid4());operation=str(uuid.uuid4())
        body=dict(messageId=message,turnId=turn,role='user',content='Qualification accepted input',dependencies=[],create=False)
        successful(self.result('reviewer-1','memory.append',self.conversation,body,operation))
        successful(self.result('reviewer-1','memory.append',self.conversation,body,operation))
        history=successful(self.result('reviewer-1','memory.read',self.conversation,{'type':'turn','turnId':turn}))
        self.require(len(history['messages'])==1,'Replay duplicated an accepted message')
        mark('same logical invocation and stable message ID return one durable record')
        concurrent_turn=str(uuid.uuid4());concurrent_operation=str(uuid.uuid4())
        concurrent_body={**body,'messageId':str(uuid.uuid4()),'turnId':concurrent_turn}
        with ThreadPoolExecutor(max_workers=4) as callers:
            repeats=list(callers.map(lambda _:self.result('reviewer-1','memory.append',self.conversation,concurrent_body,concurrent_operation),range(4)))
        completed=0
        for status,response in repeats:
            if response.get('error',{}).get('data',{}).get('code')=='CONFLICT':
                # Control may reject a simultaneous duplicate while the first
                # request owns admission. Retry only this exact original ID and
                # body after the concurrent owner has settled, never a new ID.
                continue
            successful((status,response));completed+=1
        self.require(completed>0,'No concurrent caller completed the original invocation')
        for _ in repeats:
            successful(self.result('reviewer-1','memory.append',self.conversation,concurrent_body,concurrent_operation))
        history=successful(self.result('reviewer-1','memory.read',self.conversation,{'type':'turn','turnId':concurrent_turn}))
        self.require(len(history['messages'])==1,'Concurrent retries duplicated an accepted message')
        mark('concurrent replay of the original invocation retains one durable memory record')
        before=scope_denials('memory')
        status,denied=self.result('reviewer-2','memory.read',self.conversation,{'budget':4000})
        self.require(status!=200 or 'messages' not in denied,'Other user read a private conversation')
        self.require(scope_denials('memory')>before,'Expected storage ACL denial; dependency failures cannot pass isolation')
        mark('user isolation with a real valid ToolGate grant and known root ID')
        before=scope_denials('memory')
        status,denied=self.result('reviewer-2','memory.read',self.conversation,{'budget':4000,'userId':'reviewer-1','tenantId':self.tenant,'agentName':'incident-commander'})
        self.require(status!=200 or 'messages' not in denied,'Forged identity arguments released private memory')
        self.require(scope_denials('memory')>before,'Forged identity test did not reach an explicit storage ACL denial')
        mark('caller-supplied user, tenant and agent arguments cannot authorize private memory')
        status,denied=self.mcp('reviewer-1',params={'name':'olo.memory/read','arguments':{'conversationId':self.conversation,'request':'{}'}},token=self.credential('reviewer-1',tenant=str(uuid.uuid4())))
        self.require(status in [401,403],'Foreign-tenant credential admitted')
        mark('cross-tenant credential rejected at authenticated ingress')
        status,denied=self.result('reviewer-1','memory.read',str(uuid.uuid4()),{'budget':4000})
        self.require(policy_denied(status,denied),'Substituted root must produce an explicit authorization denial')
        mark('substituted resource ID rejected before storage release')
        doc=str(uuid.uuid4())
        successful(self.result('reviewer-1','rag.ingest',self.kb,dict(docId=doc,create=True,content='The fictional Aurora project launches in December.',mediaType='text/plain',expectedVersion=0)))
        found=successful(self.result('reviewer-1','rag.query',self.kb,{'query':'When does Aurora launch?'}))
        self.require(any(v['doc_id']==doc and v['version']==1 for v in found['chunks']),'Published document missing')
        mark('real governed RAG ingest, embeddings, filtered retrieval and versioned citations')
        before=scope_denials('rag')
        status,denied=self.result('reviewer-2','rag.query',self.kb,{'query':'When does Aurora launch?'})
        self.require(status!=200 or 'chunks' not in denied,'Other user read a private knowledge base')
        self.require(scope_denials('rag')>before,'Expected RAG storage ACL denial; dependency failures cannot pass isolation')
        successful(self.result('reviewer-1','rag.share',self.kb,{'member':'reviewer-2','write':False}))
        shared=successful(self.result('reviewer-2','rag.query',self.kb,{'query':'When does Aurora launch?'}))
        self.require(any(v['doc_id']==doc for v in shared['chunks']),'Explicitly shared document missing')
        began=time.monotonic()
        successful(self.result('reviewer-1','rag.share',self.kb,{'docId':doc,'member':'reviewer-1','restricted':True}))
        restricted=successful(self.result('reviewer-2','rag.query',self.kb,{'query':'When does Aurora launch?'}))
        self.require(not restricted['chunks'],'Narrowed document ACL released a private source')
        timings['documentAclExclusionSeconds']=time.monotonic()-began
        self.require(timings['documentAclExclusionSeconds']<=30,'Document ACL exclusion exceeds revocation bound')
        mark('governed document ACL narrowing excludes a previously shared source within 30 seconds')
        began=time.monotonic()
        successful(self.result('reviewer-1','rag.delete',self.kb,{'docId':doc}))
        found=successful(self.result('reviewer-1','rag.query',self.kb,{'query':'When does Aurora launch?'}))
        self.require(not found['chunks'],'Deleted document remains searchable')
        timings['deleteExclusionSeconds']=time.monotonic()-began
        self.require(timings['deleteExclusionSeconds']<=30,'Deletion exclusion exceeds revocation bound')
        mark('governed deletion excludes the published document within 30 seconds')
        grant=self.api('/grants/knowledge-human')
        began=time.monotonic()
        self.api('/grants/knowledge-human',{**grant,'enabled':False},'PUT',revision=grant['revision'])
        status,result=self.result('reviewer-1','memory.read',self.conversation,{'budget':4000})
        self.require(policy_denied(status,result),'Revoked grant must produce an explicit authorization denial')
        timings['grantRevocationSeconds']=time.monotonic()-began
        self.require(timings['grantRevocationSeconds']<=30,'Grant revocation exceeds 30 seconds')
        mark('live Control grant revocation blocks result retrieval within 30 seconds')
        tested_image=json.loads(docker('image','inspect',self.image))[0]
        report={'passed':True,'gates':gates,'timings':timings,
            'imageId':tested_image['Id'],
            'toolgateRevision':(tested_image['Config'].get('Labels') or {}).get('org.opencontainers.image.revision'),
            'knowledgeRevision':run(['git','rev-parse','HEAD'],cwd=ROOT,text=True).stdout.strip(),
            'knowledgeWorktreeDirty':bool(run(['git','status','--porcelain'],cwd=ROOT,text=True).stdout.strip()),
            'backpressureRetries':len(self.backpressure_events),
            'scope':'Owned Quickstart plus separate memory/RAG JVM containers and databases. Gateway-Control loopback HTTP fixture; no CNI, staging load or complete Temporal pipeline claim.',
            'userCredentialExchange':'Actual Control RFC 8693 endpoint, fixture platform assertions and dedicated production issuer key configuration'}
        report['runId']=self.name
        self.artifact('real-stack.json',json.dumps(report,indent=2)+'\n')
        return report

@contextlib.contextmanager
def fresh_stack(image='olo-toolgate-quickstart:olo-m8'):
    stack=Stack(image)
    try:yield stack.start()
    except BaseException as failure:
        stack.artifact('real-stack.json',json.dumps({'passed':False,'stage':'FAILED','runId':stack.name,'failureType':type(failure).__name__},indent=2)+'\n')
        raise
    finally:stack.close()

if __name__=='__main__':
    with fresh_stack() as stack:
        if '--readiness-only' in sys.argv:
            print(json.dumps({'gate':'fresh Control/Gateway/Tool Host readiness','passed':True,'tenant':stack.tenant}))
        else:
            stack.start_services();stack.qualify()
