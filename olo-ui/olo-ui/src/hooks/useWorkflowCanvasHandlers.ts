/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { useCallback } from 'react'
import { useReactFlow, type Edge, type Node } from '@xyflow/react'
import type { CatalogFlowNodeData } from '../lib/workflowGraph'
import type { WorkflowDocument } from '../types/workflow'
import type { StudioCatalog } from '../types/catalog'
import { workflowConfigurationStore } from '../store/workflowConfigurationStore'
import { handleWorkflowCanvasDrop } from '../lib/workflowCanvasDrop'
import { useWorkflowCanvasConnections } from './useWorkflowCanvasConnections'
import { useWorkflowCanvasDeletes } from './useWorkflowCanvasDeletes'
import { useWorkflowCanvasContextHandlers } from './useWorkflowCanvasContextHandlers'

export function useWorkflowCanvasHandlers({
  draft,
  catalog,
  nodes,
  edges,
  setNodes,
  setEdges,
  readOnly,
  persistGraph,
}: {
  draft: WorkflowDocument | null
  catalog: StudioCatalog | null
  nodes: Node<CatalogFlowNodeData>[]
  edges: Edge[]
  setNodes: React.Dispatch<React.SetStateAction<Node<CatalogFlowNodeData>[]>>
  setEdges: React.Dispatch<React.SetStateAction<Edge[]>>
  readOnly: boolean
  allowNodeDrag: boolean
  persistGraph: (nextNodes: Node<CatalogFlowNodeData>[], nextEdges: Edge[]) => void
}) {
  const { screenToFlowPosition } = useReactFlow()
  const updateDraft = workflowConfigurationStore((s) => s.updateDraft)

  const connections = useWorkflowCanvasConnections({
    draft,
    catalog,
    nodes,
    edges,
    setEdges,
    readOnly,
    persistGraph,
  })

  const deletes = useWorkflowCanvasDeletes({
    nodes,
    edges,
    setNodes,
    setEdges,
    readOnly,
    persistGraph,
  })

  const context = useWorkflowCanvasContextHandlers()

  const onDragOver = useCallback((event: React.DragEvent) => {
    event.preventDefault()
    event.dataTransfer.dropEffect = 'copy'
  }, [])

  const onDrop = useCallback(
    (event: React.DragEvent) => {
      if (!draft) return
      handleWorkflowCanvasDrop(
        event,
        draft,
        catalog,
        edges,
        readOnly,
        screenToFlowPosition,
        setNodes,
        updateDraft,
      )
    },
    [catalog, draft, edges, readOnly, screenToFlowPosition, setNodes, updateDraft],
  )

  return {
    ...connections,
    ...deletes,
    ...context,
    onDragOver,
    onDrop,
  }
}
