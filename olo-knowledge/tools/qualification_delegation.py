"""Derived child outputs and summaries remain subject to the user's live RAG authority."""
import json

from qualification_incident import incident_workflow, incident_child_workflows
from qualification_pipeline import Pipeline
from qualification_stack import docker, run, ROOT


class DelegatedRag(Pipeline):
    workflow_id = 'incident-recovery'
    report_name = 'delegated-rag.json'

    def __init__(self):
        super().__init__()
        record=json.loads(self.process_file.read_text())
        record['script']='qualification_delegation.py'
        self.process_file.write_text(json.dumps(record))

    def workflow_definitions(self):
        root=incident_workflow()
        root['label']='User-governed delegated RAG qualification'
        children=incident_child_workflows()
        for child in children:
            child['nodes'][1]['configuration'].update(maxToolRounds=0, agentName='unmapped-'+child['id'],
                promptTemplate='Summarize the authorized source for the parent workflow. {message}')
        return [root,*children]

    def held_model_offset(self):
        # Commander, three children, then the parent summary. Hold the fifth model response.
        return 5

    def verify_derived_turn(self,turn):
        raw=docker('exec',self.temporal,'temporal','workflow','show',
            '--workflow-id','run-'+self.tenant+'~'+turn,'--output','json')
        events=json.loads(raw)['events']
        children=[event for event in events if 'childWorkflowExecutionCompletedEventAttributes' in event]
        self.require(len(children)==3,'Summary did not consume three completed child workflows')

    def qualify(self):
        root='/api/v1/conversations/'+self.conversation
        status,_=self.api_call(root,method='POST')
        self.require(status==200,'Delegated conversation creation failed')
        revocation=self.derived_revocation(root)
        self.artifact(self.report_name,json.dumps(dict(passed=True,runId=self.name,
            revocation=revocation,completedChildren=3,summaryHeldDuringRevocation=True,
            unmappedChildAgentsCannotAuthorizeRag=True,
            sourceRevision=run(['git','rev-parse','HEAD'],cwd=ROOT,text=True).stdout.strip(),
            scope='Real backend/Temporal children and governed RAG; scripted model, development transport.'),indent=2)+'\n')
        print('PASS revoked source blocks child-derived summaries, stored history and the next model call',flush=True)


if __name__=='__main__':
    stack=DelegatedRag()
    try:
        stack.start().start_services()
        stack.start_pipeline()
        stack.qualify()
    except BaseException as failure:
        stack.artifact(stack.report_name,json.dumps(dict(passed=False,runId=stack.name,
            failureType=type(failure).__name__))+'\n')
        raise
    finally:
        stack.close()
