/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { useCallback, useEffect, useMemo, useRef } from 'react'
import {
  useNodesState,
  useEdgesState,
  useUpdateNodeInternals,
  type Node,
  type NodeChange,
} from '@xyflow/react'
import { workflowToFlow, type CatalogFlowNodeData } from '../lib/workflowGraph'
import {
  readWorkflowCanvasView,
  type WorkflowCanvasSize,
} from '../lib/workflowCanvasView'
import { preserveLayoutPositions } from '../lib/workflowCanvasLayout'
import type { WorkflowDocument } from '../types/workflow'
import type { StudioCatalog } from '../types/catalog'
import { useWorkflowCanvasViewPersistence } from './useWorkflowCanvasViewPersistence'
import type { WorkflowCanvasFlowOptions } from './useWorkflowCanvasGraphTypes'

export type { WorkflowCanvasFlowOptions } from './useWorkflowCanvasGraphTypes'
export { useWorkflowCanvasDraft } from './useWorkflowCanvasDraft'
export { usePersistGraph } from './usePersistGraph'

export function useWorkflowCanvasGraph(
  draft: WorkflowDocument | null,
  catalog: StudioCatalog | null,
  flowOptions: WorkflowCanvasFlowOptions,
  selectedFileName: string | null,
  readOnly: boolean,
) {
  const initial = useMemo(
    () => (draft ? workflowToFlow(draft, catalog, flowOptions) : { nodes: [], edges: [] }),
    // eslint-disable-next-line react-hooks/exhaustive-deps -- reset only when workflow file changes
    [draft?.id, selectedFileName, flowOptions],
  )

  const [nodes, setNodes, onNodesChange] = useNodesState<Node<CatalogFlowNodeData>>(initial.nodes)
  const [edges, setEdges, onEdgesChange] = useEdgesState(initial.edges)
  const updateNodeInternals = useUpdateNodeInternals()
  const syncingRef = useRef(false)
  const canvasHydratingRef = useRef(true)
  const canvasContainerRef = useRef<HTMLDivElement>(null)
  const canvasSizeRef = useRef<WorkflowCanvasSize | null>(null)

  const graphSyncKey = useMemo(() => {
    if (!draft) return ''
    return JSON.stringify({ id: draft.id, nodes: draft.nodes, edges: draft.edges })
  }, [draft])

  const savedCanvasView = useMemo(
    () => (draft ? readWorkflowCanvasView(draft) : null),
    [draft?.id, draft?.metadata],
  )

  const workflowCanvasKey = `${draft?.id ?? ''}:${selectedFileName ?? ''}`

  useEffect(() => {
    canvasHydratingRef.current = true
    canvasSizeRef.current = savedCanvasView?.size ?? null
  }, [savedCanvasView?.size, workflowCanvasKey])

  const finishCanvasHydration = useCallback(() => {
    window.requestAnimationFrame(() => {
      window.requestAnimationFrame(() => {
        canvasHydratingRef.current = false
      })
    })
  }, [])

  useEffect(() => {
    if (!draft) {
      setNodes([])
      setEdges([])
      return
    }
    syncingRef.current = true
    const next = workflowToFlow(draft, catalog, flowOptions)
    setNodes((previous) =>
      preserveLayoutPositions(flowOptions.allowNodeDrag, previous, next.nodes),
    )
    setEdges(next.edges)
    queueMicrotask(() => {
      syncingRef.current = false
      requestAnimationFrame(() => {
        next.nodes.forEach((node) => updateNodeInternals(node.id))
      })
    })
  }, [catalog, draft, flowOptions, graphSyncKey, setEdges, setNodes, updateNodeInternals])

  const { schedulePersistCanvasView } = useWorkflowCanvasViewPersistence(
    draft,
    readOnly,
    syncingRef,
    workflowCanvasKey,
    canvasHydratingRef,
    canvasContainerRef,
    canvasSizeRef,
  )

  const onNodesChangeLayoutOnly = useCallback(
    (changes: NodeChange<Node<CatalogFlowNodeData>>[]) => {
      const allowed = changes.filter((change) => change.type === 'position' || change.type === 'select')
      if (allowed.length > 0) {
        onNodesChange(allowed)
      }
    },
    [onNodesChange],
  )

  return {
    nodes,
    setNodes,
    edges,
    setEdges,
    onNodesChange,
    onEdgesChange,
    onNodesChangeLayoutOnly,
    syncingRef,
    canvasContainerRef,
    savedCanvasView,
    workflowCanvasKey,
    finishCanvasHydration,
    schedulePersistCanvasView,
  }
}
