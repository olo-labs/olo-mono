/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { CatalogComponentBase } from '../types/catalog'
import type { WorkflowDocument } from '../types/workflow'
import {
  availableAgentRefs,
  catalogResourceId,
  toggleAvailableAgent,
  toggleCatalogTool,
  workflowTools,
  workflowVariables,
} from './workflowResources'
import {
  CAPABILITY_BUNDLES,
  METADATA_KEY,
  type PlannerContextSelection,
} from './plannerContextTypes'

function stringArray(value: unknown): string[] {
  if (!Array.isArray(value)) return []
  return value.filter((v): v is string => typeof v === 'string')
}

function defaultSelection(): PlannerContextSelection {
  return {
    selectedVariables: [],
    selectedTools: ['olo-core:http-tool', 'olo-core:web-search'],
    selectedAgents: [],
    injectCapabilities: true,
    injectAgents: true,
  }
}

function inferSelectedVariables(
  doc: WorkflowDocument,
  metadataRecord: Record<string, unknown>,
): string[] {
  const fromMetadata = stringArray(metadataRecord.selectedVariables ?? metadataRecord.variables)
  if (fromMetadata.length > 0) return fromMetadata

  const declared = new Set(workflowVariables(doc).map((variable) => variable.name))
  const fromCapability = doc.capability?.required_context
  if (!Array.isArray(fromCapability)) return []
  return fromCapability.filter((name): name is string => typeof name === 'string' && declared.has(name))
}

function catalogIdsForLegacyBundles(bundleIds: string[]): string[] {
  const ids = new Set<string>()
  for (const bundleId of bundleIds) {
    const bundle = CAPABILITY_BUNDLES.find((b) => b.id === bundleId)
    if (!bundle) continue
    for (const item of bundle.includes) {
      if (item.catalogId) ids.add(item.catalogId)
    }
  }
  return [...ids]
}

function inferSelectedTools(doc: WorkflowDocument): string[] {
  return workflowTools(doc)
    .map((t) => t.runtimeBinding?.implementationId ?? `olo-core:${t.id}`)
    .filter((id): id is string => typeof id === 'string' && id.length > 0)
}

function readPlannerContextRecord(doc: WorkflowDocument): Record<string, unknown> {
  const raw = doc.metadata?.[METADATA_KEY]
  if (raw && typeof raw === 'object' && !Array.isArray(raw)) {
    return raw as Record<string, unknown>
  }
  return {}
}

export function readPlannerContext(doc: WorkflowDocument): PlannerContextSelection {
  const metadataRecord = readPlannerContextRecord(doc)

  let selectedTools = stringArray(metadataRecord.selectedTools)
  if (selectedTools.length === 0 && metadataRecord.capabilities) {
    selectedTools = catalogIdsForLegacyBundles(stringArray(metadataRecord.capabilities))
  }
  if (selectedTools.length === 0) {
    selectedTools = inferSelectedTools(doc)
  }

  let selectedAgents = stringArray(metadataRecord.selectedAgents ?? metadataRecord.agents)
  if (selectedAgents.length === 0) {
    selectedAgents = availableAgentRefs(doc).map((agent) => agent.id)
  }

  const selectedVariables = inferSelectedVariables(doc, metadataRecord)

  if (Object.keys(metadataRecord).length > 0) {
    return {
      selectedVariables,
      selectedTools,
      selectedAgents,
      injectCapabilities: metadataRecord.injectCapabilities !== false,
      injectAgents: metadataRecord.injectAgents !== false,
    }
  }

  const fallback = defaultSelection()
  fallback.selectedVariables = selectedVariables
  fallback.selectedAgents = selectedAgents
  fallback.selectedTools = selectedTools
  return fallback
}

function writePlannerContext(
  doc: WorkflowDocument,
  selection: PlannerContextSelection,
): WorkflowDocument {
  const existingMetadata = readPlannerContextRecord(doc)

  return {
    ...doc,
    metadata: {
      ...doc.metadata,
      [METADATA_KEY]: {
        ...existingMetadata,
        selectedVariables: selection.selectedVariables,
        selectedTools: selection.selectedTools,
        selectedAgents: selection.selectedAgents,
        injectCapabilities: selection.injectCapabilities,
        injectAgents: selection.injectAgents,
      },
    },
  }
}

function syncCapabilityRequiredContext(
  doc: WorkflowDocument,
  selectedVariables: string[],
): WorkflowDocument {
  return {
    ...doc,
    capability: {
      ...doc.capability,
      required_context: [...selectedVariables],
    },
  }
}

function syncToolsFromSelection(
  doc: WorkflowDocument,
  selectedToolIds: string[],
  catalogTools: CatalogComponentBase[],
): WorkflowDocument {
  const targetIds = new Set(selectedToolIds)
  let next = doc
  for (const tool of catalogTools) {
    const shouldEnable = targetIds.has(tool.id)
    const isEnabled = workflowTools(next).some(
      (entry) =>
        entry.runtimeBinding?.implementationId === tool.id ||
        catalogResourceId(tool.id) === entry.id,
    )
    if (shouldEnable !== isEnabled) {
      next = toggleCatalogTool(next, tool, shouldEnable)
    }
  }
  return next
}

function syncAgents(doc: WorkflowDocument, agentIds: string[]): WorkflowDocument {
  const desired = new Set(agentIds)
  let next = doc
  for (const ref of availableAgentRefs(doc)) {
    if (!desired.has(ref.id)) {
      next = toggleAvailableAgent(next, ref.id, false)
    }
  }
  for (const id of desired) {
    if (!availableAgentRefs(next).some((agent) => agent.id === id)) {
      next = toggleAvailableAgent(next, id, true)
    }
  }
  return next
}

export function applyPlannerContext(
  doc: WorkflowDocument,
  selection: PlannerContextSelection,
  catalogTools: CatalogComponentBase[],
): WorkflowDocument {
  let next = writePlannerContext(doc, selection)
  next = syncCapabilityRequiredContext(next, selection.selectedVariables)
  next = syncToolsFromSelection(next, selection.selectedTools, catalogTools)
  next = syncAgents(next, selection.selectedAgents)
  return next
}

export function resolveAgentPromptInfos(
  doc: WorkflowDocument,
  workflows: { id?: string; label?: string; description?: string }[],
): import('./plannerContextTypes').AgentPromptInfo[] {
  const byId = new Map(
    workflows
      .filter((workflow) => typeof workflow.id === 'string' && workflow.id.length > 0)
      .map((workflow) => [workflow.id!, workflow]),
  )
  return availableAgentRefs(doc).map((ref) => {
    const summary = byId.get(ref.id)
    return {
      id: ref.id,
      label: summary?.label?.trim() || ref.id,
      description: summary?.description?.trim(),
    }
  })
}
