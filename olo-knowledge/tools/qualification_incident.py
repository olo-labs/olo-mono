"""Real delegated incident package and Temporal approval qualification; scripted test model only."""
import argparse
import hashlib
import json
import pathlib
import secrets
import time
import uuid

from qualification_pipeline import Pipeline, workflow
from qualification_stack import ROOT, docker, http, policy_denied, run

AGENTS = {
    'triage-agent': ['incidents', 'host-status', 'crash-report'],
    'remediation-agent': ['runbook', 'remediate', 'verify'],
    'incident-report-agent': ['update-incident'],
}


def incident_workflow():
    definition = workflow()
    definition.update(id='incident-recovery', label='Incident recovery qualification')
    definition['capability']['id'] = 'incident-recovery'
    template = definition['nodes'][1]
    commander = json.loads(json.dumps(template))
    commander['id'] = 'commander'
    commander['execution']['workflowRef']['workflowId'] = 'incident-recovery'
    commander['configuration'].update(agentCallPlanner=True, outputVariable='agentCallSequenceJson',
        continueNodeId='summary', maxInvalidJsonRetries=3, maxToolRounds=0,
        promptTemplate='INCIDENT_DELEGATION_CATALOG:\n{availableAgentsJson}\n'
                       'Delegate incident triage, remediation and reporting in order. Return agentCalls JSON only. {message}')
    commander['ports'].append(dict(id='agentPlug', label='Available agents', schema='agent-plug', type='agent-plug', direction='INPUT'))
    summary = json.loads(json.dumps(template))
    summary['id'] = 'summary'
    summary['execution']['workflowRef']['workflowId'] = 'incident-recovery'
    summary['configuration'].update(agentName='incident-commander', maxToolRounds=0,
        promptTemplate='Summarize the completed incident child-agent results. {agentResultsJson}')
    plugins = []
    for name in AGENTS:
        plugins.append(dict(id='delegate-'+name, type='AGENT', label=name,
            ports=[dict(id='agentPlug', label='Available agents', schema='agent-plug', type='agent-plug', direction='OUTPUT')],
            execution=dict(executionKind='SUBWORKFLOW', executionModel='CHILD_WORKFLOW',
                workflowRef=dict(workflowId=name, version='1.0.0', inputMapping={}, outputMapping={})),
            configuration=dict(delegateAgentId=name)))
    definition['nodes'] = [definition['nodes'][0], commander, summary, definition['nodes'][-1], *plugins]
    definition['edges'] = [dict(id=a+'-'+b, sourceNodeId=a, sourcePortId='out', targetNodeId=b, targetPortId='in')
                           for a, b in [('start','commander'),('commander','summary'),('summary','end')]]
    definition['edges'] += [dict(id='plug-'+name, sourceNodeId='delegate-'+name, sourcePortId='agentPlug',
                                targetNodeId='commander', targetPortId='agentPlug') for name in AGENTS]
    definition['childWorkflows'] = [dict(workflowId=name, workflowVersion='1.0.0') for name in AGENTS]
    definition['availableAgents'] = [dict(id=name) for name in AGENTS]
    definition['metadata']['agentSelectionStrategy'] = 'dynamic'
    definition['runtime']['delegation'] = dict(enabled=True, parallelEnabled=False, maxDepth=3,
        maxDelegations=3, resultAggregation='MERGE', memoryScope='SHARED')
    definition['metadata']['returnOutputKey'] = 'summary'
    definition['variables'] += [dict(name=name, type=kind, scope='LOCAL', **({'defaultValue':default} if default is not None else {}))
        for name,kind,default in [('agentCallSequenceJson','string',None),('availableAgentsJson','string','[]'),
                                 ('agentResultsJson','string','[]'),('agentCallSequenceJsonRetryCount','integer',0),
                                 ('agentCallSequenceJsonValidationError','string',None)]]
    return definition


def incident_child_workflows():
    children = []
    for name in AGENTS:
        child = workflow()
        child.update(id=name, label=name, isDefault=False, isChildWorkflow=True)
        child['capability']['id'] = name
        child['nodes'][1]['execution']['workflowRef']['workflowId'] = name
        child['nodes'][1]['configuration'].update(agentName=name, maxToolRounds=5,
            promptTemplate='Complete your delegated fictional INC-5001 stage. {message}')
        children.append(child)
    return children


