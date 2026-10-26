/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { CatalogComponentBase, CatalogNode, CatalogParameter, StudioCatalog } from '../types/catalog'

import type { WorkflowDocument, WorkflowNode } from '../types/workflow'

import { normalizeNodeType } from './boundaryNodes'

import { findWorkflowNodeTemplate } from './workflowDesigner'

import { workflowNodeToCatalogNode } from './workflowNodeTemplates'



export function findWorkflowPreset(catalog: StudioCatalog | null, presetId: string) {

  return catalog?.workflowPresets?.find((preset) => preset.id === presetId) ?? null

}



export function findCatalogNode(

  catalog: StudioCatalog | null,

  nodeType: string,

  workflow?: WorkflowDocument | null,

): CatalogNode | null {

  const normalized = normalizeNodeType(nodeType)



  if (workflow) {

    const template = findWorkflowNodeTemplate(workflow, normalized)

    if (template) return workflowNodeToCatalogNode(workflow, template)

  }



  if (!catalog?.nodes?.length) return null

  const upper = normalized.toUpperCase()

  return (

    catalog.nodes.find((node) => node.id === nodeType || node.id === normalized)

    ?? catalog.nodes.find((node) => node.id.endsWith(`:${upper}`))

    ?? catalog.nodes.find((node) => node.id.toUpperCase().endsWith(`:${upper}`))

    ?? null

  )

}



export function findCatalogComponent(

  catalog: StudioCatalog | null,

  nodeType: string,

  workflowNode?: WorkflowNode | null,

): CatalogComponentBase | null {

  const configuration = workflowNode?.configuration ?? {}

  const configuredId =

    (typeof configuration.toolId === 'string' && configuration.toolId)

    || (typeof configuration.hookId === 'string' && configuration.hookId)

    || (typeof configuration.catalogId === 'string' && configuration.catalogId)

    || null

  if (configuredId && catalog) {

    const direct =

      catalog.tools?.find((tool) => tool.id === configuredId)

      ?? catalog.hooks?.find((hook) => hook.id === configuredId)

      ?? catalog.nodes?.find((node) => node.id === configuredId)

    if (direct) return direct

  }



  const nodeDescriptor = findCatalogNode(catalog, nodeType, workflowNode ? { nodes: [workflowNode] } as WorkflowDocument : null)

  if (nodeDescriptor) return nodeDescriptor

  return null
}



/** Catalog preset parameters when workflow id matches a workflowPresets entry. */

export function presetParametersForWorkflow(

  catalog: StudioCatalog | null,

  workflow: WorkflowDocument,

): CatalogParameter[] {

  const preset = findWorkflowPreset(catalog, workflow.id)

  return preset?.parameters ?? []

}

