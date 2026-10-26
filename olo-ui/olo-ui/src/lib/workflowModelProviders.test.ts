/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { describe, expect, it } from 'vitest'
import {
  DEFAULT_PROVIDER_ID,
  DEFAULT_ROUTING_ID,
  applyAgentModelSelection,
  applyProviderRef,
  ensureWorkflowModelInfrastructure,
  providerKind,
  readAgentModelSelection,
  upsertModelProvider,
  workflowModelProviders,
  workflowModelRouting,
} from './workflowModelProviders'
import type { WorkflowDocument } from '../types/workflow'

const baseDoc: WorkflowDocument = { id: 'agent', modelProviders: [] }

describe('workflowModelProviders', () => {
  it('upserts providers by id', () => {
    const doc = upsertModelProvider(baseDoc, {
      id: 'local-llm',
      provider: 'local',
      model: 'llama3.2',
      configuration: { baseUrl: 'http://localhost:51435' },
    })
    expect(workflowModelProviders(doc).some((p) => p.id === 'local-llm')).toBe(true)
    const updated = upsertModelProvider(doc, {
      id: 'local-llm',
      provider: 'local',
      model: 'mistral',
      configuration: { baseUrl: 'http://localhost:51435' },
    })
    expect(workflowModelProviders(updated).find((p) => p.id === 'local-llm')?.model).toBe('mistral')
  })

  it('maps ollama to local kind', () => {
    expect(providerKind({ id: 'x', provider: 'ollama' })).toBe('local')
  })

  it('sets providerRef on model nodes', () => {
    const doc: WorkflowDocument = {
      ...ensureWorkflowModelInfrastructure(baseDoc),
      nodes: [{ id: 'model', type: 'MODEL', configuration: {} }],
    }
    const next = applyProviderRef(doc, 'model', 'openai-default')
    expect(next.nodes?.[0].configuration?.providerRef).toBe('openai-default')
  })

  it('ensures default provider and routing on empty workflow', () => {
    const doc = ensureWorkflowModelInfrastructure({ id: 'wf' })
    expect(workflowModelProviders(doc)[0].id).toBe(DEFAULT_PROVIDER_ID)
    expect(workflowModelRouting(doc)[0]).toMatchObject({
      id: DEFAULT_ROUTING_ID,
      defaultProviderId: DEFAULT_PROVIDER_ID,
    })
  })

  it('applies routingRef on agent nodes', () => {
    const doc: WorkflowDocument = {
      ...ensureWorkflowModelInfrastructure(baseDoc),
      nodes: [{ id: 'agent', type: 'AGENT', configuration: {} }],
    }
    const next = applyAgentModelSelection(doc, 'agent', `routing:${DEFAULT_ROUTING_ID}`)
    expect(next.nodes?.[0].configuration?.routingRef).toBe(DEFAULT_ROUTING_ID)
    expect(next.nodes?.[0].configuration?.providerRef).toBeUndefined()
  })

  it('defaults agent selection to workflow routing', () => {
    const doc: WorkflowDocument = {
      ...ensureWorkflowModelInfrastructure(baseDoc),
      nodes: [{ id: 'agent', type: 'AGENT', configuration: {} }],
    }
    const node = doc.nodes![0]
    expect(readAgentModelSelection(node, doc)).toBe(`routing:${DEFAULT_ROUTING_ID}`)
  })
})
