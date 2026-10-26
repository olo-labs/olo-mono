/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { CatalogComponentBase, CatalogNode } from '../types/catalog'
import type { WorkflowDocument, WorkflowNode } from '../types/workflow'
import { normalizeNodeType, isBoundaryNodeType } from './boundaryNodes'
import { findWorkflowNodeTemplate, readNodeTypeDesigner } from './workflowDesigner'
import { uniqueNodeId } from './workflowNodeId'

function catalogIdForNodeType(nodeType: string): string {
  return `workflow-template:${normalizeNodeType(nodeType)}`
}

export function workflowNodeToCatalogNode(
  workflow: WorkflowDocument,
  node: WorkflowNode,
): CatalogNode {
  const nodeType = normalizeNodeType(node.type)
  const typeDesigner = readNodeTypeDesigner(workflow, nodeType)
  const palette = typeDesigner.palette ?? {}
  return {
    id: catalogIdForNodeType(nodeType),
    kind: 'NODE',
    name: palette.name?.trim() || node.label?.trim() || nodeType,
    description: typeDesigner.description,
    emoji: typeDesigner.emoji ?? workflow.emoji,
    category: palette.category ?? 'control',
    inputs: (node.ports ?? []).filter((port) => port.direction === 'INPUT').map((port) => ({
      id: port.id,
      name: port.name ?? port.id,
      label: port.label,
      schema: port.schema,
      type: port.type,
      acceptType: port.acceptType,
      shortDescription: port.shortDescription,
      required: port.required,
      minConnections: port.minConnections,
      maxConnections: port.maxConnections,
      ui: port.ui,
    })),
    outputs: (node.ports ?? []).filter((port) => port.direction === 'OUTPUT').map((port) => ({
      id: port.id,
      name: port.name ?? port.id,
      label: port.label,
      schema: port.schema,
      type: port.type,
      acceptType: port.acceptType,
      shortDescription: port.shortDescription,
      required: port.required,
      minConnections: port.minConnections,
      maxConnections: port.maxConnections,
      ui: port.ui,
    })),
  }
}

export function workflowPaletteNodes(workflow: WorkflowDocument | null | undefined): CatalogComponentBase[] {
  if (!workflow?.nodes?.length) return []
  const seen = new Set<string>()
  const items: CatalogComponentBase[] = []
  for (const node of workflow.nodes) {
    const nodeType = normalizeNodeType(node.type)
    if (!isBoundaryNodeType(nodeType) || seen.has(nodeType)) continue
    const typeDesigner = readNodeTypeDesigner(workflow, nodeType)
    if (typeDesigner.palette?.enabled === false) continue
    seen.add(nodeType)
    items.push(workflowNodeToCatalogNode(workflow, node))
  }
  return items
}

export function isWorkflowTemplateCatalogId(catalogId: string): boolean {
  return catalogId.startsWith('workflow-template:')
}

export function workflowTypeFromTemplateCatalogId(catalogId: string): string {
  return catalogId.slice('workflow-template:'.length)
}

export function cloneWorkflowNodeTemplate(
  workflow: WorkflowDocument,
  nodeType: string,
  position: { x: number; y: number },
  existingIds: Iterable<string>,
): WorkflowNode {
  const template = findWorkflowNodeTemplate(workflow, nodeType)
  if (!template) {
    throw new Error(`No workflow node template for type ${nodeType}`)
  }
  const normalized = normalizeNodeType(nodeType)
  const baseId = normalized.toLowerCase()
  const id = uniqueNodeId(baseId, existingIds)
  return {
    ...template,
    id,
    type: normalized,
    label: template.label,
    ports: template.ports?.map((port) => ({ ...port, ui: port.ui ? { ...port.ui } : undefined })),
    reads: [...(template.reads ?? [])],
    writes: [...(template.writes ?? [])],
    configuration: {
      ...(template.configuration ?? {}),
      designer: {
        ...((template.configuration?.designer as object) ?? {}),
        position,
      },
    },
  }
}
