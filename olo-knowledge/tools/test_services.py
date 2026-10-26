"""Run real PostgreSQL storage tests in an owned disposable container. Never print generated credentials."""
import os, pathlib, secrets, subprocess, sys, time, urllib.request, json, tempfile, math
from test_pki import prepare
root = pathlib.Path(__file__).resolve().parents[1]
password = secrets.token_hex(24)
runtime=root/'build/qualification/runtime';runtime.mkdir(parents=True,exist_ok=True)
process_file=runtime/('fixture-process-'+str(os.getpid())+'.json')
process_file.write_text(json.dumps({'workspace':str(root),'pid':os.getpid(),'session':'olo-qualification-'+secrets.token_hex(6),'script':'test_services.py'}),encoding='utf-8')
container = None
dependencies=[]
volumes=[]
pki=None
def run(args, **kwargs):
    return subprocess.run(args, check=True, **kwargs)
try:
    container = subprocess.check_output(['docker','run','--label','io.olo.qualification.owner=olo-knowledge','--rm','-d','-e','POSTGRES_PASSWORD',
        '-p','127.0.0.1::5432','postgres:16'], env=dict(os.environ,POSTGRES_PASSWORD=password),text=True).strip()
    for _ in range(60):
        if subprocess.run(['docker','exec',container,'pg_isready','-U','postgres'],capture_output=True).returncode == 0: break
        time.sleep(0.5)
    else: raise RuntimeError('Test database did not become ready')
    run(['docker','exec','-i',container,'psql','-U','postgres','-v','ON_ERROR_STOP=1'],
        input=f"CREATE ROLE knowledge_test LOGIN PASSWORD '{password}' NOSUPERUSER NOBYPASSRLS;",text=True,capture_output=True)
    port=subprocess.check_output(['docker','inspect','--format','{{(index (index .NetworkSettings.Ports "5432/tcp") 0).HostPort}}',container],text=True).strip()
    env=dict(os.environ,KNOWLEDGE_EVALUATION='true' if '--evaluation' in sys.argv else 'false',KNOWLEDGE_TEST_URL=f'jdbc:postgresql://127.0.0.1:{port}/postgres?sslmode=disable',
        KNOWLEDGE_TEST_PASSWORD=password,JAVA_TOOL_OPTIONS='-Duser.timezone=UTC')
    if '--with-tls' in sys.argv:
        pki=tempfile.TemporaryDirectory(prefix='olo-knowledge-pki-',dir=runtime)
        env.update(KNOWLEDGE_PKI=prepare(pki.name,password),KNOWLEDGE_PKI_PASSWORD=password)
    if any(flag in sys.argv for flag in ('--with-vectors','--evaluation','--profile-embeddings')):
        def dependency(image, internal_port, extra=()):
            ident=subprocess.check_output(['docker','run','--label','io.olo.qualification.owner=olo-knowledge','--rm','-d','-p',f'127.0.0.1::{internal_port}',*extra,image],text=True).strip()
            dependencies.append(ident)
            exposed=subprocess.check_output(['docker','inspect','--format',f'{{{{(index (index .NetworkSettings.Ports "{internal_port}/tcp") 0).HostPort}}}}',ident],text=True).strip()
            return f'http://127.0.0.1:{exposed}'
        qdrant=dependency('qdrant/qdrant:latest',6333)
        models=root/'build/ollama-models';models.mkdir(parents=True,exist_ok=True)
        model_mount=f'{models.as_posix()}:/root/.ollama'
        native_models='--native-model-volume' in sys.argv or '--profile-embeddings' not in sys.argv
        if native_models:
            volume=subprocess.check_output(['docker','volume','create','--label','io.olo.qualification.owner=olo-knowledge'],text=True).strip()
            volumes.append(volume)
            run(['docker','run','--rm','--label','io.olo.qualification.owner=olo-knowledge','--network','none',
                '-v',f'{models.as_posix()}:/seed:ro','-v',volume+':/target','--entrypoint','sh',
                'ollama/ollama:latest','-c','cp -a /seed/. /target/'],capture_output=True)
            model_mount=volume+':/root/.ollama'
        ollama=dependency('ollama/ollama:latest',11434,('-v',model_mount))
        env['KNOWLEDGE_MODEL_STORAGE']='native-volume' if native_models else 'windows-bind'
        def exchange(url, data=None, timeout=180):
            request=urllib.request.Request(url,data=json.dumps(data).encode() if data is not None else None,headers={'Content-Type':'application/json'})
            with urllib.request.urlopen(request,timeout=timeout) as response:return json.load(response)
        for _ in range(60):
            try: exchange(ollama+'/api/tags');break
            except (OSError,ValueError):time.sleep(.5)
        print('Preparing pinned real embedding fixture and Qdrant',flush=True)
        exchange(ollama+'/api/pull',{'model':'all-minilm:latest','stream':False})
        if native_models and not (models/'manifests').exists():
            run(['docker','run','--rm','--label','io.olo.qualification.owner=olo-knowledge','--network','none',
                '-v',volume+':/seed:ro','-v',f'{models.as_posix()}:/target','--entrypoint','sh',
                'ollama/ollama:latest','-c','cp -a /seed/. /target/'],capture_output=True)
        tags=exchange(ollama+'/api/tags')['models']
        digest=next(m['digest'] for m in tags if m['name']=='all-minilm:latest')
        exchange(ollama+'/api/embed',{'model':'all-minilm:latest','input':['warmup'],'truncate':False})
        env.update(KNOWLEDGE_QDRANT=qdrant,KNOWLEDGE_OLLAMA=ollama,KNOWLEDGE_MODEL_DIGEST='sha256:'+digest.removeprefix('sha256:'))
        if '--profile-embeddings' in sys.argv:
            threads=int(os.environ.get('KNOWLEDGE_EMBEDDING_THREADS','0'))
            if not 0<=threads<=16:raise ValueError('Embedding profile threads must be 0 (default) or 1–16')
            observations=[]
            for _ in range(20):
                began=time.perf_counter()
                exchange(ollama+'/api/tags')
                embed_began=time.perf_counter()
                request={'model':'all-minilm:latest','input':['When does the fictional Aurora project launch?'],'truncate':False,'keep_alive':'5m'}
                if threads:request['options']={'num_thread':threads}
                embedded=exchange(ollama+'/api/embed',request)
                embed_end=time.perf_counter()
                exchange(ollama+'/api/tags')
                observations.append({'clientTotalMs':(time.perf_counter()-began)*1000,
                    'clientEmbeddingMs':(embed_end-embed_began)*1000,
                    'serverTotalMs':embedded.get('total_duration',0)/1e6,
                    'serverLoadMs':embedded.get('load_duration',0)/1e6})
            def percentile(key,p):return sorted(row[key] for row in observations)[math.ceil(len(observations)*p)-1]
            report={'scope':'embedding dependency only, warm sequential requests; excludes SQL, Gateway and authorization',
                'modelDigest':digest,'threads':threads,'nativeModelVolume':native_models,
                'samples':len(observations),'clientP50Ms':percentile('clientTotalMs',.5),
                'clientP95Ms':percentile('clientTotalMs',.95),'observations':observations}
            output=root/'build/qualification/embedding-profile.json'
            output.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
            print('Embedding dependency p50/p95 ms:',report['clientP50Ms'],report['clientP95Ms'],flush=True)
    gradle=os.environ.get('GRADLE',str(pathlib.Path.home()/'.gradle/wrapper/dists/gradle-8.12-bin/cetblhg4pflnnks72fxwobvgv/gradle-8.12/bin/gradle.bat'))
    if '--profile-embeddings' not in sys.argv:
        run([gradle,'test','--rerun-tasks','--console=plain'],cwd=root,env=env)
finally:
    for dependency in dependencies:subprocess.run(['docker','stop',dependency],capture_output=True)
    for volume in volumes:subprocess.run(['docker','volume','rm',volume],capture_output=True)
    if container: subprocess.run(['docker','stop',container],capture_output=True)
    if pki:pki.cleanup()
    process_file.unlink(missing_ok=True)
