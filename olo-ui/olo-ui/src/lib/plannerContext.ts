/* Copyright (c) 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
import type { WorkflowDocument } from '../types/workflow'
import type { PlannerContextSelection } from './plannerContextTypes'
export { PLANNER_MACRO_AGENTS, PLANNER_MACROS, type PlannerContextSelection,
  type PlannerPromptValidationIssue, type AgentPromptInfo } from './plannerContextTypes'
export { extractPromptPlaceholders, validatePromptTemplate, estimateTokenCount, formatAgentLines,
  generatePlannerPrompt, plannerAutocompleteOptions } from './plannerPrompt'
export { readPlannerContext, applyPlannerContext, resolveAgentPromptInfos } from './plannerContextSync'
import { applyPlannerContext, readPlannerContext } from './plannerContextSync'

export function updatePlannerContext(doc: WorkflowDocument, patch: Partial<PlannerContextSelection>): WorkflowDocument {
  return applyPlannerContext(doc, { ...readPlannerContext(doc), ...patch })
}

export function togglePlannerAgent(doc: WorkflowDocument, agentId: string, enabled: boolean): WorkflowDocument {
  const current = readPlannerContext(doc)
  const selectedAgents = enabled ? [...new Set([...current.selectedAgents, agentId])]
    : current.selectedAgents.filter(id => id !== agentId)
  return applyPlannerContext(doc, { ...current, selectedAgents })
}

export function isPlannerAgentEnabled(doc: WorkflowDocument, agentId: string): boolean {
  return readPlannerContext(doc).selectedAgents.includes(agentId)
}
export function isPlannerVariableEnabled(doc: WorkflowDocument, name: string): boolean {
  return readPlannerContext(doc).selectedVariables.includes(name)
}
export function togglePlannerVariable(doc: WorkflowDocument, name: string, enabled: boolean): WorkflowDocument {
  const current = readPlannerContext(doc)
  const selectedVariables = enabled ? [...new Set([...current.selectedVariables, name])]
    : current.selectedVariables.filter(value => value !== name)
  return applyPlannerContext(doc, { ...current, selectedVariables })
}
export function plannerContextSummary(_doc: WorkflowDocument, selection: PlannerContextSelection): string {
  return `Agents (${selection.selectedAgents.length})`
}
