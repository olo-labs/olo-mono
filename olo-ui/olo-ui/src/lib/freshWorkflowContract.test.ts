/* Copyright (c) 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
import { afterEach, describe, expect, it, vi } from 'vitest'
import { parseWorkflowJson } from './workflowConfiguration'
import { getWorkflowConfiguration, saveWorkflowConfiguration } from '../api/restConfiguration'

afterEach(() => vi.unstubAllGlobals())

describe('fresh workflow boundary', () => {
  it.each([
    ['tools', { id: 'fresh', tools: [] }],
    ['toolCalls', { id: 'fresh', metadata: { toolCalls: [] } }],
    ['tool_requirements', { id: 'fresh', capability: { tool_requirements: [] } }],
    ['toolCallPlanner', { id: 'fresh', nodes: [{ type: 'AGENT', configuration: { toolCallPlanner: true } }] }],
    ['TOOL', { id: 'fresh', nodes: [{ id: 'local', type: 'TOOL' }] }],
    ['availableToolsJson', { id: 'fresh', nodes: [{ configuration: { promptTemplate: 'Use {availableToolsJson}' } }] }],
  ])('rejects %s with a field-specific error', (field, document) => {
    expect(() => parseWorkflowJson(JSON.stringify(document))).toThrow(String(field))
  })

  it('accepts a new agent graph with bounded business-tool rounds', () => {
    const graph = { id: 'fresh', nodes: [{ id: 'triage', type: 'AGENT', configuration: {
      agentName: 'triage-agent', maxToolRounds: 3, agentCallPlanner: false,
    } }] }
    expect(parseWorkflowJson(JSON.stringify(graph))).toEqual(graph)
  })

  it('rejects a legacy response from the configuration API', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: async () => ({ id: 'old', tools: [] }) }))
    await expect(getWorkflowConfiguration('old.json')).rejects.toThrow('tools')
  })

  it('does not send an invalid save request', async () => {
    const fetch = vi.fn()
    vi.stubGlobal('fetch', fetch)
    await expect(saveWorkflowConfiguration('old.json', { id: 'old', tools: [] })).rejects.toThrow('tools')
    expect(fetch).not.toHaveBeenCalled()
  })
})
