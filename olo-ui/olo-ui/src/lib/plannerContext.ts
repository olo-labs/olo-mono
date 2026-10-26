/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { CatalogComponentBase } from '../types/catalog'
import type { WorkflowDocument } from '../types/workflow'
import type { PlannerContextSelection } from './plannerContextTypes'
export {
  PLANNER_MACRO_CAPABILITIES,
  PLANNER_MACRO_AGENTS,
  PLANNER_MACROS,
  CAPABILITY_BUNDLES,
  type PlannerContextSelection,
  type PlannerPromptValidationIssue,
  type AgentPromptInfo,
} from './plannerContextTypes'
export {
  extractPromptPlaceholders,
  validatePromptTemplate,
  estimateTokenCount,
  formatCapabilityLines,
  formatAgentLines,
  generatePlannerPrompt,
  plannerAutocompleteOptions,
} from './plannerPrompt'
export {
  readPlannerContext,
  applyPlannerContext,
  resolveAgentPromptInfos,
} from './plannerContextSync'

import { applyPlannerContext, readPlannerContext } from './plannerContextSync'

export function updatePlannerContext(
  doc: WorkflowDocument,
  patch: Partial<PlannerContextSelection>,
  catalogTools: CatalogComponentBase[],
): WorkflowDocument {
  const current = readPlannerContext(doc)
  return applyPlannerContext(doc, { ...current, ...patch }, catalogTools)
}

export function togglePlannerTool(
  doc: WorkflowDocument,
  toolId: string,
  enabled: boolean,
  catalogTools: CatalogComponentBase[],
): WorkflowDocument {
  const current = readPlannerContext(doc)
  const selectedTools = enabled
    ? current.selectedTools.includes(toolId)
      ? current.selectedTools
      : [...current.selectedTools, toolId]
    : current.selectedTools.filter((id) => id !== toolId)
  return applyPlannerContext(doc, { ...current, selectedTools }, catalogTools)
}

export function togglePlannerAgent(
  doc: WorkflowDocument,
  agentId: string,
  enabled: boolean,
  catalogTools: CatalogComponentBase[],
): WorkflowDocument {
  const current = readPlannerContext(doc)
  const selectedAgents = enabled
    ? current.selectedAgents.includes(agentId)
      ? current.selectedAgents
      : [...current.selectedAgents, agentId]
    : current.selectedAgents.filter((id) => id !== agentId)
  return applyPlannerContext(doc, { ...current, selectedAgents }, catalogTools)
}

export function isPlannerToolEnabled(doc: WorkflowDocument, toolId: string): boolean {
  return readPlannerContext(doc).selectedTools.includes(toolId)
}

export function isPlannerAgentEnabled(doc: WorkflowDocument, agentId: string): boolean {
  return readPlannerContext(doc).selectedAgents.includes(agentId)
}

export function isPlannerVariableEnabled(doc: WorkflowDocument, variableName: string): boolean {
  return readPlannerContext(doc).selectedVariables.includes(variableName)
}

export function togglePlannerVariable(
  doc: WorkflowDocument,
  variableName: string,
  enabled: boolean,
  catalogTools: CatalogComponentBase[],
): WorkflowDocument {
  const current = readPlannerContext(doc)
  const selectedVariables = enabled
    ? current.selectedVariables.includes(variableName)
      ? current.selectedVariables
      : [...current.selectedVariables, variableName]
    : current.selectedVariables.filter((name) => name !== variableName)
  return applyPlannerContext(doc, { ...current, selectedVariables }, catalogTools)
}

export function plannerContextSummary(
  _doc: WorkflowDocument,
  selection: PlannerContextSelection,
): string {
  return [
    `Tools (${selection.selectedTools.length})`,
    `Agents (${selection.selectedAgents.length})`,
  ].join(' · ')
}
