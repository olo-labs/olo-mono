/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { MarkerType } from '@xyflow/react'
import { CatalogFlowEdge } from './CatalogFlowEdge'
import { CatalogFlowNode } from './CatalogFlowNode'
import { FLOW_EDGE_TYPE, FLOW_NODE_TYPE } from '../../lib/workflowGraph'

export const workflowCanvasNodeTypes = { [FLOW_NODE_TYPE]: CatalogFlowNode }
export const workflowCanvasEdgeTypes = { [FLOW_EDGE_TYPE]: CatalogFlowEdge }
export const defaultFlowEdgeOptions = {
  type: FLOW_EDGE_TYPE,
  markerEnd: {
    type: MarkerType.ArrowClosed,
    width: 20,
    height: 20,
  },
} as const

export type WorkflowCanvasMode = 'builder' | 'log'

export interface WorkflowCanvasProps {
  readOnly?: boolean
  mode?: WorkflowCanvasMode
}
