/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { CatalogComponentBase, StudioCatalog } from '../types/catalog'
import type { WorkflowDocument, WorkflowNode } from '../types/workflow'
import { togglePlannerAgent, togglePlannerTool } from './plannerContext'
import {
  catalogComponentToPorts,
  findWorkflowPresetPorts,
  resolveNodePorts,
} from './workflowNodePorts'
import { toggleCatalogHook, toggleCatalogTool } from './workflowResources'

function findCatalogItem(
  catalog: StudioCatalog | null,
  catalogId: string,
): CatalogComponentBase | null {
  if (!catalog) return null
  return (
    catalog.tools?.find((tool) => tool.id === catalogId)
    ?? catalog.hooks?.find((hook) => hook.id === catalogId)
    ?? catalog.nodes?.find((node) => node.id === catalogId)
    ?? null
  )
}

function toolIdFromNode(node: WorkflowNode): string | null {
  const configuration = node.configuration ?? {}
  const toolId = configuration.toolId ?? configuration.toolRef
  return typeof toolId === 'string' && toolId.length > 0 ? toolId : null
}

function hookIdFromNode(node: WorkflowNode): string | null {
  const configuration = node.configuration ?? {}
  const hookId = configuration.hookId ?? configuration.hookRef
  return typeof hookId === 'string' && hookId.length > 0 ? hookId : null
}

function workflowIdFromNode(node: WorkflowNode): string | null {
  const execution = node.execution
  const workflowId = execution?.workflowRef?.workflowId
  return typeof workflowId === 'string' && workflowId.length > 0 ? workflowId : null
}

/** Sync planner mode, registry entries, and metadata from graph wiring. */
export function applyWorkflowGraphSemantics(
  workflow: WorkflowDocument,
  catalog: StudioCatalog | null,
): WorkflowDocument {
  const nodes = workflow.nodes ?? []
  const catalogTools = catalog?.tools ?? []
  let next = workflow

  next = {
    ...next,
    nodes: (next.nodes ?? []).map((node) => {
      const nodeType = node.type?.toUpperCase() ?? ''
      let catalogPorts: ReturnType<typeof catalogComponentToPorts> = []
      if (nodeType === 'TOOL') {
        const toolId = toolIdFromNode(node)
        const item = toolId ? findCatalogItem(catalog, toolId) : null
        catalogPorts = item ? catalogComponentToPorts(item) : []
      } else if (nodeType === 'HOOK') {
        const hookId = hookIdFromNode(node)
        const item = hookId ? findCatalogItem(catalog, hookId) : null
        catalogPorts = item ? catalogComponentToPorts(item) : []
      } else if (nodeType === 'AGENT') {
        const delegateAgentId =
          typeof node.configuration?.delegateAgentId === 'string'
            ? node.configuration.delegateAgentId
            : null
        catalogPorts = delegateAgentId ? findWorkflowPresetPorts(catalog, delegateAgentId) : []
      }
      const ports = resolveNodePorts(nodeType, node.ports, catalogPorts, node.configuration)
      let updated = ports === node.ports ? node : { ...node, ports }
      if (nodeType === 'AGENT') {
        const configuration = { ...(updated.configuration ?? {}) }
        delete configuration.dynamicGraphPlanner
        delete configuration.plannerMode
        updated = { ...updated, configuration }
      }
      return updated
    }),
  }

  for (const node of nodes) {
    const nodeType = node.type?.toUpperCase() ?? ''
    if (nodeType === 'TOOL') {
      const toolId = toolIdFromNode(node)
      if (!toolId) continue
      const item = findCatalogItem(catalog, toolId)
      if (item) {
        next = toggleCatalogTool(next, item, true)
        next = togglePlannerTool(next, item.id, true, catalogTools)
      }
      continue
    }
    if (nodeType === 'HOOK') {
      const hookId = hookIdFromNode(node)
      if (!hookId) continue
      const item = findCatalogItem(catalog, hookId)
      if (item) {
        next = toggleCatalogHook(next, item, true)
      }
      continue
    }
    if (nodeType === 'AGENT') {
      const workflowId = workflowIdFromNode(node)
      if (workflowId) {
        next = togglePlannerAgent(next, workflowId, true, catalogTools)
      }
    }
  }

  return next
}

export function hookNodeConfiguration(
  item: CatalogComponentBase,
  position: { x: number; y: number },
): Record<string, unknown> {
  return {
    hookId: item.id,
    pattern: '**',
    designer: { position },
  }
}

export function toolNodeConfiguration(
  item: CatalogComponentBase,
  position: { x: number; y: number },
): Record<string, unknown> {
  return {
    toolId: item.id,
    designer: { position },
  }
}

export function agentPresetExecution(agentId: string): WorkflowNode['execution'] {
  return {
    executionKind: 'SUBWORKFLOW',
    executionModel: 'CHILD_WORKFLOW',
    routers: [],
    workflowRef: {
      workflowId: agentId,
      version: '1.0.0',
      inputMapping: {},
      outputMapping: {},
    },
  }
}

export function resolveDelegateAgentPorts(
  catalog: StudioCatalog | null,
  agentId: string,
): WorkflowNode['ports'] {
  return findWorkflowPresetPorts(catalog, agentId)
}
