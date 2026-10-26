/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
/** Designer and inline-property types for workflow Studio. */

export interface NodeSize {
  width?: number
  height?: number
}

export type InlinePropertyWidget =
  | 'VARIABLE_CHECKLIST'
  | 'VARIABLE_SELECT'
  | 'WORKFLOW_PARAMETERS'
  | 'MODEL_SELECTOR'
  | 'PROVIDER_SELECTOR'
  | 'STRING'
  | 'NUMBER'

export interface InlinePropertyConfig {
  id: string
  widget: InlinePropertyWidget
  label?: string
  binding?: string
  defaultValue?: string
  group?: string
  order?: number
}

export interface NodeTypeDesignerConfig {
  emoji?: string
  typeLabel?: string
  description?: string
  palette?: {
    name?: string
    category?: string
    enabled?: boolean
  }
  inlineProperties?: InlinePropertyConfig[]
}

export interface WorkflowLayoutDesigner {
  originX?: number
  originY?: number
  columnGap?: number
  rowGap?: number
  columns?: number
}

export interface WorkflowCanvasDesigner {
  backgroundColor?: string
  gridGap?: number
  edgeStroke?: string
  selectionBorder?: string
  minimapNodeColor?: string
}

export interface WorkflowDesigner {
  paletteGroup?: string
  searchKeywords?: string[]
  nodeSize?: NodeSize
  resizable?: boolean
  draggable?: boolean
  layout?: WorkflowLayoutDesigner
  canvas?: WorkflowCanvasDesigner
  portColors?: Record<string, string>
  nodeTypes?: Record<string, NodeTypeDesignerConfig>
}

export interface NodeDesignerConfig {
  position?: { x?: number; y?: number }
  x?: number
  y?: number
  emoji?: string
  typeLabel?: string
  description?: string
  inlineProperties?: InlinePropertyConfig[]
}
