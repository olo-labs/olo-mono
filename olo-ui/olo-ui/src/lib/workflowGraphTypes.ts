/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { WorkflowEdge, WorkflowPort } from '../types/workflow'
import type { NodePresentation } from './nodePresentation'

export const FLOW_NODE_TYPE = 'catalogNode'
export const FLOW_EDGE_TYPE = 'catalogEdge'

export interface CatalogFlowNodeData {
  label: string
  emoji?: string
  workflowType: string
  catalogId?: string
  workflowPorts?: WorkflowPort[]
  presentation?: NodePresentation
  readOnly?: boolean
  [key: string]: unknown
}

export function catalogIdToWorkflowType(catalogId: string): string {
  const colon = catalogId.lastIndexOf(':')
  if (colon >= 0) return catalogId.substring(colon + 1)
  return catalogId
}

export { slugifyNodeId, uniqueNodeId } from './workflowNodeId'

export function workflowEdgeEndpoints(edge: WorkflowEdge): {
  source: string
  target: string
  sourceHandle?: string
  targetHandle?: string
} | null {
  const source = edge.sourceNodeId ?? edge.source ?? edge.from
  const target = edge.targetNodeId ?? edge.target ?? edge.to
  if (!source || !target) return null
  return {
    source,
    target,
    sourceHandle: edge.sourcePortId,
    targetHandle: edge.targetPortId,
  }
}
