/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { portDisplayColor } from './portConnection'
import { resolveNodePort } from './portResolve'
import type { CatalogNode, StudioCatalog } from '../types/catalog'
import type { WorkflowDocument } from '../types/workflow'
import type { CatalogFlowNodeData } from './workflowGraph'
import type { Edge, Node } from '@xyflow/react'
import { MarkerType } from '@xyflow/react'
import { resolveCanvasTheme, resolvePortColors } from './workflowDesigner'

export interface CatalogFlowEdgeData {
  tooltip?: string
  sourcePortColor?: string
}

export function defaultEdgeStroke(workflow?: WorkflowDocument | null): string {
  return resolveCanvasTheme(workflow).edgeStroke
}

export const DEFAULT_EDGE_STROKE = '#52525b'

const FLOW_ARROW_MARKER_SIZE = { width: 20, height: 20 } as const

export function flowEdgeMarkerEnd(color: string) {
  return {
    type: MarkerType.ArrowClosed,
    ...FLOW_ARROW_MARKER_SIZE,
    color,
  }
}

export function applyCatalogFlowEdgePresentation(
  edge: Edge,
  nodes: Node<CatalogFlowNodeData>[],
  catalog: StudioCatalog | null,
  workflow?: WorkflowDocument | null,
  portColors?: Record<string, string>,
): Edge {
  const colors = portColors ?? resolvePortColors(workflow)
  const data = buildCatalogFlowEdgeData(edge, nodes, catalog, workflow, colors)
  const color = data.sourcePortColor ?? defaultEdgeStroke(workflow)
  return {
    ...edge,
    data: data as Record<string, unknown>,
    markerEnd: flowEdgeMarkerEnd(color),
  }
}

export function nodeTooltipLines(
  nodeId: string,
  data: CatalogFlowNodeData,
  descriptor: CatalogNode | null,
): string[] {
  const typeLabel = data.presentation?.typeLabel ?? data.workflowType
  const lines = [
    data.label || nodeId,
    `${typeLabel} · ${nodeId}`,
  ]
  const description = data.presentation?.description ?? descriptor?.description
  if (description) lines.push(description)
  return lines
}

export function edgeTooltipText(
  edge: Pick<Edge, 'source' | 'target' | 'sourceHandle' | 'targetHandle'>,
  nodes: Node<CatalogFlowNodeData>[],
): string {
  const source = nodes.find((n) => n.id === edge.source)
  const target = nodes.find((n) => n.id === edge.target)
  const sourceLabel = (source?.data as CatalogFlowNodeData | undefined)?.label ?? edge.source
  const targetLabel = (target?.data as CatalogFlowNodeData | undefined)?.label ?? edge.target
  const outPort = edge.sourceHandle ?? 'out'
  const inPort = edge.targetHandle ?? 'in'
  return `${sourceLabel} (${outPort}) → ${targetLabel} (${inPort})`
}

export function buildCatalogFlowEdgeData(
  edge: Pick<Edge, 'source' | 'target' | 'sourceHandle' | 'targetHandle'>,
  nodes: Node<CatalogFlowNodeData>[],
  catalog: StudioCatalog | null,
  workflow?: WorkflowDocument | null,
  portColors?: Record<string, string>,
): CatalogFlowEdgeData {
  const colors = portColors ?? resolvePortColors(workflow)
  const sourceNode = nodes.find((node) => node.id === edge.source)
  const outputPort = sourceNode
    ? resolveNodePort(sourceNode, edge.sourceHandle, 'OUTPUT', catalog, workflow)
    : null
  const sourcePortColor = outputPort ? portDisplayColor(outputPort, colors) : undefined
  return {
    tooltip: edgeTooltipText(edge, nodes),
    ...(sourcePortColor ? { sourcePortColor } : {}),
  }
}

export function resolveConnectionLineColor(
  nodeId: string | null | undefined,
  handleId: string | null | undefined,
  nodes: Node<CatalogFlowNodeData>[],
  catalog: StudioCatalog | null,
  workflow?: WorkflowDocument | null,
): string {
  if (!nodeId) return defaultEdgeStroke(workflow)
  const sourceNode = nodes.find((node) => node.id === nodeId)
  if (!sourceNode) return defaultEdgeStroke(workflow)
  const colors = resolvePortColors(workflow)
  const outputPort = resolveNodePort(sourceNode, handleId, 'OUTPUT', catalog, workflow)
  return outputPort ? portDisplayColor(outputPort, colors) ?? defaultEdgeStroke(workflow) : defaultEdgeStroke(workflow)
}
