/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { StudioCatalog } from '../types/catalog'
import type { WorkflowDocument, WorkflowNode } from '../types/workflow'
import { findCatalogNode } from './catalogLookup'
import { normalizeNodeType } from './boundaryNodes'

export function resolveNodeDisplayLabel(
  node: Pick<WorkflowNode, 'id' | 'label' | 'type'>,
  catalog: StudioCatalog | null,
): string {
  const custom = node.label?.trim()
  if (custom) return custom
  const descriptor = findCatalogNode(catalog, normalizeNodeType(node.type))
  return descriptor?.name ?? node.id
}

export function applyNodeLabel(
  workflow: WorkflowDocument,
  nodeId: string,
  label: string,
): WorkflowDocument {
  const trimmed = label.trim()
  return {
    ...workflow,
    nodes: (workflow.nodes ?? []).map((node) =>
      node.id === nodeId
        ? { ...node, ...(trimmed ? { label: trimmed } : { label: undefined }) }
        : node,
    ),
  }
}
