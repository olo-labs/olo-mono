"""The scripted model must consume real catalogs and remediation results."""
import unittest
import json
from pipeline_dependencies import incident_reply, incident_delegation_reply
from qualification_incident import incident_workflow, incident_child_workflows


class IncidentFixtureTest(unittest.TestCase):
    def test_agent_catalog_is_a_ceiling(self):
        request = {'tools': [{'function': {'name': 'incidents_read'}}]}
        answer = incident_reply(request, [('incidents_remediate', {}), ('incidents_read', {})])
        self.assertEqual('incidents_read', answer['tool_calls'][0]['function']['name'])

    def test_verification_requires_the_real_result(self):
        request = {'tools': [{'function': {'name': 'incidents_verify'}}]}
        with self.assertRaisesRegex(ValueError, 'actual remediation'):
            incident_reply(request, [('incidents_verify', {'remediationId': 'REM-fake'})])
        request['messages'] = [{'role': 'tool', 'content': '{"remediationId":"REM-deadbeef"}'}]
        answer = incident_reply(request, [('incidents_verify', {})])
        self.assertEqual({'remediationId': 'REM-deadbeef'}, answer['tool_calls'][0]['function']['arguments'])

    def test_already_selected_operation_is_not_regenerated(self):
        request = {'tools': [{'function': {'name': 'incidents_remediate'}}], 'messages': [
            {'tool_calls': [{'function': {'name': 'incidents_remediate'}}]}]}
        self.assertNotIn('tool_calls', incident_reply(request, [('incidents_remediate', {})]))

    def test_new_workflow_has_explicit_agent_ceiling_and_no_legacy_tool_nodes(self):
        graph = incident_workflow()
        self.assertNotIn('tools', graph)
        commander = next(node for node in graph['nodes'] if node['id']=='commander')
        self.assertTrue(commander['configuration']['agentCallPlanner'])
        self.assertEqual(0, commander['configuration']['maxToolRounds'])
        self.assertEqual(3, len(graph['childWorkflows']))
        self.assertEqual('dynamic', graph['metadata']['agentSelectionStrategy'])
        self.assertTrue(graph['runtime']['delegation']['enabled'])
        self.assertEqual(3, graph['runtime']['delegation']['maxDelegations'])
        self.assertEqual(6, len(graph['edges']))
        for definition in [graph, *incident_child_workflows()]:
            for node in definition['nodes']:
                for port in node.get('ports', []):
                    self.assertTrue(port.get('label'), 'Serialized ports require a label')
        for child in incident_child_workflows():
            self.assertEqual(child['id'], child['nodes'][1]['configuration']['agentName'])
            self.assertEqual(5, child['nodes'][1]['configuration']['maxToolRounds'])
            self.assertNotIn('tools', child)

    def test_commander_can_delegate_only_the_rendered_catalog(self):
        request = {'messages':[{'content':'INCIDENT_DELEGATION_CATALOG:\n[{"agentId":"triage-agent"}]\nDelegate now.'}]}
        result = json.loads(incident_delegation_reply(request)['content'])
        self.assertEqual(['triage-agent'], [call['agentId'] for call in result['agentCalls']])
        self.assertNotIn('toolCalls', result)


if __name__ == '__main__':
    unittest.main()
