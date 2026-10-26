"""An unavailable dependency must never count as successful authorization isolation."""
import unittest
from unittest.mock import Mock, patch

from qualification_stack import Stack, policy_denied


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


if __name__ == '__main__':
    unittest.main()
