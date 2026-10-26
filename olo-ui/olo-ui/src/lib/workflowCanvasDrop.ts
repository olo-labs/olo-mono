/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { Edge, Node } from '@xyflow/react'
import type { CatalogComponentBase, StudioCatalog } from '../types/catalog'
import type { WorkflowDocument } from '../types/workflow'
import { readCatalogDrag } from './canvasDrag'
import { resolveNodePresentation } from './nodePresentation'
import {
  FLOW_NODE_TYPE,
  catalogIdToWorkflowType,
  createWorkflowNodeFromDrag,
  flowToWorkflow,
  resolveNodeDisplayLabel,
  type CatalogFlowNodeData,
} from './workflowGraph'
import {
  isWorkflowTemplateCatalogId,
  workflowPaletteNodes,
  workflowTypeFromTemplateCatalogId,
} from './workflowNodeTemplates'

export function appendCatalogFlowNode(
  workflowNode: WorkflowDocument['nodes'] extends (infer N)[] | undefined ? N : never,
  position: { x: number; y: number },
  catalogItem: CatalogComponentBase,
  payloadEmoji: string | undefined,
  workflowType: string,
  draft: WorkflowDocument,
  catalog: StudioCatalog | null,
  edges: Edge[],
  readOnly: boolean,
  setNodes: React.Dispatch<React.SetStateAction<Node<CatalogFlowNodeData>[]>>,
  updateDraft: (document: WorkflowDocument) => void,
) {
  const presentation = resolveNodePresentation(draft, workflowNode, catalog)
  const flowNode: Node<CatalogFlowNodeData> = {
    id: workflowNode.id,
    type: FLOW_NODE_TYPE,
    position,
    data: {
      label: resolveNodeDisplayLabel(workflowNode, catalog),
      emoji: presentation.emoji ?? catalogItem.emoji ?? payloadEmoji,
      workflowType,
      catalogId: catalogItem.id,
      workflowPorts: workflowNode.ports,
      presentation,
      readOnly,
    },
  }
  setNodes((nds) => {
    const next = [...nds, flowNode]
    const updatedWorkflow = flowToWorkflow(next, edges, {
      ...draft,
      nodes: [...(draft.nodes ?? []), workflowNode],
    }, catalog)
    updateDraft(updatedWorkflow)
    return next
  })
}

export function handleWorkflowCanvasDrop(
  event: React.DragEvent,
  draft: WorkflowDocument,
  catalog: StudioCatalog | null,
  edges: Edge[],
  readOnly: boolean,
  screenToFlowPosition: (pos: { x: number; y: number }) => { x: number; y: number },
  setNodes: React.Dispatch<React.SetStateAction<Node<CatalogFlowNodeData>[]>>,
  updateDraft: (document: WorkflowDocument) => void,
): void {
  event.preventDefault()
  if (readOnly) return

  const payload = readCatalogDrag(event.dataTransfer)
  if (!payload) return

  const position = screenToFlowPosition({ x: event.clientX, y: event.clientY })
  const existingIds = (draft.nodes ?? []).map((n) => n.id)
  const paletteNodes = workflowPaletteNodes(draft)

  let catalogItem: CatalogComponentBase | undefined
  if (payload.kind === 'NODE') {
    catalogItem =
      catalog?.nodes?.find((n) => n.id === payload.catalogId)
      ?? paletteNodes.find((n) => n.id === payload.catalogId)
  } else if (payload.kind === 'TOOL') {
    catalogItem = catalog?.tools?.find((tool) => tool.id === payload.catalogId)
  } else if (payload.kind === 'HOOK') {
    catalogItem = catalog?.hooks?.find((hook) => hook.id === payload.catalogId)
  } else if (payload.kind === 'AGENT') {
    catalogItem = {
      id: payload.catalogId,
      kind: 'NODE',
      name: payload.name ?? payload.catalogId,
      emoji: payload.emoji ?? '🤖',
    }
  }

  if (!catalogItem) return

  const workflowNode = createWorkflowNodeFromDrag(
    payload,
    catalogItem,
    position,
    existingIds,
    catalog,
    draft,
  )

  if (payload.kind !== 'NODE') {
    appendCatalogFlowNode(
      workflowNode, position, catalogItem, payload.emoji, workflowNode.type,
      draft, catalog, edges, readOnly, setNodes, updateDraft,
    )
    return
  }

  const workflowType = isWorkflowTemplateCatalogId(catalogItem.id)
    ? workflowTypeFromTemplateCatalogId(catalogItem.id)
    : catalogIdToWorkflowType(catalogItem.id)
  appendCatalogFlowNode(
    workflowNode, position, catalogItem, payload.emoji, workflowType,
    draft, catalog, edges, readOnly, setNodes, updateDraft,
  )
}
