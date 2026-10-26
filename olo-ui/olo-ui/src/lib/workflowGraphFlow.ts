/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { StudioCatalog } from '../types/catalog'
import type { WorkflowDocument, WorkflowEdge, WorkflowNode } from '../types/workflow'
import { applyCatalogFlowEdgePresentation } from './canvasLabels'
import { findCatalogComponent } from './catalogLookup'
import { resolveNodePresentation } from './nodePresentation'
import { defaultNodePosition as designerDefaultNodePosition, resolvePortColors } from './workflowDesigner'
import { normalizeNodeType } from './boundaryNodes'
import { layoutLogWorkflowNodes } from './logGraphLayout'
import { applyWorkflowGraphSemantics } from './workflowGraphSemantics'
import {
  FLOW_EDGE_TYPE,
  FLOW_NODE_TYPE,
  type CatalogFlowNodeData,
  workflowEdgeEndpoints,
} from './workflowGraphTypes'
import { resolveNodeDisplayLabel } from './workflowGraphLabels'
import type { Node, Edge } from '@xyflow/react'

function readDesignerPosition(node: WorkflowNode): { x: number; y: number } | null {
  const designer = node.configuration?.designer as
    | { position?: { x?: number; y?: number }; x?: number; y?: number }
    | undefined
  if (!designer) return null
  if (designer.position && typeof designer.position.x === 'number' && typeof designer.position.y === 'number') {
    return { x: designer.position.x, y: designer.position.y }
  }
  if (typeof designer.x === 'number' && typeof designer.y === 'number') {
    return { x: designer.x, y: designer.y }
  }
  return null
}

function defaultNodePosition(workflow: WorkflowDocument, index: number): { x: number; y: number } {
  return designerDefaultNodePosition(workflow, index)
}

export function workflowToFlow(
  workflow: WorkflowDocument,
  catalog: StudioCatalog | null,
  options?: { readOnly?: boolean; allowNodeDrag?: boolean; autoLayout?: boolean },
): { nodes: Node<CatalogFlowNodeData>[]; edges: Edge[] } {
  const readOnly = Boolean(options?.readOnly)
  const allowNodeDrag = Boolean(options?.allowNodeDrag)
  const autoLayout = Boolean(options?.autoLayout)
  const workflowNodes = workflow.nodes ?? []
  const portColors = resolvePortColors(workflow)
  const layoutPositions = autoLayout ? layoutLogWorkflowNodes(workflow) : null
  const nodes: Node<CatalogFlowNodeData>[] = workflowNodes.map((node, index) => {
    const workflowType = normalizeNodeType(node.type)
    const descriptor = findCatalogComponent(catalog, workflowType, node)
    const presentation = resolveNodePresentation(workflow, node, catalog)
    const position =
      layoutPositions?.get(node.id)
      ?? readDesignerPosition(node)
      ?? defaultNodePosition(workflow, index)
    const catalogId =
      (typeof node.configuration?.toolId === 'string' && node.configuration.toolId)
      || (typeof node.configuration?.hookId === 'string' && node.configuration.hookId)
      || descriptor?.id
    return {
      id: node.id,
      type: FLOW_NODE_TYPE,
      position,
      draggable: readOnly && !allowNodeDrag ? false : undefined,
      data: {
        label: resolveNodeDisplayLabel(node, catalog),
        emoji: presentation.emoji ?? descriptor?.emoji,
        workflowType,
        catalogId,
        workflowPorts: node.ports,
        presentation,
        readOnly,
      },
    }
  })

  const edges: Edge[] = []
  for (const [index, edge] of (workflow.edges ?? []).entries()) {
    const endpoints = workflowEdgeEndpoints(edge)
    if (!endpoints) continue
    const flowEdge: Edge = {
      id: `edge-${index}-${endpoints.source}-${endpoints.sourceHandle ?? 'out'}-${endpoints.target}`,
      type: FLOW_EDGE_TYPE,
      source: endpoints.source,
      target: endpoints.target,
      sourceHandle: endpoints.sourceHandle,
      targetHandle: endpoints.targetHandle,
    }
    edges.push(applyCatalogFlowEdgePresentation(flowEdge, nodes, catalog, workflow, portColors))
  }

  return { nodes, edges }
}

export function flowToWorkflow(
  flowNodes: Node<CatalogFlowNodeData>[],
  flowEdges: Edge[],
  workflow: WorkflowDocument,
  catalog: StudioCatalog | null = null,
): WorkflowDocument {
  const existingById = new Map((workflow.nodes ?? []).map((n) => [n.id, n]))

  const nodes: WorkflowNode[] = flowNodes.map((flowNode) => {
    const existing = existingById.get(flowNode.id)
    const configuration = {
      ...(existing?.configuration ?? {}),
      designer: {
        ...((existing?.configuration?.designer as object) ?? {}),
        position: { x: flowNode.position.x, y: flowNode.position.y },
      },
    }
    return {
      ...existing,
      id: flowNode.id,
      type: normalizeNodeType(flowNode.data.workflowType),
      label:
        typeof flowNode.data.label === 'string' && flowNode.data.label.trim()
          ? flowNode.data.label.trim()
          : existing?.label,
      execution: existing?.execution,
      configuration,
      ports: existing?.ports ?? [],
      reads: existing?.reads ?? [],
      writes: existing?.writes ?? [],
    }
  })

  const edges: WorkflowEdge[] = flowEdges.map((edge) => ({
    sourceNodeId: edge.source,
    targetNodeId: edge.target,
    ...(edge.sourceHandle ? { sourcePortId: edge.sourceHandle } : {}),
    ...(edge.targetHandle ? { targetPortId: edge.targetHandle } : {}),
  }))

  return applyWorkflowGraphSemantics(
    {
      ...workflow,
      nodes,
      edges,
    },
    catalog,
  )
}
