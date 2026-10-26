/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { describe, expect, it } from 'vitest'
import {
  catalogIdToWorkflowType,
  createWorkflowNodeFromCatalog,
  flowToWorkflow,
  uniqueNodeId,
  workflowToFlow,
} from './workflowGraph'
import type { WorkflowDocument } from '../types/workflow'

const agentWorkflowTemplate: WorkflowDocument = {
  id: 'agent',
  emoji: '🤖',
  designer: {
    nodeSize: { width: 300, height: 120 },
    nodeTypes: {
      START: {
        emoji: '▶',
        typeLabel: 'Start',
        palette: { name: 'Start' },
      },
      AGENT: {
        emoji: '🤖',
        typeLabel: 'Agent',
      },
      END: {
        emoji: '⏹',
        typeLabel: 'End',
        palette: { name: 'End' },
      },
    },
  },
  nodes: [
    {
      id: 'start',
      type: 'START',
      ports: [{ id: 'out', schema: 'message', type: 'message', direction: 'OUTPUT' }],
      reads: [],
      writes: [],
      configuration: { inputVariableMappings: [] },
    },
    {
      id: 'end',
      type: 'END',
      ports: [{ id: 'in', schema: 'message', type: 'message', direction: 'INPUT', acceptType: 'message' }],
      reads: [],
      writes: [],
      configuration: { outputVariableMapping: '' },
    },
  ],
  edges: [],
}

describe('workflowGraph', () => {
  it('maps catalog id to workflow type', () => {
    expect(catalogIdToWorkflowType('olo-core:AGENT')).toBe('AGENT')
    expect(catalogIdToWorkflowType('olo-core:START')).toBe('START')
  })

  it('generates unique node ids', () => {
    const taken = new Set(['agent', 'agent-2'])
    expect(uniqueNodeId('Agent', taken)).toBe('agent-3')
  })

  it('round-trips workflow nodes and edges through flow', () => {
    const workflow: WorkflowDocument = {
      id: 'test',
      designer: {
        nodeTypes: {
          START: { typeLabel: 'Start' },
          END: { typeLabel: 'End' },
        },
      },
      nodes: [
        {
          id: 'start',
          type: 'START',
          label: 'Workflow start',
          configuration: { designer: { position: { x: 10, y: 20 } } },
          ports: [],
        },
        {
          id: 'end',
          type: 'END',
          configuration: { designer: { position: { x: 300, y: 20 } } },
          ports: [],
        },
      ],
      edges: [{ sourceNodeId: 'start', targetNodeId: 'end' }],
    }

    const { nodes, edges } = workflowToFlow(workflow, null)
    expect(nodes).toHaveLength(2)
    expect(nodes[0].data.label).toBe('Workflow start')
    expect(nodes[0].position).toEqual({ x: 10, y: 20 })
    expect(nodes[0].data.presentation?.typeLabel).toBe('Start')
    expect(edges).toHaveLength(1)
    expect(edges[0].source).toBe('start')

    const back = flowToWorkflow(nodes, edges, workflow)
    expect(back.nodes?.[0].configuration?.designer).toEqual({ position: { x: 10, y: 20 } })
    expect(back.edges?.[0]).toEqual({ sourceNodeId: 'start', targetNodeId: 'end' })
  })

  it('creates workflow node from catalog item', () => {
    const node = createWorkflowNodeFromCatalog(
      {
        id: 'olo-core:AGENT',
        kind: 'NODE',
        name: 'Agent',
        emoji: '🤖',
        inputs: [{ id: 'in', schema: 'any' }],
        outputs: [{ id: 'out', schema: 'any' }],
      },
      { x: 100, y: 200 },
      [],
      null,
      agentWorkflowTemplate,
    )
    expect(node.type).toBe('AGENT')
    expect(node.id).toBe('agent')
    expect(node.label).toBe('Agent')
    expect(node.configuration?.designer).toEqual({ position: { x: 100, y: 200 } })
    expect(node.ports).toHaveLength(4)
    expect(node.ports?.some((port) => port.id === 'in' && port.direction === 'INPUT')).toBe(true)
    expect(node.ports?.some((port) => port.id === 'out' && port.direction === 'OUTPUT')).toBe(true)
  })

  it('clones start node from workflow template', () => {
    const node = createWorkflowNodeFromCatalog(
      {
        id: 'workflow-template:START',
        kind: 'NODE',
        name: 'Start',
        emoji: '▶',
      },
      { x: 40, y: 50 },
      ['start'],
      null,
      agentWorkflowTemplate,
    )
    expect(node.type).toBe('START')
    expect(node.id).toBe('start-2')
    expect(node.ports?.[0]?.schema).toBe('message')
  })
})
