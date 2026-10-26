/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { describe, expect, it } from 'vitest'
import { layoutLogWorkflowNodes, mainMessagePath } from './logGraphLayout'
import type { WorkflowDocument } from '../types/workflow'

const injectedAgentLog: WorkflowDocument = {
  id: 'agent',
  designer: {
    layout: { originX: 80, originY: 80, columnGap: 360, rowGap: 200, columns: 4 },
  },
  nodes: [
    { id: 'start', type: 'START', ports: [] },
    { id: 'calculator', type: 'TOOL', ports: [], configuration: { toolId: 'olo-core:calculator' } },
    { id: 'cpu-usage', type: 'TOOL', ports: [], configuration: { toolId: 'olo-core:cpu-usage' } },
    { id: 'agent', type: 'AGENT', ports: [], configuration: { designer: { position: { x: 440, y: 80 } } } },
    { id: 'end', type: 'END', ports: [], configuration: { designer: { position: { x: 800, y: 80 } } } },
    { id: 'tool-dyn-step-0', type: 'TOOL', label: 'Dyn-CPU Usage', ports: [] },
    { id: 'tool-dyn-synthesis', type: 'AGENT', label: 'Dyn-Tool synthesis', ports: [] },
  ],
  edges: [
    { sourceNodeId: 'start', sourcePortId: 'out', targetNodeId: 'agent', targetPortId: 'in' },
    { sourceNodeId: 'calculator', sourcePortId: 'capabilities', targetNodeId: 'agent', targetPortId: 'capabilities' },
    { sourceNodeId: 'cpu-usage', sourcePortId: 'capabilities', targetNodeId: 'agent', targetPortId: 'capabilities' },
    { sourceNodeId: 'agent', sourcePortId: 'out', targetNodeId: 'tool-dyn-step-0', targetPortId: 'in' },
    { sourceNodeId: 'tool-dyn-step-0', sourcePortId: 'out', targetNodeId: 'tool-dyn-synthesis', targetPortId: 'in' },
    { sourceNodeId: 'tool-dyn-synthesis', sourcePortId: 'out', targetNodeId: 'end', targetPortId: 'in' },
  ],
}

describe('logGraphLayout', () => {
  it('walks the injected main message path', () => {
    expect(mainMessagePath(injectedAgentLog.nodes ?? [], injectedAgentLog.edges ?? [])).toEqual([
      'start',
      'agent',
      'tool-dyn-step-0',
      'tool-dyn-synthesis',
      'end',
    ])
  })

  it('places main path horizontally and capability tools on a second row', () => {
    const positions = layoutLogWorkflowNodes(injectedAgentLog)

    expect(positions.get('start')).toEqual({ x: 80, y: 80 })
    expect(positions.get('agent')).toEqual({ x: 440, y: 80 })
    expect(positions.get('tool-dyn-step-0')).toEqual({ x: 800, y: 80 })
    expect(positions.get('tool-dyn-synthesis')).toEqual({ x: 1160, y: 80 })
    expect(positions.get('end')).toEqual({ x: 1520, y: 80 })

    expect(positions.get('calculator')).toEqual({ x: 80, y: 280 })
    expect(positions.get('cpu-usage')).toEqual({ x: 440, y: 280 })

    const unique = new Set(Array.from(positions.values()).map((point) => `${point.x},${point.y}`))
    expect(unique.size).toBe(positions.size)
  })
})
