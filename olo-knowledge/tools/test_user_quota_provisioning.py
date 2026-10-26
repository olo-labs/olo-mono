"""Each user gets an independent durable ToolGate budget, with no shared authority fallback."""
import unittest
from unittest.mock import Mock
from qualification_stack import Stack
class QuotaProvisioningTest(unittest.TestCase):
    def test_single_user_grants_keep_exact_resources_and_independent_budgets(self):
        stack=Stack.__new__(Stack);stack.api=Mock()
        scope=dict(conditions={'requireOnline':True},resources=[{'locator':'olo://tenant/kb/id','match':'EXACT'}],actions=['rag.query'],allActions=False)
        stack.provision_user_grants(scope)
        teams=[call.args[1] for call in stack.api.call_args_list if call.args[0]=='/teams']
        grants=[call.args[1] for call in stack.api.call_args_list if call.args[0]=='/grants']
        self.assertEqual([['reviewer-1'],['reviewer-2']],[team['userIds'] for team in teams])
        self.assertEqual(2,len({grant['id'] for grant in grants}))
        self.assertEqual([team['id'] for team in teams],[grant['sourceId'] for grant in grants])
        for grant in grants:
            self.assertEqual(600,grant['scope']['conditions']['maxInvocationsPerMinute'])
            self.assertEqual(scope['resources'],grant['scope']['resources'])
            self.assertFalse(grant['scope']['allActions'])
        self.assertNotIn('maxInvocationsPerMinute',scope['conditions'])
