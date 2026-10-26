/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { CatalogComponentBase, CatalogNode, StudioCatalog } from '../types/catalog'
import type { WorkflowDocument, WorkflowNode, WorkflowPort } from '../types/workflow'
import {
  createEndNode,
  createStartNode,
  normalizeNodeType,
} from './boundaryNodes'
import { findCatalogNode } from './catalogLookup'
import type { CatalogDragPayload } from './canvasDrag'
import {
  catalogComponentToPorts,
  resolveAgentHostPorts,
  resolveCapabilityPluginPorts,
} from './workflowNodePorts'
import {
  agentPresetExecution,
  hookNodeConfiguration,
  resolveDelegateAgentPorts,
  toolNodeConfiguration,
} from './workflowGraphSemantics'
import {
  cloneWorkflowNodeTemplate,
  isWorkflowTemplateCatalogId,
  workflowTypeFromTemplateCatalogId,
} from './workflowNodeTemplates'
import { catalogIdToWorkflowType } from './workflowGraphTypes'
import { uniqueNodeId } from './workflowNodeId'

function catalogPortsToWorkflowPorts(catalogNode: CatalogNode): WorkflowNode['ports'] {
  const ports: NonNullable<WorkflowNode['ports']> = []
  for (const input of catalogNode.inputs ?? []) {
    ports.push({
      id: input.id,
      label: input.label ?? input.name ?? input.id,
      name: input.name ?? input.id,
      shortDescription: input.shortDescription,
      schema: input.schema ?? 'any',
      type: input.type ?? input.schema ?? 'any',
      acceptType: input.acceptType ?? input.type ?? input.schema ?? 'any',
      direction: 'INPUT',
      required: input.required,
      minConnections: input.minConnections,
      maxConnections: input.maxConnections,
      ui: { position: input.ui?.position ?? 'LEFT', color: input.ui?.color },
    })
  }
  for (const output of catalogNode.outputs ?? []) {
    ports.push({
      id: output.id,
      label: output.label ?? output.name ?? output.id,
      name: output.name ?? output.id,
      shortDescription: output.shortDescription,
      schema: output.schema ?? 'any',
      type: output.type ?? output.schema ?? 'any',
      acceptType: output.acceptType,
      direction: 'OUTPUT',
      required: output.required,
      minConnections: output.minConnections,
      maxConnections: output.maxConnections,
      ui: { position: output.ui?.position ?? 'RIGHT', color: output.ui?.color },
    })
  }
  return ports
}

export function createWorkflowNodeFromCatalog(
  catalogItem: CatalogComponentBase,
  position: { x: number; y: number },
  existingIds: Iterable<string>,
  catalog: StudioCatalog | null,
  workflow: WorkflowDocument,
): WorkflowNode {
  if (isWorkflowTemplateCatalogId(catalogItem.id)) {
    const workflowType = workflowTypeFromTemplateCatalogId(catalogItem.id)
    return cloneWorkflowNodeTemplate(workflow, workflowType, position, existingIds)
  }

  const workflowType = catalogIdToWorkflowType(catalogItem.id)
  const normalized = normalizeNodeType(workflowType)
  if (normalized === 'START') {
    return createStartNode(workflow, position, existingIds)
  }
  if (normalized === 'END') {
    return createEndNode(workflow, position, existingIds)
  }
  const id = uniqueNodeId(catalogItem.name ?? workflowType, existingIds)
  const catalogNode =
    findCatalogNode(catalog, workflowType)
    ?? findCatalogNode(catalog, catalogItem.id)
    ?? (catalogItem.kind === 'NODE' ? (catalogItem as CatalogNode) : null)
  const catalogPorts = catalogNode
    ? (catalogPortsToWorkflowPorts(catalogNode) ?? [])
    : catalogComponentToPorts(catalogItem)
  const ports: WorkflowPort[] = resolveAgentHostPorts(normalized, [], catalogPorts)

  return {
    id,
    type: normalizeNodeType(workflowType),
    label: catalogItem.name?.trim() || undefined,
    ports,
    reads: [],
    writes: [],
    configuration: {
      designer: { position },
    },
  }
}

export function createWorkflowNodeFromDrag(
  payload: CatalogDragPayload,
  catalogItem: CatalogComponentBase,
  position: { x: number; y: number },
  existingIds: Iterable<string>,
  catalog: StudioCatalog | null,
  workflow: WorkflowDocument,
): WorkflowNode {
  if (payload.kind === 'TOOL') {
    const id = uniqueNodeId(catalogItem.name ?? 'tool', existingIds)
    return {
      id,
      type: 'TOOL',
      label: catalogItem.name?.trim() || undefined,
      ports: resolveCapabilityPluginPorts('TOOL', [], catalogComponentToPorts(catalogItem)),
      reads: [],
      writes: [],
      configuration: toolNodeConfiguration(catalogItem, position),
    }
  }

  if (payload.kind === 'HOOK') {
    const id = uniqueNodeId(catalogItem.name ?? 'hook', existingIds)
    return {
      id,
      type: 'HOOK',
      label: catalogItem.name?.trim() || undefined,
      ports: resolveCapabilityPluginPorts('HOOK', [], catalogComponentToPorts(catalogItem)),
      reads: [],
      writes: [],
      configuration: hookNodeConfiguration(catalogItem, position),
    }
  }

  if (payload.kind === 'AGENT') {
    const id = uniqueNodeId(payload.name ?? payload.catalogId, existingIds)
    return {
      id,
      type: 'AGENT',
      label: payload.name?.trim() || undefined,
      ports: resolveDelegateAgentPorts(catalog, payload.catalogId),
      execution: agentPresetExecution(payload.catalogId),
      reads: [],
      writes: [],
      configuration: {
        delegateAgentId: payload.catalogId,
        designer: { position },
      },
    }
  }

  return createWorkflowNodeFromCatalog(
    catalogItem,
    position,
    existingIds,
    catalog,
    workflow,
  )
}
