/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { describe, expect, it } from 'vitest'
import {
  defaultNodePosition,
  readWorkflowDesigner,
  resolveCanvasTheme,
  resolveLayoutGrid,
  resolveNodeSize,
  resolvePortColors,
} from './workflowDesigner'
import { resolveNodePresentation } from './nodePresentation'
import type { WorkflowDocument } from '../types/workflow'

const agentPreset: WorkflowDocument = {
  id: 'agent',
  emoji: '🤖',
  designer: {
    nodeSize: { width: 300, height: 120 },
    layout: { originX: 80, originY: 80, columnGap: 360, rowGap: 200, columns: 4 },
    canvas: { backgroundColor: '#3f3f46' },
    portColors: { message: '#ef4444' },
    nodeTypes: {
      START: {
        typeLabel: 'Start',
        inlineProperties: [{ id: 'inputVariables', widget: 'VARIABLE_CHECKLIST' }],
      },
      AGENT: {
        typeLabel: 'Agent',
        inlineProperties: [
          { id: 'parameters', widget: 'WORKFLOW_PARAMETERS' },
          { id: 'model', widget: 'MODEL_SELECTOR' },
        ],
      },
      END: {
        typeLabel: 'End',
        inlineProperties: [{ id: 'outputVariable', widget: 'VARIABLE_SELECT' }],
      },
    },
  },
  nodes: [{ id: 'start', type: 'START', ports: [] }],
}

describe('workflowDesigner', () => {
  it('reads studio layout from generated agent preset', () => {
    const designer = readWorkflowDesigner(agentPreset)

    expect(designer.nodeSize).toEqual({ width: 300, height: 120 })
    expect(resolveLayoutGrid(agentPreset)).toEqual({
      originX: 80,
      originY: 80,
      columnGap: 360,
      rowGap: 200,
      columns: 4,
    })
    expect(resolveNodeSize(agentPreset)).toEqual({ width: 300, height: 120 })
    expect(resolveCanvasTheme(agentPreset).backgroundColor).toBe('#3f3f46')
    expect(resolvePortColors(agentPreset).message).toBe('#ef4444')
    expect(designer.nodeTypes?.START?.typeLabel).toBe('Start')
    expect(designer.nodeTypes?.AGENT?.inlineProperties).toHaveLength(2)
    expect(designer.nodeTypes?.END?.inlineProperties).toHaveLength(1)
  })

  it('resolves node presentation from workflow designer', () => {
    const startNode = agentPreset.nodes?.find((node) => node.type === 'START')
    expect(startNode).toBeDefined()
    const presentation = resolveNodePresentation(agentPreset, startNode!, null)
    expect(presentation.typeLabel).toBe('Start')
    expect(presentation.inlineProperties[0]?.widget).toBe('VARIABLE_CHECKLIST')
  })

  it('uses designer layout for default node positions', () => {
    expect(defaultNodePosition(agentPreset, 0)).toEqual({ x: 80, y: 80 })
    expect(defaultNodePosition(agentPreset, 1)).toEqual({ x: 440, y: 80 })
  })
})
