/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { Node } from '@xyflow/react'
import type { CatalogPort, StudioCatalog } from '../types/catalog'
import type { WorkflowDocument } from '../types/workflow'
import { findCatalogNode } from './catalogLookup'
import type { CatalogFlowNodeData } from './workflowGraph'
import type { WorkflowPort } from '../types/workflow'

function catalogPortToWire(port: CatalogPort): WorkflowPort {
  return {
    id: port.id,
    label: port.label ?? port.name,
    name: port.name,
    schema: port.schema,
    type: port.type,
    acceptType: port.acceptType,
    direction: port.direction,
    required: port.required,
    ui: port.ui,
  }
}

export function resolveNodePort(
  node: Node<CatalogFlowNodeData>,
  handleId: string | null | undefined,
  direction: 'INPUT' | 'OUTPUT',
  catalog: StudioCatalog | null,
  workflow?: WorkflowDocument | null,
): WorkflowPort | null {
  const portId = handleId?.trim()
  if (!portId) return null

  const workflowPorts = node.data.workflowPorts ?? []
  const fromWorkflow = workflowPorts.find((port) => port.id === portId && port.direction === direction)
  if (fromWorkflow) return fromWorkflow

  const descriptor = findCatalogNode(catalog, node.data.workflowType, workflow)
  const catalogPorts = direction === 'INPUT' ? descriptor?.inputs ?? [] : descriptor?.outputs ?? []
  const fromCatalog = catalogPorts.find((port) => port.id === portId)
  return fromCatalog ? catalogPortToWire(fromCatalog) : null
}
