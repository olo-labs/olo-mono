/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { useCallback } from 'react'
import type { Edge, Node } from '@xyflow/react'
import type { CatalogFlowNodeData } from '../lib/workflowGraph'

export function useWorkflowCanvasDeletes({
  nodes,
  edges,
  setNodes,
  setEdges,
  readOnly,
  persistGraph,
}: {
  nodes: Node<CatalogFlowNodeData>[]
  edges: Edge[]
  setNodes: React.Dispatch<React.SetStateAction<Node<CatalogFlowNodeData>[]>>
  setEdges: React.Dispatch<React.SetStateAction<Edge[]>>
  readOnly: boolean
  persistGraph: (nextNodes: Node<CatalogFlowNodeData>[], nextEdges: Edge[]) => void
}) {
  const onNodesDelete = useCallback(
    (deleted: Node<CatalogFlowNodeData>[]) => {
      if (readOnly) return
      const deletedIds = new Set(deleted.map((n) => n.id))
      setNodes((nds) => {
        const remaining = nds.filter((n) => !deletedIds.has(n.id))
        setEdges((eds) => {
          const nextEdges = eds.filter((e) => !deletedIds.has(e.source) && !deletedIds.has(e.target))
          persistGraph(remaining, nextEdges)
          return nextEdges
        })
        return remaining
      })
    },
    [persistGraph, readOnly, setEdges, setNodes],
  )

  const onEdgesDelete = useCallback(
    (deleted: Edge[]) => {
      if (readOnly) return
      const deletedIds = new Set(deleted.map((e) => e.id))
      setEdges((eds) => {
        const next = eds.filter((e) => !deletedIds.has(e.id))
        persistGraph(nodes, next)
        return next
      })
    },
    [nodes, persistGraph, readOnly, setEdges],
  )

  const onNodeDragStop = useCallback(() => {
    if (readOnly) return
    persistGraph(nodes, edges)
  }, [edges, nodes, persistGraph, readOnly])

  const deleteNodeById = useCallback(
    (nodeId: string) => {
      const node = nodes.find((n) => n.id === nodeId)
      if (node) onNodesDelete([node])
    },
    [nodes, onNodesDelete],
  )

  const deleteEdgeById = useCallback(
    (edgeId: string) => {
      const edge = edges.find((e) => e.id === edgeId)
      if (edge) onEdgesDelete([edge])
    },
    [edges, onEdgesDelete],
  )

  return {
    onNodesDelete,
    onEdgesDelete,
    onNodeDragStop,
    deleteNodeById,
    deleteEdgeById,
  }
}
