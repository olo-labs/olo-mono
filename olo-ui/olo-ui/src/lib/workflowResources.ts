/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { CatalogComponentBase } from '../types/catalog'
import type {
  AgentReference,
  ChildWorkflowRef,
  WorkflowDocument,
  WorkflowHook,
  WorkflowTool,
  WorkflowVariable,
} from '../types/workflow'
import { normalizeWorkflowVariable } from './workflowVariables'

export function catalogResourceId(catalogId: string): string {
  const colon = catalogId.lastIndexOf(':')
  return colon >= 0 ? catalogId.substring(colon + 1) : catalogId
}

export function workflowVariables(doc: WorkflowDocument): WorkflowVariable[] {
  return Array.isArray(doc.variables) ? doc.variables.map(normalizeWorkflowVariable) : []
}

export function upsertVariable(
  doc: WorkflowDocument,
  variable: WorkflowVariable,
  previousName?: string,
): WorkflowDocument {
  const list = workflowVariables(doc).filter((v) => v.name !== (previousName ?? variable.name))
  return { ...doc, variables: [...list, normalizeWorkflowVariable(variable)] }
}

export function removeVariable(doc: WorkflowDocument, name: string): WorkflowDocument {
  return {
    ...doc,
    variables: workflowVariables(doc).filter((v) => v.name !== name),
  }
}

export function workflowTools(doc: WorkflowDocument): WorkflowTool[] {
  return Array.isArray(doc.tools) ? [...doc.tools] : []
}

export function workflowHooks(doc: WorkflowDocument): WorkflowHook[] {
  return Array.isArray(doc.hooks) ? [...doc.hooks] : []
}

export function childWorkflowRefs(doc: WorkflowDocument): ChildWorkflowRef[] {
  return Array.isArray(doc.childWorkflows) ? [...doc.childWorkflows] : []
}

export function availableAgentRefs(doc: WorkflowDocument): AgentReference[] {
  return Array.isArray(doc.availableAgents) ? [...doc.availableAgents] : []
}

export function toolFromCatalog(item: CatalogComponentBase): WorkflowTool {
  const id = catalogResourceId(item.id)
  return {
    id,
    type: 'TOOL',
    capability: {
      name: item.name ?? id,
      description: item.description,
      tags: [],
      examples: item.examples ?? [],
      required_inputs: [],
      required_outputs: [],
      tool_requirements: [],
      required_context: [],
    },
    runtimeBinding: {
      implementationId: item.id,
    },
    configuration: {},
  }
}

export function hookFromCatalog(item: CatalogComponentBase, pattern = '**'): WorkflowHook {
  const id = catalogResourceId(item.id)
  return {
    id,
    pattern,
    pre: { implementationId: item.id },
  }
}

export function isCatalogToolEnabled(doc: WorkflowDocument, catalogId: string): boolean {
  const id = catalogResourceId(catalogId)
  return workflowTools(doc).some(
    (t) => t.id === id || t.runtimeBinding?.implementationId === catalogId,
  )
}

export function isCatalogHookEnabled(doc: WorkflowDocument, catalogId: string): boolean {
  const id = catalogResourceId(catalogId)
  return workflowHooks(doc).some(
    (h) => h.id === id || h.pre?.implementationId === catalogId,
  )
}

export function toggleCatalogTool(
  doc: WorkflowDocument,
  item: CatalogComponentBase,
  enabled: boolean,
): WorkflowDocument {
  const id = catalogResourceId(item.id)
  const existing = workflowTools(doc)
  if (enabled) {
    if (isCatalogToolEnabled(doc, item.id)) return doc
    return { ...doc, tools: [...existing, toolFromCatalog(item)] }
  }
  return {
    ...doc,
    tools: existing.filter(
      (t) => t.id !== id && t.runtimeBinding?.implementationId !== item.id,
    ),
  }
}

export function toggleCatalogHook(
  doc: WorkflowDocument,
  item: CatalogComponentBase,
  enabled: boolean,
): WorkflowDocument {
  const id = catalogResourceId(item.id)
  const existing = workflowHooks(doc)
  if (enabled) {
    if (isCatalogHookEnabled(doc, item.id)) return doc
    return { ...doc, hooks: [...existing, hookFromCatalog(item)] }
  }
  return {
    ...doc,
    hooks: existing.filter(
      (h) => h.id !== id && h.pre?.implementationId !== item.id,
    ),
  }
}

export function isChildWorkflowEnabled(doc: WorkflowDocument, workflowId: string): boolean {
  return childWorkflowRefs(doc).some((c) => c.workflowId === workflowId)
}

export function toggleChildWorkflow(
  doc: WorkflowDocument,
  workflowId: string,
  workflowVersion: string,
  enabled: boolean,
): WorkflowDocument {
  const existing = childWorkflowRefs(doc)
  if (enabled) {
    if (isChildWorkflowEnabled(doc, workflowId)) return doc
    return {
      ...doc,
      childWorkflows: [...existing, { workflowId, workflowVersion }],
    }
  }
  return {
    ...doc,
    childWorkflows: existing.filter((c) => c.workflowId !== workflowId),
  }
}

export function isAvailableAgentEnabled(doc: WorkflowDocument, agentId: string): boolean {
  return availableAgentRefs(doc).some((a) => a.id === agentId)
}

export function toggleAvailableAgent(
  doc: WorkflowDocument,
  agentId: string,
  enabled: boolean,
): WorkflowDocument {
  const existing = availableAgentRefs(doc)
  if (enabled) {
    if (isAvailableAgentEnabled(doc, agentId)) return doc
    return { ...doc, availableAgents: [...existing, { id: agentId }] }
  }
  return {
    ...doc,
    availableAgents: existing.filter((a) => a.id !== agentId),
  }
}

export function uniqueVariableName(existing: WorkflowVariable[]): string {
  const taken = new Set(existing.map((v) => v.name))
  let base = 'variable'
  let n = 1
  while (taken.has(n === 1 ? base : `${base}${n}`)) n += 1
  return n === 1 ? base : `${base}${n}`
}
