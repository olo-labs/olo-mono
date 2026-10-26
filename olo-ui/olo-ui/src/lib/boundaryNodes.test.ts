/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { describe, expect, it } from 'vitest'
import {
  END_NODE_TYPE,
  START_NODE_TYPE,
  applyEndOutputMapping,
  applyStartInputMappings,
  isEndNodeType,
  isStartNodeType,
  normalizeNodeType,
  normalizeWorkflowBoundaries,
} from './boundaryNodes'
import type { WorkflowDocument } from '../types/workflow'

describe('boundaryNodes', () => {
  it('normalizes legacy INPUT/OUTPUT types', () => {
    expect(normalizeNodeType('INPUT')).toBe(START_NODE_TYPE)
    expect(normalizeNodeType('OUTPUT')).toBe(END_NODE_TYPE)
    expect(isStartNodeType('INPUT')).toBe(true)
    expect(isEndNodeType('OUTPUT')).toBe(true)
  })

  it('migrates workflow nodes and edge references', () => {
    const doc: WorkflowDocument = {
      id: 'test',
      nodes: [
        { id: 'input', type: 'INPUT', ports: [], reads: [], writes: [] },
        { id: 'output', type: 'OUTPUT', ports: [], reads: [], writes: [] },
      ],
      edges: [{ sourceNodeId: 'input', targetNodeId: 'output' }],
    }
    const migrated = normalizeWorkflowBoundaries(doc)
    expect(migrated.nodes?.[0]).toMatchObject({ id: 'start', type: START_NODE_TYPE })
    expect(migrated.nodes?.[1]).toMatchObject({ id: 'end', type: END_NODE_TYPE })
    expect(migrated.edges?.[0]).toEqual({ sourceNodeId: 'start', targetNodeId: 'end' })
  })

  it('maps start input variables to reads paths', () => {
    const doc: WorkflowDocument = {
      id: 'test',
      nodes: [{ id: 'start', type: START_NODE_TYPE, ports: [], reads: [], writes: [] }],
    }
    const next = applyStartInputMappings(doc, 'start', ['customerId', 'orderId'])
    expect(next.nodes?.[0].reads).toEqual(['input.customerId', 'input.orderId'])
  })

  it('maps end output variable to metadata.returnVariable', () => {
    const doc: WorkflowDocument = {
      id: 'test',
      nodes: [{ id: 'end', type: END_NODE_TYPE, ports: [], reads: [], writes: [] }],
      metadata: {},
    }
    const next = applyEndOutputMapping(doc, 'end', 'ReturnValue')
    expect(next.metadata?.returnVariable).toBe('ReturnValue')
    expect(next.nodes?.[0].writes).toEqual(['state.ReturnValue'])
  })
})
