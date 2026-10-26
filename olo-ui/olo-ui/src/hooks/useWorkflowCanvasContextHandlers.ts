/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { useCallback, useState, type MutableRefObject } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import type { Edge, Node } from '@xyflow/react'
import { buildQuery, parseQuery, parsedToPanelParams } from '../routes'
import type { CanvasContextMenuState } from '../components/canvas/CanvasContextMenu'
import type { CatalogFlowNodeData } from '../lib/workflowGraph'
import { workflowConfigurationStore } from '../store/workflowConfigurationStore'

export function useWorkflowCanvasContextHandlers() {
  const navigate = useNavigate()
  const location = useLocation()
  const setSelectedCanvasNodeId = workflowConfigurationStore((s) => s.setSelectedCanvasNodeId)
  const [contextMenu, setContextMenu] = useState<CanvasContextMenuState | null>(null)

  const closeContextMenu = useCallback(() => setContextMenu(null), [])

  const openNodeProperties = useCallback(
    (nodeId: string) => {
      setSelectedCanvasNodeId(nodeId)
      const q = parseQuery(location.search)
      const params = parsedToPanelParams(q)
      navigate(`${location.pathname}?${buildQuery({ ...params, props: 1 })}`, { replace: true })
    },
    [location.pathname, location.search, navigate, setSelectedCanvasNodeId],
  )

  const onNodeContextMenu = useCallback(
    (event: React.MouseEvent, node: Node<CatalogFlowNodeData>) => {
      event.preventDefault()
      setContextMenu({ x: event.clientX, y: event.clientY, target: { kind: 'node', nodeId: node.id } })
    },
    [],
  )

  const onEdgeContextMenu = useCallback(
    (event: React.MouseEvent, edge: Edge) => {
      event.preventDefault()
      setContextMenu({ x: event.clientX, y: event.clientY, target: { kind: 'edge', edgeId: edge.id } })
    },
    [],
  )

  const onSelectionChange = useCallback(
    ({ nodes: selected }: { nodes: Node<CatalogFlowNodeData>[] }, syncingRef: MutableRefObject<boolean>) => {
      if (syncingRef.current) return
      const nextId = selected[0]?.id ?? null
      if (nextId === workflowConfigurationStore.getState().selectedCanvasNodeId) return
      setSelectedCanvasNodeId(nextId)
    },
    [setSelectedCanvasNodeId],
  )

  return {
    contextMenu,
    closeContextMenu,
    openNodeProperties,
    onNodeContextMenu,
    onEdgeContextMenu,
    onSelectionChange,
  }
}
