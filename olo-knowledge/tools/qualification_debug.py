"""Keep one owned, fresh governed debug stack alive until the stop script removes it."""
import json
import os
import signal
import threading

from qualification_pipeline import Pipeline
from qualification_stack import ROOT, run


class DebugStack(Pipeline):
    report_name = 'debug-readiness.json'


def publish(path, value):
    temporary = path.with_suffix('.tmp')
    temporary.write_text(json.dumps(value, indent=2)+'\n')
    os.replace(temporary, path)


if __name__ == '__main__':
    stack = DebugStack()
    record = json.loads(stack.process_file.read_text())
    record['script'] = 'qualification_debug.py'
    stack.process_file.write_text(json.dumps(record))
    manifest = ROOT/'build/qualification/runtime/debug-session.json'
    stopped = threading.Event()
    for name in ('SIGTERM', 'SIGINT'):
        signal.signal(getattr(signal, name), lambda *_: stopped.set())
    state = dict(workspace=str(ROOT), pid=os.getpid(), session=stack.name, status='STARTING')
    publish(manifest, state)
    try:
        stack.start().start_services()
        stack.start_pipeline()
        state.update(status='READY', backend=stack.api_origin, gateway=stack.gateway,
                     containers=stack.containers, scope='Disposable development transport; scripted IdP/model; not production qualification')
        publish(manifest, state)
        stack.artifact('debug-readiness.json',json.dumps(dict(passed=True,runId=stack.name,
            knowledgeRevision=run(['git','rev-parse','HEAD'],cwd=ROOT,text=True).stdout.strip(),
            knowledgeWorktreeDirty=bool(run(['git','status','--porcelain'],cwd=ROOT,text=True).stdout.strip()),
            scope=state['scope'],backendWorkerAndGovernedServicesReady=True),indent=2)+'\n')
        print('Owned governed debug stack ready. Use governed-debug.ps1 down to remove all owned runtime resources.', flush=True)
        stopped.wait()
    except BaseException as error:
        state.update(status='FAILED', failureType=type(error).__name__)
        publish(manifest, state)
        raise
    finally:
        stack.close()
