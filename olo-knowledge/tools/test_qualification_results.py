"""An unavailable dependency must never count as successful authorization isolation."""
import unittest
from unittest.mock import Mock, patch

from qualification_stack import Stack, policy_denied, fenced_outcomes, source_digest


class QualificationResultsTest(unittest.TestCase):
    def test_backpressure_retries_reuse_the_exact_invocation_and_body(self):
        stack=Stack.__new__(Stack)
        stack.backpressure_events=[]
        stack.call=Mock(side_effect=[
            (503, {'error': {'data': {'code': 'DEPENDENCY_UNAVAILABLE'}}}),
            (200, {'result': {'structuredContent': {'json': '{"messages":[]}'}}}),
        ])
        body={'budget':4000}
        with patch('qualification_stack.time.sleep'):
            self.assertEqual((200,{'messages':[]}),stack.result('user','memory.read','root',body,'original-id'))
        self.assertEqual(stack.call.call_args_list[0],stack.call.call_args_list[1])
        self.assertEqual('original-id',stack.call.call_args.args[-1])
        self.assertEqual([503],stack.backpressure_events)

    def test_mcp_read_dependency_error_recovers_under_same_invocation(self):
        stack=Stack.__new__(Stack);stack.backpressure_events=[]
        stack.call=Mock(side_effect=[(200,{'error':{'data':{'code':'DEPENDENCY_UNAVAILABLE'}}}),(200,{'result':{'structuredContent':{'json':'{"chunks":[]}'}}})])
        with patch('qualification_stack.time.sleep'):
            self.assertEqual((200,{'chunks':[]}),stack.result('user','rag.query','root',{'query':'test'},'same-id'))
        self.assertEqual(stack.call.call_args_list[0],stack.call.call_args_list[1])

    def test_unknown_write_is_never_retried(self):
        stack=Stack.__new__(Stack)
        stack.backpressure_events=[]
        unknown=(503,{'error':{'data':{'outcome':'OUTCOME_UNKNOWN'}}})
        stack.call=Mock(return_value=unknown)
        self.assertEqual(unknown,stack.result('user','memory.append','root',{},'original-id'))
        stack.call.assert_called_once()
        self.assertEqual([],stack.backpressure_events)

    def test_unavailability_and_arbitrary_tool_errors_are_not_denials(self):
        for status, response in [
            (503, {'error': {'data': {'code': 'DEPENDENCY_UNAVAILABLE'}}}),
            (200, {'error': {'data': {'code': 'DEPENDENCY_UNAVAILABLE'}}}),
            (200, {'error': {'data': {'code': 'TIMEOUT'}}}),
            (200, {'result': {'isError': True}}),
            (200, {'messages': []}),
        ]:
            self.assertFalse(policy_denied(status, response))

    def test_explicit_gateway_or_policy_denials_are_recognized(self):
        for status, response in [
            (401, {}), (403, {}), (403, None),
            (200, {'error': {'data': {'code': 'FORBIDDEN'}}}),
            (200, {'result': {'structuredContent': {'state': 'DENIED'}}}),
        ]:
            self.assertTrue(policy_denied(status, response))

    def evidence(self):
        targets={name:[dict(effect_id=name+'-effect',auth_nonce=name+'-nonce',authorization_generation=1,
            state='DONE',outcome='COMPLETED',result_digest='sha256:'+'a'*64,delivered=1)] for name in ['memory','rag']}
        control=[dict(effect_id=name+'-effect',auth_nonce=name+'-nonce',generation=1,state='COMPLETED',consumer='olo_'+name)
            for name in ['memory','rag']]
        return targets,control

    def test_fenced_gate_requires_both_actual_target_outcomes(self):
        targets,control=self.evidence()
        ready,summary=fenced_outcomes(targets,control)
        self.assertTrue(ready);self.assertEqual(1,summary['memory']['completed'])
        targets['rag']=[]
        self.assertFalse(fenced_outcomes(targets,control)[0])

    def test_local_done_without_control_consumption_or_matching_binding_is_rejected(self):
        for change in ['missing','generation','consumer','conflict','intent']:
            targets,control=self.evidence()
            if change=='missing':control.pop()
            elif change=='generation':control[0]['generation']=2
            elif change=='consumer':control[0]['consumer']='olo_rag'
            elif change=='conflict':control[0]['state']='CONFLICT'
            else:targets['memory'][0]['outcome']=None
            with self.assertRaises(AssertionError,msg=change):fenced_outcomes(targets,control)

    def test_report_delivery_must_settle_and_cannot_be_acknowledged_early(self):
        targets,control=self.evidence()
        control[0]['state']='CONSUMED';targets['memory'][0]['delivered']=0
        self.assertFalse(fenced_outcomes(targets,control)[0])
        targets['memory'][0]['delivered']=1
        with self.assertRaises(AssertionError):fenced_outcomes(targets,control)

    def test_source_pins_cover_code_and_refuse_directory_links(self):
        import pathlib,tempfile
        with tempfile.TemporaryDirectory() as folder:
            root=pathlib.Path(folder)/'component';root.mkdir()
            code=root/'main.py';code.write_text('first')
            before=source_digest(root)
            (root/'build').mkdir();(root/'build/output.json').write_text('{}')
            self.assertEqual(before,source_digest(root))
            code.write_text('second');self.assertNotEqual(before,source_digest(root))
            outside=pathlib.Path(folder)/'outside';outside.mkdir();(outside/'other.py').write_text('outside')
            (root/'linked').symlink_to(outside,target_is_directory=True)
            with self.assertRaises(ValueError):source_digest(root)



if __name__ == '__main__':
    unittest.main()
