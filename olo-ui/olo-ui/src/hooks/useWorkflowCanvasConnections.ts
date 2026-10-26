/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { useCallback, useState } from 'react'
import type { Connection, Edge, Node, OnConnectStartParams } from '@xyflow/react'
import {
  applyCatalogFlowEdgePresentation,
  defaultEdgeStroke,
  resolveConnectionLineColor,
} from '../lib/canvasLabels'
import { arePortsCompatible } from '../lib/portConnection'
import { isPlannerRoutedMessagePort } from '../lib/workflowNodePorts'
import { resolveNodePort } from '../lib/portResolve'
import { FLOW_EDGE_TYPE, type CatalogFlowNodeData } from '../lib/workflowGraph'
import type { WorkflowDocument } from '../types/workflow'
import type { StudioCatalog } from '../types/catalog'

export function useWorkflowCanvasConnections({
  draft,
  catalog,
  nodes,
  edges: _edges,
  setEdges,
  readOnly,
  persistGraph,
}: {
  draft: WorkflowDocument | null
  catalog: StudioCatalog | null
  nodes: Node<CatalogFlowNodeData>[]
  edges: Edge[]
  setEdges: React.Dispatch<React.SetStateAction<Edge[]>>
  readOnly: boolean
  persistGraph: (nextNodes: Node<CatalogFlowNodeData>[], nextEdges: Edge[]) => void
}) {
  const [connectionLineColor, setConnectionLineColor] = useState(() => defaultEdgeStroke(draft))

  const onConnect = useCallback(
    (connection: Connection) => {
      if (readOnly || !connection.source || !connection.target) return
      setEdges((eds) => {
        const exists = eds.some(
          (e) =>
            e.source === connection.source
            && e.target === connection.target
            && e.sourceHandle === connection.sourceHandle
            && e.targetHandle === connection.targetHandle,
        )
        if (exists) return eds
        const newEdge = applyCatalogFlowEdgePresentation(
          {
            id: `edge-${connection.source}-${connection.sourceHandle ?? 'out'}-${connection.target}-${connection.targetHandle ?? 'in'}`,
            type: FLOW_EDGE_TYPE,
            source: connection.source,
            target: connection.target,
            sourceHandle: connection.sourceHandle,
            targetHandle: connection.targetHandle,
          },
          nodes,
          catalog,
          draft,
        )
        const next = [...eds, newEdge]
        persistGraph(nodes, next)
        return next
      })
    },
    [catalog, draft, nodes, persistGraph, readOnly, setEdges],
  )

  const onConnectStart = useCallback(
    (_event: MouseEvent | TouchEvent, params: OnConnectStartParams) => {
      if (readOnly || params.handleType !== 'source') {
        setConnectionLineColor(defaultEdgeStroke(draft))
        return
      }
      setConnectionLineColor(
        resolveConnectionLineColor(params.nodeId, params.handleId, nodes, catalog, draft),
      )
    },
    [catalog, draft, nodes, readOnly],
  )

  const onConnectEnd = useCallback(() => {
    setConnectionLineColor(defaultEdgeStroke(draft))
  }, [draft])

  const isValidConnection = useCallback(
    (connection: Connection | Edge) => {
      if (readOnly || !connection.source || !connection.target) return false
      const sourceNode = nodes.find((node) => node.id === connection.source)
      const targetNode = nodes.find((node) => node.id === connection.target)
      if (!sourceNode || !targetNode) return false

      const sourceWorkflowNode = draft?.nodes?.find((node) => node.id === sourceNode.id)
      const targetWorkflowNode = draft?.nodes?.find((node) => node.id === targetNode.id)
      if (
        isPlannerRoutedMessagePort(
          sourceNode.data.workflowType,
          connection.sourceHandle,
          sourceWorkflowNode?.configuration,
        )
        || isPlannerRoutedMessagePort(
          targetNode.data.workflowType,
          connection.targetHandle,
          targetWorkflowNode?.configuration,
        )
      ) {
        return false
      }

      const outputPort = resolveNodePort(sourceNode, connection.sourceHandle, 'OUTPUT', catalog, draft)
      const inputPort = resolveNodePort(targetNode, connection.targetHandle, 'INPUT', catalog, draft)
      if (!outputPort || !inputPort) return true
      return arePortsCompatible(outputPort, inputPort)
    },
    [catalog, draft, nodes, readOnly],
  )

  return {
    connectionLineColor,
    onConnect,
    onConnectStart,
    onConnectEnd,
    isValidConnection,
  }
}
