/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { describe, expect, it } from 'vitest'
import {
  toggleAvailableAgent,
  toggleCatalogTool,
  toggleChildWorkflow,
  toolFromCatalog,
  upsertVariable,
} from './workflowResources'
import type { WorkflowDocument } from '../types/workflow'

const baseDoc: WorkflowDocument = { id: 'agent', variables: [], tools: [], hooks: [] }

describe('workflowResources', () => {
  it('upserts variables by name', () => {
    const doc = upsertVariable(baseDoc, {
      name: 'ReturnValue',
      type: 'string',
      description: 'result',
      scope: 'LOCAL',
    })
    expect(doc.variables).toHaveLength(1)
    expect(doc.variables?.[0].scope).toBe('LOCAL')
    const updated = upsertVariable(doc, {
      name: 'ReturnValue',
      type: 'string',
      description: 'updated',
    })
    expect(updated.variables?.[0].description).toBe('updated')
  })

  it('toggles catalog tools', () => {
    const item = {
      id: 'olo-core:calculator',
      kind: 'TOOL',
      name: 'Calculator',
      description: 'Math',
    }
    const enabled = toggleCatalogTool(baseDoc, item, true)
    expect(enabled.tools).toHaveLength(1)
    expect(enabled.tools?.[0].id).toBe('calculator')
    const disabled = toggleCatalogTool(enabled, item, false)
    expect(disabled.tools).toHaveLength(0)
  })

  it('creates tool from catalog', () => {
    const tool = toolFromCatalog({
      id: 'olo-core:calculator',
      name: 'Calculator',
      description: 'Math',
    })
    expect(tool.runtimeBinding?.implementationId).toBe('olo-core:calculator')
  })

  it('toggles child workflows and agents', () => {
    let doc = toggleChildWorkflow(baseDoc, 'planner', '1.0.0', true)
    expect(doc.childWorkflows?.[0].workflowId).toBe('planner')
    doc = toggleAvailableAgent(doc, 'reviewer', true)
    expect(doc.availableAgents?.[0].id).toBe('reviewer')
  })
})
