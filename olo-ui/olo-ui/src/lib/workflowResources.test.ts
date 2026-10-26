/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { describe, expect, it } from 'vitest'
import {
  toggleAvailableAgent,
  toggleCatalogHook,
  toggleChildWorkflow,
  hookFromCatalog,
  upsertVariable,
} from './workflowResources'
import type { WorkflowDocument } from '../types/workflow'

const baseDoc: WorkflowDocument = { id: 'agent', variables: [], hooks: [] }

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

  it('toggles catalog hooks', () => {
    const item = {
      id: 'olo-core:calculator',
      kind: 'HOOK',
      name: 'Calculator',
      description: 'Math',
    }
    const enabled = toggleCatalogHook(baseDoc, item, true)
    expect(enabled.hooks).toHaveLength(1)
    expect(enabled.hooks?.[0].id).toBe('calculator')
    const disabled = toggleCatalogHook(enabled, item, false)
    expect(disabled.hooks).toHaveLength(0)
  })

  it('creates hook from catalog', () => {
    const tool = hookFromCatalog({
      id: 'olo-core:calculator',
      name: 'Calculator',
      description: 'Math',
    })
    expect(tool.pre?.implementationId).toBe('olo-core:calculator')
  })

  it('toggles child workflows and agents', () => {
    let doc = toggleChildWorkflow(baseDoc, 'planner', '1.0.0', true)
    expect(doc.childWorkflows?.[0].workflowId).toBe('planner')
    doc = toggleAvailableAgent(doc, 'reviewer', true)
    expect(doc.availableAgents?.[0].id).toBe('reviewer')
  })
})
