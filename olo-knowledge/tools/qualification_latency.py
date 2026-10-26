"""Measure the governed Gateway query latency floor; not the corpus/load acceptance test."""
import json
import math
import time
import uuid

from qualification_stack import ROOT, Stack, docker, run


def qualify(stack):
    stack.artifact('gateway-latency.json',json.dumps(dict(passed=False,runId=stack.name,stage='STARTING'))+'\n')
    record=json.loads(stack.process_file.read_text())
    record['script']='qualification_latency.py'
    stack.process_file.write_text(json.dumps(record),encoding='utf-8')
    stack.start().start_services()
    doc=str(uuid.uuid4())
    status,result=stack.result('reviewer-1','rag.ingest',stack.kb,dict(docId=doc,create=True,
        content='The fictional Aurora project launches in December.',mediaType='text/plain',expectedVersion=0))
    stack.require(status==200 and result.get('version')==1,'Source publication failed')
    samples=[]
    for index in range(17):
        began=time.perf_counter()
        status,result=stack.result('reviewer-1','rag.query',stack.kb,{'query':'When does Aurora launch?'})
        elapsed=(time.perf_counter()-began)*1000
        stack.require(status==200 and any(chunk['doc_id']==doc and chunk['version']==1
                     for chunk in result.get('chunks',[])),'Governed baseline retrieval failed')
        if index:samples.append(elapsed)
    ordered=sorted(samples)
    p50=ordered[math.ceil(len(ordered)*.50)-1]
    p95=ordered[math.ceil(len(ordered)*.95)-1]
    passed=p50<=300 and p95<=800
    report=dict(passed=passed,runId=stack.name,samplesMs=samples,gatewayQueryP50Ms=p50,gatewayQueryP95Ms=p95,
        targetsMs=dict(p50=300,p95=800),backpressureRetries=len(stack.backpressure_events),
        scope='Single-user single-document latency floor through real Gateway/broker/service/store. One warm-up excluded; no corpus, concurrency or M8.2a signoff.',
        knowledgeRevision=run(['git','rev-parse','HEAD'],cwd=ROOT,text=True).stdout.strip(),
        knowledgeWorktreeDirty=bool(run(['git','status','--porcelain'],cwd=ROOT,text=True).stdout.strip()),
        toolgateImageId=json.loads(docker('image','inspect',stack.image))[0]['Id'])
    stack.artifact('gateway-latency.json',json.dumps(report,indent=2)+'\n')
    print('Gateway single-user latency floor p50/p95 ms:',round(p50,2),round(p95,2),flush=True)
    stack.require(passed,'Gateway latency floor exceeds accepted targets; full RAG qualification remains open')


if __name__=='__main__':
    stack=Stack('olo-toolgate-quickstart:olo-m8')
    try:qualify(stack)
    finally:stack.close()
