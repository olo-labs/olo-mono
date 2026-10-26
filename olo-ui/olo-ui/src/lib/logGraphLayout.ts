/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { WorkflowDocument, WorkflowEdge, WorkflowNode } from '../types/workflow'
import { normalizeNodeType } from './boundaryNodes'
import { resolveLayoutGrid } from './workflowDesigner'

export interface CanvasPoint {
  x: number
  y: number
}

/** Layout merged / injected workflow graphs for read-only Log canvas. */
export function layoutLogWorkflowNodes(workflow: WorkflowDocument): Map<string, CanvasPoint> {
  const nodes = workflow.nodes ?? []
  const edges = workflow.edges ?? []
  const grid = resolveLayoutGrid(workflow)
  const positions = new Map<string, CanvasPoint>()

  const mainPath = mainMessagePath(nodes, edges)
  const mainPathSet = new Set(mainPath)

  mainPath.forEach((nodeId, index) => {
    positions.set(nodeId, {
      x: grid.originX + index * grid.columnGap,
      y: grid.originY,
    })
  })

  let pluginColumn = 0
  for (const node of nodes) {
    if (mainPathSet.has(node.id) || positions.has(node.id)) {
      continue
    }
    if (isCapabilityPluginNode(node, edges)) {
      positions.set(node.id, {
        x: grid.originX + pluginColumn * grid.columnGap,
        y: grid.originY + grid.rowGap,
      })
      pluginColumn += 1
      continue
    }
    positions.set(node.id, {
      x: grid.originX + (mainPath.length + positions.size) * grid.columnGap,
      y: grid.originY,
    })
  }

  return positions
}

export function mainMessagePath(nodes: WorkflowNode[], edges: WorkflowEdge[]): string[] {
  const startId =
    nodes.find((node) => normalizeNodeType(node.type) === 'START')?.id
    ?? nodes[0]?.id
  if (!startId) return []

  const nextByMessageEdge = new Map<string, string>()
  for (const edge of edges) {
    if (isMessageFlowEdge(edge) && edge.sourceNodeId && edge.targetNodeId) {
      nextByMessageEdge.set(edge.sourceNodeId, edge.targetNodeId)
    }
  }

  const path: string[] = []
  const visited = new Set<string>()
  let current: string | undefined = startId
  while (current && !visited.has(current)) {
    visited.add(current)
    path.push(current)
    current = nextByMessageEdge.get(current)
  }
  return path
}

function isMessageFlowEdge(edge: WorkflowEdge): boolean {
  const sourcePort = edge.sourcePortId ?? 'out'
  const targetPort = edge.targetPortId ?? 'in'
  return sourcePort === 'out' && targetPort === 'in'
}

function isCapabilityPluginNode(node: WorkflowNode, edges: WorkflowEdge[]): boolean {
  if (normalizeNodeType(node.type) !== 'TOOL') {
    return false
  }
  const hasCapabilityOut = edges.some(
    (edge) => edge.sourceNodeId === node.id && edge.sourcePortId === 'capabilities',
  )
  const hasMessageIn = edges.some(
    (edge) => edge.targetNodeId === node.id && (edge.targetPortId ?? 'in') === 'in',
  )
  return hasCapabilityOut && !hasMessageIn
}