class Incident(Pipeline):
    workflow_id = 'incident-recovery'
    report_name = 'incident.json'

    def __init__(self, package):
        super().__init__()
        self.package = package.resolve(strict=True)
        record = json.loads(self.process_file.read_text())
        record['script'] = 'qualification_incident.py'
        self.process_file.write_text(json.dumps(record))
        self.artifact('incident.json', json.dumps(dict(passed=False, runId=self.name, stage='STARTING'))+'\n')

    def workflow_definitions(self):
        return [incident_workflow(), *incident_child_workflows()]

    def configure_worker(self, config):
        config['toolgate'].update(mcpUrl='http://control:8085/mcp', credentialsDir='/run/olo/actors', agentMappings=[dict(
            tenant=self.tenant, workflow=name, agents=[name]) for name in AGENTS])
        directory = self.path/'pipeline'
        actors = directory/'actors'
        actors.mkdir()
        for name, token in self.actors.items():
            (actors/(name+'.token')).write_text(token)
        plan = [('incidents', {'priority': 'P1'}), ('host-status', {'host': 'app-02'}),
                ('crash-report', {'host': 'app-02'}), ('runbook', {'cause': 'OOM'}),
                ('remediate', dict(host='app-02', service='payments-api', action='ROLLBACK_DEPLOY', reference='INC-5001-qualify')),
                ('verify', {'remediationId': 'REM-00000000'}),
                ('update-incident', dict(incidentId='INC-5001', status='RESOLVED', note='Qualified rollback and verification.', reference='INC-5001-report'))]
        (directory/'idp/incident-plan.json').write_text(json.dumps([
            (self.tools[name].replace('.', '_').replace('/', '_'), arguments) for name, arguments in plan]))
        return config

    def scope(self, names):
        return dict(toolGroups=dict(ids=['default-tools'], all=False), deviceGroups=dict(ids=['default-devices'], all=False),
                    actions=['invoke'], allActions=False,
                    resources=[dict(kind='CUSTOM', locator='tool://'+self.tools[name], match='EXACT') for name in names],
                    conditions=dict(notBeforeUnixMs=0, expiresAtUnixMs=0, networkCidrs=[], devicePosture=[], regions=[],
                                    hoursUtc=[], requireOnline=True, highRisk=False))

    def provision_incident(self):
        status, uploaded = http(self.origin+'/api/control/v1/packages/uploads', self.tokens['admin'],
                                self.package.read_bytes(), 'POST', {'Content-Type': 'application/vnd.ololabs.toolgate.package+zip',
                                                                   'Idempotency-Key': str(uuid.uuid4())})
        self.require(status in (200, 201), 'Incident package upload failed HTTP '+str(status))
        deployment = self.api('/packages/deployments', dict(packageDigest=uploaded['packageDigest'], globals={}, hostIds=['tool-host-local']), 'POST')
        self.tools = {tool.rsplit('/', 1)[-1]: tool for tool in deployment['toolIds']}
        self.require(set(self.tools) == {name for names in AGENTS.values() for name in names}, 'Unexpected incident package tools')
        for tool in self.tools.values():
            entry = self.api('/tools/'+tool)
            self.api('/tools/'+tool, {**entry, 'enabled': True}, 'PUT', revision=entry['revision'])
        binding = self.api('/bindings/binding-default')
        self.api('/bindings', {**binding, 'id': 'binding-incident', 'name': 'Incident qualification', 'revision': 1,
                              'actions': ['invoke'], 'allowedPackageDigests': [uploaded['packageDigest'].removeprefix('sha256:')]}, 'POST')
        self.api('/teams', dict(id='incident-users', name='Incident requesters', enabled=True, revision=1, userIds=['reviewer-1'], roleIds=[]), 'POST')
        # Bootstrap reviewers are super-administrators. The requester must be an ordinary user,
        # otherwise the documented break-glass review override invalidates an independence test.
        knowledge=self.api('/grants/knowledge-human')
        self.api('/grants/knowledge-human',{**knowledge,'sourceId':'incident-users'},'PUT',revision=knowledge['revision'])
        administrators=self.api('/teams/installation-administrators')
        self.api('/teams/installation-administrators',{**administrators,
            'userIds':[user for user in administrators['userIds'] if user!='reviewer-1']},
            'PUT',revision=administrators['revision'])
        full = self.scope(list(self.tools))
        self.api('/grants', dict(id='incident-human', name='Incident user grant', enabled=True, revision=1,
                                sourceType='TEAM', sourceId='incident-users', purpose='HUMAN', scope=full), 'POST')
        self.actors = {}
        records = []
        for agent, names in AGENTS.items():
            self.api('/agents', dict(id=agent, name=agent, enabled=True, revision=1, ownerUserId='reviewer-1'), 'POST')
            group = agent+'-group'
            self.api('/agent-groups', dict(id=group, name=group, enabled=True, revision=1, agentIds=[agent], roleIds=[]), 'POST')
            scope = self.scope(names)
            self.api('/grants', dict(id=agent+'-ceiling', name=agent+' ceiling', enabled=True, revision=1,
                                    sourceType='AGENT_GROUP', sourceId=group, purpose='CAPABILITY', scope=scope), 'POST')
            self.api('/delegations', dict(id=agent+'-delegation', name=agent+' delegation', enabled=True, revision=1,
                                         teamId='incident-users', agentGroupId=group, scope=scope), 'POST')
            token = secrets.token_urlsafe(48)
            digest = hashlib.sha256(token.encode()).hexdigest()
            workload = agent+'-workload'
            self.api('/workload-bindings', dict(id=workload, name=workload, enabled=True, revision=1, agentId=agent,
                mode='DELEGATED', issuer='gateway', subject=workload, audience='gateway', credentialSha256=digest,
                credentialEpoch=1, expiresAtUnixMs=int(time.time()*1000)+3600000), 'POST')
            records.append(dict(tokenSha256=digest, expiresAtUnixMs=int(time.time()*1000)+3600000,
                context=dict(requestId='incident-actor', tenantId=self.tenant, mode='DELEGATED', credentialKind='STATIC_TOKEN',
                             agentId=agent, workloadBindingId=workload, chain=[], credentialEpoch=1,
                             bindingId='binding-incident', credentialSha256=digest)))
            self.actors[agent] = token
        # A dedicated Gateway credential file keeps the supervisor's own credentials independent.
        self.script("from pathlib import Path;import json,sys; p=Path('/data/run/incident-credentials.json');"
                    "p.write_text(json.dumps(json.loads(Path('/data/run/credentials.json').read_text())+json.loads(sys.argv[1])))", json.dumps(records))
        self.script("from pathlib import Path; p=Path('/data/run/qualification-gateway.json');"
                    "import json,sys;v=json.loads(p.read_text());v['listen']='0.0.0.0:8085';v['managementListen']='127.0.0.1:9095';"
                    "v['tenantHosts']=['control:8085',sys.argv[1]];Path('/data/run/incident-gateway.json').write_text(json.dumps(v))",
                    '127.0.0.1:'+json.loads(docker('inspect',self.control))[0]['NetworkSettings']['Ports']['8085/tcp'][0]['HostPort'])
        docker('exec', '-d', self.control, 'env', 'TOOLGATE_GATEWAY_CONFIG=/data/run/incident-gateway.json',
               'TOOLGATE_GATEWAY_CREDENTIALS=/data/run/incident-credentials.json', 'sh', '-c',
               'exec /usr/local/bin/olo-toolgate-gateway > /data/run/incident-gateway.log 2>&1')
        self.api('/policies', dict(id='incident-review', name='Independent rollback approval', enabled=True, revision=1,
            scope=self.scope(['remediate']), decision='ASK', teams=dict(ids=[], all=True), agentGroups=dict(ids=[], all=True),
            approverTeams=dict(ids=['installation-administrators'], all=False)), 'POST')
        self.incident_gateway='http://127.0.0.1:'+json.loads(docker('inspect',self.control))[0]['NetworkSettings']['Ports']['8085/tcp'][0]['HostPort']
        self.wait(lambda:self.actor_catalog('triage-agent') == {self.tools[name] for name in AGENTS['triage-agent']},
                  'Deployed delegated incident catalog',120)
        return self

    def actor_catalog(self,agent):
        status,response=self.mcp('reviewer-1',method='tools/list',actor=self.actors[agent],gateway=self.incident_gateway)
        return {tool['name'] for tool in response.get('result',{}).get('tools',[])} if status==200 else set()

    def qualify_approval_terminals(self):
        def invoke(operation,arguments):
            return self.mcp('reviewer-1',params=dict(name=self.tools['remediate'],arguments=arguments),operation=operation,
                            actor=self.actors['remediation-agent'],gateway=self.incident_gateway)
        def pending(label):
            operation=str(uuid.uuid4())
            arguments=dict(host='app-02',service='payments-api',action='ROLLBACK_DEPLOY',reference='INC-5001-'+label)
            status,response=invoke(operation,arguments)
            content=response.get('result',{}).get('structuredContent',{})
            self.require(status==200 and content.get('state')=='PENDING_APPROVAL','Negative approval case was not pending')
            approval=next(item for item in self.api('/approvals')['items'] if item['invocationId']==content['invocationId'])
            return operation,arguments,approval
        def review(approval,decision):
            return http(self.origin+'/api/control/v1/approvals/'+approval['id']+'/decision',self.tokens['reviewer-2'],
                dict(obligationId='incident-review',expectedRevision=approval['revision'],decision=decision),
                'POST',{'Idempotency-Key':str(uuid.uuid4())})
        evidence={}
        operation,arguments,approval=pending('denied')
        status,decision=review(approval,'DENY')
        self.require(status==200 and decision.get('state')=='DENIED','Independent denial was not recorded')
        status,response=invoke(operation,arguments)
        ended=response.get('result',{}).get('structuredContent',{})
        self.require(status==200 and ended.get('invocationId')==approval['invocationId'] and ended.get('state')=='FAILED',
                     'Denied original operation restarted an approval wait or execution')
        evidence['denial']=dict(invocationId=approval['invocationId'],state='FAILED')
        operation,arguments,approval=pending('cancelled')
        status,response=self.mcp('reviewer-1',method='olo/invocations/cancel',params={'operationId':operation},
                                actor=self.actors['remediation-agent'],gateway=self.incident_gateway)
        self.artifact('incident-cancellation.json',json.dumps(dict(status=status,response=response),indent=2)+'\n')
        self.require(status==200 and response.get('result',{}).get('cancelled') is True,'Original operation cancellation failed')
        status,response=invoke(operation,arguments)
        ended=response.get('result',{}).get('structuredContent',{})
        retained=self.api('/access/invocations/'+approval['invocationId'])
        code=response.get('code') or response.get('error',{}).get('data',{}).get('code')
        terminal=status==200 and ended.get('invocationId')==approval['invocationId'] and ended.get('state')=='CANCELLED'
        # Control's durable cancel marker intentionally rejects submission before placement.
        rejected=status==409 and code=='CONFLICT'
        self.artifact('incident-cancellation-replay.json',json.dumps(dict(status=status,response=response,
            invocationId=retained['id'],state=retained['state'],completedResources=retained['completedResources'],
            permitReserved=retained.get('reservedNonce') is not None),indent=2)+'\n')
        self.require((terminal or rejected) and retained['id']==approval['invocationId']
                     and retained['state']=='CANCELLED' and not retained['completedResources']
                     and retained.get('reservedNonce') is None,
                     'Cancellation replay did not preserve the original cancelled invocation without dispatch')
        evidence['cancellation']=dict(invocationId=approval['invocationId'],state='CANCELLED',replayStatus=status,
                                     dispatched=False)
        operation,arguments,approval=pending('revoked-user')
        grant=self.api('/grants/incident-human')
        self.api('/grants/incident-human',{**grant,'enabled':False},'PUT',revision=grant['revision'])
        try:
            status,_=review(approval,'APPROVE')
            self.require(status==403,'Approval manufactured a revoked user grant')
            status,response=invoke(operation,arguments)
            self.require(policy_denied(status,response),'Revoked user call did not receive an explicit authorization denial')
        finally:
            grant=self.api('/grants/incident-human')
            self.api('/grants/incident-human',{**grant,'enabled':True},'PUT',revision=grant['revision'])
        retained=self.api('/access/invocations/'+approval['invocationId'])
        self.require(retained['state']=='PENDING_APPROVAL','Revoked user operation reached dispatch')
        evidence['revokedUser']=dict(invocationId=approval['invocationId'],approvalDenied=True,dispatched=False)
        # Exercise an actual authorization deadline without shortening the seven-day approval contract.
        grant=self.api('/grants/incident-human')
        self.api('/grants/incident-human',{**grant,'enabled':False},'PUT',revision=grant['revision'])
        scope=self.scope(['remediate']);deadline=int(time.time()*1000)+8000
        scope['conditions']['expiresAtUnixMs']=deadline
        self.api('/grants',dict(id='incident-expiring',name='Bounded approval qualification',enabled=True,revision=1,
                              sourceType='TEAM',sourceId='incident-users',purpose='HUMAN',scope=scope),'POST')
        try:
            operation,arguments,approval=pending('expired-authority')
            self.wait(lambda:int(time.time()*1000)>deadline,'User grant authorization deadline',15)
            status,_=review(approval,'APPROVE')
            self.require(status==403,'Approval extended an expired user grant')
            status,response=invoke(operation,arguments)
            self.require(policy_denied(status,response),'Expired user authority did not block dispatch')
            evidence['expiredAuthority']=dict(invocationId=approval['invocationId'],approvalDenied=True,dispatched=False)
        finally:
            for identifier,enabled in [('incident-expiring',False),('incident-human',True)]:
                grant=self.api('/grants/'+identifier)
                self.api('/grants/'+identifier,{**grant,'enabled':enabled},'PUT',revision=grant['revision'])
        self.artifact('incident-terminal-approvals.json',json.dumps(evidence,indent=2)+'\n')
        print('PASS real approval denial, cancellation, revoked user grants and expired authority block original operations',flush=True)
        return evidence

    def qualify_incident(self):
        self.require(self.tools['remediate'] not in self.actor_catalog('triage-agent'), 'Triage catalog exposes remediation')
        status,denied=self.mcp('reviewer-2',params=dict(name=self.tools['remediate'],arguments=dict(
            host='app-02',service='payments-api',action='ROLLBACK_DEPLOY',reference='INC-5001-denied')),
            actor=self.actors['remediation-agent'],gateway=self.incident_gateway)
        self.require(policy_denied(status,denied), 'Agent ceiling substituted for missing user grant')
        root = '/api/v1/conversations/'+self.conversation
        status, _ = self.api_call(root, method='POST')
        self.require(status == 200, 'Incident conversation creation failed')
        status, accepted = self.api_call(root+'/turns', {'message': 'Resolve fictional INC-5001. '+self.canary}, 'POST')
        self.require(status == 202, 'Incident turn admission failed')
        turn = accepted['runId']
        def pending():
            items = self.api('/approvals')['items']
            return next((item for item in items if item.get('state') == 'PENDING'), None)
        approval = self.wait(pending, 'Real incident approval', 180)
        invocation = self.api('/access/invocations/'+approval['invocationId'])
        self.require(invocation['state'] == 'PENDING_APPROVAL', 'Remediation ran before independent approval')
        # Crash the actual worker after triage and during the child remediation's ASK wait.
        docker('kill', self.worker)
        docker('start', self.worker)
        decision = dict(obligationId='incident-review', expectedRevision=approval['revision'], decision='APPROVE')
        status, _ = http(self.origin+'/api/control/v1/approvals/'+approval['id']+'/decision',
                         self.tokens['reviewer-1'], decision, 'POST', {'Idempotency-Key': str(uuid.uuid4())})
        self.require(status == 403, 'Requester approved their own remediation')
        self.api('/approvals/'+approval['id']+'/decision', decision, 'POST', user='reviewer-2')
        history = self.turn_history(root, turn)
        self.require(sorted(v['role'] for v in history['messages']) == ['assistant', 'user'], 'Incident turn duplicated durable messages')
        original = self.api('/access/invocations/'+approval['invocationId'])
        self.require(original['state'] == 'SUCCEEDED', 'Approved original invocation did not succeed')
        raw = docker('exec', self.temporal, 'temporal', 'workflow', 'show',
                     '--workflow-id', 'run-'+self.tenant+'~'+turn, '--output', 'json')
        events = json.loads(raw)['events']
        started = [event['startChildWorkflowExecutionInitiatedEventAttributes'] for event in events
                   if 'startChildWorkflowExecutionInitiatedEventAttributes' in event]
        completed_children = [event for event in events if 'childWorkflowExecutionCompletedEventAttributes' in event]
        self.require(len(started)==3 and len(completed_children)==3,
                     'Commander did not start and complete exactly three real Temporal child workflows')
        invocations = self.api('/access/invocations?limit=100')['items']
        completed = [item for item in invocations if item['evaluation']['toolId'] in self.tools.values()
                     and item['state']=='SUCCEEDED']
        self.require(len(completed)==7 and {item['evaluation']['toolId'] for item in completed}==set(self.tools.values()),
                     'Incident tools did not each succeed exactly once')
        evidence=[]
        expected={self.tools[name]:agent for agent,names in AGENTS.items() for name in names}
        for item in completed:
            evaluation=item['evaluation'];context=evaluation['context']
            self.require(context['mode']=='DELEGATED' and context['userId']=='reviewer-1'
                         and context['agentId']==expected[evaluation['toolId']], 'Incident call has incorrect subject or actor')
            self.require(evaluation['resources']==[dict(kind='CUSTOM',locator='tool://'+evaluation['toolId'])],
                         'Incident call did not bind the exact tool resource')
            evidence.append(dict(invocationId=item['id'], tool=evaluation['toolId'], subject=context['userId'],
                                 actor=context['agentId'], state=item['state'], resources=evaluation['resources']))
        self.artifact('incident-invocations.json',json.dumps(evidence,indent=2)+'\n')
        audited=[];cursor='0';seen=set()
        identifiers={item['id'] for item in completed}
        for _ in range(100):
            page=self.api('/audit?cursor='+cursor+'&limit=100')
            for event in page['items']:
                detail=event.get('evidence') or {}
                if detail.get('invocationId') in identifiers:
                    self.require(detail.get('mode')=='DELEGATED' and detail.get('subjectUserId')=='reviewer-1'
                                 and detail.get('actorAgentId')==expected[detail['toolId']],
                                 'ToolGate audit lost the delegated subject or actor')
                    audited.append({key:event[key] for key in ('sequence','operation','target','requestId','evidence')})
                    if detail.get('state')=='SUCCEEDED':seen.add(detail['invocationId'])
            if 'nextCursor' not in page:break
            self.require(page['nextCursor']!=cursor,'Audit cursor did not advance')
            cursor=page['nextCursor']
        self.require(seen==identifiers,'Missing ToolGate completion audit for an incident tool')
        self.artifact('incident-audit.json',json.dumps(audited,indent=2)+'\n')
        terminals=self.qualify_approval_terminals()
        for container in (self.backend, self.worker, self.control, *self.services.values()):
            logs=run(['docker','logs',container],text=True)
            self.require(self.canary not in logs.stdout+logs.stderr,'Private incident text leaked into diagnostics')
        images={name:dict(imageId=info['Id'],sourceRevision=(info['Config'].get('Labels') or {}).get('io.olo.qualification.source-revision',
                        (info['Config'].get('Labels') or {}).get('org.opencontainers.image.revision')))
                for name in ('olo-backend:olo-qualification','olo-worker:olo-qualification',self.image)
                for info in json.loads(docker('image','inspect',name))}
        report = dict(passed=True, runId=self.name, turnId=turn, approvedInvocationId=approval['invocationId'],
            originalInvocationResumed=True,completionAuditVerified=True,packageSha256=hashlib.sha256(self.package.read_bytes()).hexdigest(),
            commanderGeneratedDelegation=True, temporalChildWorkflows=3, workerCrashDuringChildApproval=True,
            approvalTerminals=terminals,images=images,
            knowledgeRevision=run(['git','rev-parse','HEAD'],cwd=ROOT,text=True).stdout.strip(),
            knowledgeWorktreeDirty=bool(run(['git','status','--porcelain'],cwd=ROOT,text=True).stdout.strip()),
            scope='Real backend/worker/Temporal/Gateway/Control/Tool Host and official fictional incident package; scripted model, development transport. Not complete M8/M10.')
        self.artifact('incident.json', json.dumps(report, indent=2)+'\n')
        print('PASS real delegated incident stages and independent approval resume the original invocation', flush=True)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--package', type=pathlib.Path, required=True)
    instance = Incident(parser.parse_args().package)
    try:
        instance.start().start_services()
        instance.provision_incident().start_pipeline()
        instance.qualify_incident()
    except BaseException as error:
        instance.artifact('incident.json',json.dumps(dict(passed=False,runId=instance.name,failureType=type(error).__name__))+'\n')
        raise
    finally:
        instance.close()
