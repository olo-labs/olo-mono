"""Exercise the committed NetworkPolicies on an owned Calico cluster; always delete probe resources."""
import argparse,json,pathlib,subprocess,tempfile,time

ROOT=pathlib.Path(__file__).resolve().parents[1]

def qualify(kubeconfig):
    kubeconfig=pathlib.Path(kubeconfig).resolve(strict=True)
    expected=(ROOT/'build/qualification/runtime/kubeconfig').resolve()
    if kubeconfig!=expected:raise ValueError('Use the owned staging kubeconfig')
    def kubectl(*args,check=True):
        return subprocess.run(['kubectl','--kubeconfig',str(kubeconfig),*args],check=check,capture_output=True,text=True)
    context=kubectl('config','current-context').stdout.strip()
    if context!='kind-olo-qualification':raise ValueError('Qualification context mismatch')
    def apply(value):
        return subprocess.run(['kubectl','--kubeconfig',str(kubeconfig),'apply','-f','-'],input=json.dumps(value),text=True,capture_output=True,check=True)
    image='olo-knowledge-network-probe:qualification'
    subprocess.run(['docker','build','--platform','linux/amd64','--provenance=false','--label','io.olo.qualification.owner=olo-knowledge','-t',image,'-'],input='FROM python:3.12-alpine@sha256:b658b05ce9c4eb97ec6fc19424818a1f31993659c2b505a532f470e83807815d\n',text=True,check=True,capture_output=True)
    subprocess.run([str(ROOT/'build/tooling/kind.exe'),'load','docker-image',image,'--name','olo-qualification'],check=True,capture_output=True)
    def pod(name,namespace,app,port=None,part=False,component=None):
        labels={'app.kubernetes.io/name':app,'io.olo.qualification':'network-probe'}
        if part:labels['app.kubernetes.io/part-of']='olo-knowledge'
        if component:labels['app.kubernetes.io/component']=component
        command=['python','-m','http.server',str(port)] if port else ['python','-c','import time;time.sleep(3600)']
        container={'name':'probe','image':image,'imagePullPolicy':'Never','command':command,'resources':{'requests':{'cpu':'10m','memory':'24Mi'},'limits':{'cpu':'100m','memory':'64Mi'}}}
        if port:container['readinessProbe']={'tcpSocket':{'port':port},'initialDelaySeconds':1,'periodSeconds':1}
        return {'apiVersion':'v1','kind':'Pod','metadata':{'name':name,'namespace':namespace,'labels':labels},
            'spec':{'containers':[container],
                'securityContext':{'runAsNonRoot':True,'runAsUser':10001}}}
    existing=[]
    for namespace in ['olo-knowledge','toolgate']:
        value=kubectl('get','namespace',namespace,check=False)
        if value.returncode==0:existing.append(namespace)
    if existing:raise ValueError('Probe namespaces already exist; run only on the empty owned staging cluster')
    try:
        for name in ['olo-knowledge','toolgate']:apply({'apiVersion':'v1','kind':'Namespace','metadata':{'name':name,'labels':{'io.olo.qualification':'network-probe'}}})
        kubectl('apply','-f',str(ROOT/'deploy/network-policy.yaml'))
        specs=[pod('host','toolgate','host',component='tool-host'),pod('impostor','olo-knowledge','impostor',component='tool-host'),
            pod('backend','olo-knowledge','olo'),pod('worker','olo-knowledge','olo-worker'),
            pod('memory','olo-knowledge','olo-memory',8443,True),pod('rag','olo-knowledge','olo-rag',8443,True),
            pod('memory-db','olo-knowledge','memory-postgres',5432),pod('rag-db','olo-knowledge','rag-postgres',5432),
            pod('vectors','olo-knowledge','qdrant',6333),pod('embeddings','olo-knowledge','embeddings',11434)]
        for value in specs:apply(value)
        for namespace in ['toolgate','olo-knowledge']:kubectl('wait','--for=condition=Ready','pod','--all','-n',namespace,'--timeout=180s')
        addresses={}
        for value in specs:
            meta=value['metadata'];v=json.loads(kubectl('get','pod',meta['name'],'-n',meta['namespace'],'-o','json').stdout)
            addresses[meta['name']]=v['status']['podIP']
        results=[]
        def probe(source,target,port,allowed):
            namespace='toolgate' if source=='host' else 'olo-knowledge'
            code="import socket,sys;s=socket.socket();s.settimeout(2);r=s.connect_ex((sys.argv[1],int(sys.argv[2])));s.close();sys.exit(0 if r==0 else 1)"
            result=kubectl('exec','-n',namespace,source,'--','python','-c',code,addresses[target],str(port),check=False)
            observed=result.returncode==0
            results.append(dict(source=source,target=target,port=port,allowed=allowed,observedAllowed=observed))
            if observed!=allowed:raise AssertionError('Network isolation mismatch: '+source+' -> '+target+'; probe details: '+result.stderr.strip()+' '+result.stdout.strip())
        # Positive controls prove servers and network are functioning before negative-access tests.
        for target in ['memory','rag']:probe('host',target,8443,True)
        probe('memory','memory-db',5432,True);probe('rag','rag-db',5432,True)
        probe('rag','vectors',6333,True);probe('rag','embeddings',11434,True)
        for caller in ['backend','worker','impostor']:
            for target in ['memory','rag']:probe(caller,target,8443,False)
            probe(caller,'memory-db',5432,False);probe(caller,'rag-db',5432,False)
        probe('memory','rag-db',5432,False);probe('rag','memory-db',5432,False)
        probe('memory-db','rag-db',5432,False);probe('vectors','embeddings',11434,False)
        output=ROOT/'build/qualification/network-policy.json';output.parent.mkdir(parents=True,exist_ok=True)
        output.write_text(json.dumps({'passed':True,'context':context,'scope':'Live Calico NetworkPolicy with TCP probe servers; application mTLS is a separate gate','probes':results},indent=2)+'\n',encoding='utf-8')
        print('PASS live Calico network isolation: '+str(len(results))+' positive/negative probes')
    finally:
        for namespace in ['toolgate','olo-knowledge']:
            kubectl('delete','namespace',namespace,'--ignore-not-found','--wait=true','--timeout=120s',check=False)

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--kubeconfig',required=True)
    qualify(parser.parse_args().kubeconfig)
