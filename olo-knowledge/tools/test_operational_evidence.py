"""Ambiguous failures cannot count as an explicit durable unknown outcome."""
import unittest
from qualification_operations import unknown_invocation


class UnknownOutcomeEvidenceTest(unittest.TestCase):
    def test_dependency_failure_is_not_unknown_evidence(self):
        self.assertIsNone(unknown_invocation({'error':{'data':{'code':'DEPENDENCY_UNAVAILABLE','requestId':'inv-1'}}}))
        self.assertIsNone(unknown_invocation({'result':{'structuredContent':{'state':'FAILED','invocationId':'inv-1'}}}))

    def test_both_initial_and_replay_wire_shapes_preserve_invocation_id(self):
        self.assertEqual('inv-1',unknown_invocation({'error':{'data':{'outcome':'OUTCOME_UNKNOWN','requestId':'inv-1'}}}))
        self.assertEqual('inv-1',unknown_invocation({'result':{'structuredContent':{'state':'OUTCOME_UNKNOWN','invocationId':'inv-1'}}}))


if __name__=='__main__':
    unittest.main()
