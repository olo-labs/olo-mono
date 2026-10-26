/* Copyright (c) 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
import type { WorkflowDocument } from '../types/workflow'
import { availableAgentRefs, toggleAvailableAgent, workflowVariables } from './workflowResources'
import { METADATA_KEY, type PlannerContextSelection } from './plannerContextTypes'

function stringArray(value: unknown): string[] {
  return Array.isArray(value) ? value.filter((item): item is string => typeof item === 'string') : []
}

export function readPlannerContext(doc: WorkflowDocument): PlannerContextSelection {
  const raw = doc.metadata?.[METADATA_KEY]
  const record = raw && typeof raw === 'object' && !Array.isArray(raw) ? raw as Record<string, unknown> : {}
  const declared = new Set(workflowVariables(doc).map(variable => variable.name))
  return {
    selectedVariables: Array.isArray(record.selectedVariables) ? stringArray(record.selectedVariables)
      : stringArray(doc.capability?.required_context).filter(name => declared.has(name)),
    selectedAgents: Array.isArray(record.selectedAgents) ? stringArray(record.selectedAgents)
      : availableAgentRefs(doc).map(agent => agent.id),
    injectAgents: record.injectAgents !== false,
  }
}

export function applyPlannerContext(doc: WorkflowDocument, selection: PlannerContextSelection): WorkflowDocument {
  let next: WorkflowDocument = {
    ...doc,
    metadata: { ...doc.metadata, [METADATA_KEY]: {
      selectedVariables: [...selection.selectedVariables],
      selectedAgents: [...selection.selectedAgents],
      injectAgents: selection.injectAgents,
    } },
    capability: { ...doc.capability, required_context: [...selection.selectedVariables] },
  }
  const desired = new Set(selection.selectedAgents)
  for (const ref of availableAgentRefs(doc)) {
    if (!desired.has(ref.id)) next = toggleAvailableAgent(next, ref.id, false)
  }
  for (const id of desired) {
    if (!availableAgentRefs(next).some(agent => agent.id === id)) next = toggleAvailableAgent(next, id, true)
  }
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
