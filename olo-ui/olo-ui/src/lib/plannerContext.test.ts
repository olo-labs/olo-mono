/* Copyright 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
import { describe, expect, it } from 'vitest'
import { generatePlannerPrompt, readPlannerContext, togglePlannerAgent, togglePlannerVariable, updatePlannerContext, validatePromptTemplate } from './plannerContext'
import type { WorkflowDocument } from '../types/workflow'
const doc: WorkflowDocument = { id: 'agent', nodes: [], variables: [{name: 'message', type: 'string', required: true}], availableAgents: [{id: 'planner'}], metadata: {plannerContext: {selectedVariables: ['message'], selectedAgents: [], injectAgents: true}} }
const agents = [{id: 'planner', label: 'Planner', description: 'Plans work'}]
describe('plannerContext', () => {
  it('keeps explicit empty agent selection empty', () => {
    expect(readPlannerContext(doc).selectedAgents).toEqual([])
    expect(generatePlannerPrompt('Delegate {availableAgentsJson}', readPlannerContext(doc), agents)).toBe('Delegate (none)')
  })
  it('expands canonical agent token once', () => {
    const selected = {...readPlannerContext(doc), selectedAgents: ['planner']}
    const text = generatePlannerPrompt('Delegate {availableAgentsJson}', selected, agents)
    expect(text).toContain('Planner')
    expect(text).not.toContain('{availableAgentsJson}')
    expect(text.match(/Planner/g)).toHaveLength(1)
  })
  it('does not expose agents when injection is disabled', () => {
    const selected = {...readPlannerContext(doc), selectedAgents: ['planner'], injectAgents: false}
    expect(generatePlannerPrompt('Delegate {availableAgentsJson}', selected, agents)).toBe('Delegate (none)')
  })
  it('synchronizes agent references and selection', () => {
    const enabled = togglePlannerAgent(doc, 'reviewer', true)
    expect(readPlannerContext(enabled).selectedAgents).toEqual(['reviewer'])
    expect(enabled.availableAgents?.map(ref => ref.id)).toEqual(['reviewer'])
    expect(readPlannerContext(togglePlannerAgent(enabled, 'reviewer', false)).selectedAgents).toEqual([])
  })
  it('synchronizes selected variables with required context', () => {
    const updated = togglePlannerVariable(doc, 'message', false)
    expect(updated.capability?.required_context).toEqual([])
    expect(updatePlannerContext(updated, {selectedVariables: ['message']}).capability?.required_context).toEqual(['message'])
  })
  it('reports undeclared prompt variables', () => {
    expect(validatePromptTemplate('{message} {missing} {availableAgentsJson}', doc)).toEqual([expect.objectContaining({code: 'dangling-placeholder', name: 'missing'})])
  })
})
