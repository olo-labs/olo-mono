/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { StudioCatalog } from '../types/catalog'
import type {
  InlinePropertyConfig,
  NodeTypeDesignerConfig,
  WorkflowDocument,
  WorkflowNode,
} from '../types/workflow'
import { normalizeNodeType } from './boundaryNodes'
import { findCatalogNode } from './catalogLookup'
import {
  readNodeDesigner,
  readNodeTypeDesigner,
  resolveNodeSize,
} from './workflowDesigner'

export interface NodePresentation {
  emoji?: string
  typeLabel: string
  description?: string
  width: number
  height: number
  selectionBorder?: string
  inlineProperties: InlinePropertyConfig[]
}

function mergeInlineProperties(
  typeProperties: InlinePropertyConfig[] | undefined,
  nodeProperties: InlinePropertyConfig[] | undefined,
): InlinePropertyConfig[] {
  if (nodeProperties?.length) return nodeProperties
  return typeProperties ?? []
}

function resolveTypeDesignerForNode(
  workflow: WorkflowDocument | null | undefined,
  node: WorkflowNode,
): NodeTypeDesignerConfig {
  const workflowType = normalizeNodeType(node.type)
  return readNodeTypeDesigner(workflow, workflowType)
}

export function resolveNodePresentation(
  workflow: WorkflowDocument | null | undefined,
  node: WorkflowNode,
  catalog: StudioCatalog | null,
): NodePresentation {
  const workflowType = normalizeNodeType(node.type)
  const typeDesigner = resolveTypeDesignerForNode(workflow, node)
  const nodeDesigner = readNodeDesigner(node)
  const catalogNode = findCatalogNode(catalog, workflowType, workflow)
  const size = resolveNodeSize(workflow)
  const canvas = workflow?.designer?.canvas

  const typeLabel =
    nodeDesigner.typeLabel?.trim()
    || typeDesigner.typeLabel?.trim()
    || catalogNode?.name?.trim()
    || workflowType

  const emoji =
    nodeDesigner.emoji?.trim()
    || typeDesigner.emoji?.trim()
    || (workflowType === 'AGENT' ? workflow?.emoji?.trim() : undefined)
    || catalogNode?.emoji?.trim()

  const description =
    nodeDesigner.description?.trim()
    || typeDesigner.description?.trim()
    || catalogNode?.description?.trim()

  return {
    emoji,
    typeLabel,
    description,
    width: size.width,
    height: size.height,
    selectionBorder: canvas?.selectionBorder,
    inlineProperties: mergeInlineProperties(typeDesigner.inlineProperties, nodeDesigner.inlineProperties),
  }
}

export function resolveNodeTypeLabel(
  workflow: WorkflowDocument | null | undefined,
  node: Pick<WorkflowNode, 'type' | 'configuration'>,
  catalog: StudioCatalog | null,
): string {
  const workflowType = normalizeNodeType(node.type)
  const nodeDesigner = readNodeDesigner(node as WorkflowNode)
  const typeDesigner = resolveTypeDesignerForNode(workflow, node as WorkflowNode)
  const catalogNode = findCatalogNode(catalog, workflowType, workflow)
  return (
    nodeDesigner.typeLabel?.trim()
    || typeDesigner.typeLabel?.trim()
    || catalogNode?.name?.trim()
    || workflowType
  )
}
