/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type {
  NodeDesignerConfig,
  NodeTypeDesignerConfig,
  WorkflowDesigner,
  WorkflowDocument,
  WorkflowNode,
} from '../types/workflow'
import { normalizeNodeType } from './boundaryNodes'

const DEFAULT_LAYOUT = {
  originX: 80,
  originY: 80,
  columnGap: 360,
  rowGap: 200,
  columns: 4,
} as const

const DEFAULT_NODE_SIZE = {
  width: 300,
  height: 120,
} as const

const DEFAULT_CANVAS = {
  backgroundColor: '#3f3f46',
  gridGap: 16,
  edgeStroke: '#52525b',
  selectionBorder: '#3b82f6',
  minimapNodeColor: '#52525b',
} as const

export function readWorkflowDesigner(workflow: WorkflowDocument | null | undefined): WorkflowDesigner {
  return workflow?.designer ?? {}
}

export function readNodeDesigner(node: WorkflowNode): NodeDesignerConfig {
  const designer = node.configuration?.designer
  if (!designer || typeof designer !== 'object') return {}
  return designer as NodeDesignerConfig
}

export function readNodeTypeDesigner(
  workflow: WorkflowDocument | null | undefined,
  nodeType: string,
): NodeTypeDesignerConfig {
  const normalized = normalizeNodeType(nodeType)
  return readWorkflowDesigner(workflow).nodeTypes?.[normalized] ?? {}
}

export function resolveLayoutGrid(workflow: WorkflowDocument | null | undefined) {
  const layout = readWorkflowDesigner(workflow).layout ?? {}
  return {
    originX: layout.originX ?? DEFAULT_LAYOUT.originX,
    originY: layout.originY ?? DEFAULT_LAYOUT.originY,
    columnGap: layout.columnGap ?? DEFAULT_LAYOUT.columnGap,
    rowGap: layout.rowGap ?? DEFAULT_LAYOUT.rowGap,
    columns: layout.columns ?? DEFAULT_LAYOUT.columns,
  }
}

export function defaultNodePosition(
  workflow: WorkflowDocument | null | undefined,
  index: number,
): { x: number; y: number } {
  const grid = resolveLayoutGrid(workflow)
  const col = index % grid.columns
  const row = Math.floor(index / grid.columns)
  return {
    x: grid.originX + col * grid.columnGap,
    y: grid.originY + row * grid.rowGap,
  }
}

export function resolveNodeSize(workflow: WorkflowDocument | null | undefined) {
  const size = readWorkflowDesigner(workflow).nodeSize ?? {}
  return {
    width: size.width ?? DEFAULT_NODE_SIZE.width,
    height: size.height ?? DEFAULT_NODE_SIZE.height,
  }
}

export function resolveCanvasTheme(workflow: WorkflowDocument | null | undefined) {
  const canvas = readWorkflowDesigner(workflow).canvas ?? {}
  return {
    backgroundColor: canvas.backgroundColor ?? DEFAULT_CANVAS.backgroundColor,
    gridGap: canvas.gridGap ?? DEFAULT_CANVAS.gridGap,
    edgeStroke: canvas.edgeStroke ?? DEFAULT_CANVAS.edgeStroke,
    selectionBorder: canvas.selectionBorder ?? DEFAULT_CANVAS.selectionBorder,
    minimapNodeColor: canvas.minimapNodeColor ?? DEFAULT_CANVAS.minimapNodeColor,
  }
}

export function resolvePortColors(
  workflow: WorkflowDocument | null | undefined,
): Record<string, string> {
  return readWorkflowDesigner(workflow).portColors ?? {}
}

export function isNodeDraggable(
  workflow: WorkflowDocument | null | undefined,
  node?: WorkflowNode,
): boolean {
  const nodeDesigner = node ? readNodeDesigner(node) : {}
  if (typeof (nodeDesigner as { draggable?: boolean }).draggable === 'boolean') {
    return (nodeDesigner as { draggable?: boolean }).draggable!
  }
  const workflowDesigner = readWorkflowDesigner(workflow)
  return workflowDesigner.draggable ?? true
}

export function findWorkflowNodeTemplate(
  workflow: WorkflowDocument | null | undefined,
  nodeType: string,
): WorkflowNode | null {
  const normalized = normalizeNodeType(nodeType)
  return (workflow?.nodes ?? []).find((node) => normalizeNodeType(node.type) === normalized) ?? null
}
