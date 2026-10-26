/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { Node } from '@xyflow/react'
import type { CatalogFlowNodeData } from './workflowGraph'

export function preserveLayoutPositions(
  allowNodeDrag: boolean,
  previous: Node<CatalogFlowNodeData>[],
  nextNodes: Node<CatalogFlowNodeData>[],
): Node<CatalogFlowNodeData>[] {
  if (!allowNodeDrag || previous.length === 0 || previous.length !== nextNodes.length) {
    return nextNodes
  }
  const previousIds = new Set(previous.map((node) => node.id))
  if (!nextNodes.every((node) => previousIds.has(node.id))) {
    return nextNodes
  }
  const positionById = new Map(previous.map((node) => [node.id, node.position]))
  return nextNodes.map((node) => ({
    ...node,
    position: positionById.get(node.id) ?? node.position,
  }))
}
