/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { WorkflowDocument } from '../types/workflow'
import { workflowVariables } from './workflowResources'
import {
  PLANNER_MACRO_AGENTS,
  PLANNER_MACROS,
  PLACEHOLDER_PATTERN,
  type AgentPromptInfo,
  type PlannerContextSelection,
  type PlannerPromptValidationIssue,
} from './plannerContextTypes'

const INTERNAL_AGENTS_HEADING = '## Available agents'

function appendPromptSection(body: string, heading: string, block: string): string {
  const trimmed = body.trimEnd()
  if (!block.trim()) return trimmed
  return `${trimmed}\n\n${heading}\n\n${block}`.trim()
}

export function extractPromptPlaceholders(template: string): string[] {
  const names = new Set<string>()
  for (const match of template.matchAll(PLACEHOLDER_PATTERN)) {
    names.add(match[1])
  }
  return [...names]
}

export function validatePromptTemplate(
  template: string,
  doc: WorkflowDocument,
): PlannerPromptValidationIssue[] {
  const issues: PlannerPromptValidationIssue[] = []
  const variables = workflowVariables(doc)
  const variableNames = new Set(variables.map((variable) => variable.name))
  const placeholders = extractPromptPlaceholders(template)

  for (const name of placeholders) {
    if (PLANNER_MACROS.includes(name as (typeof PLANNER_MACROS)[number])) continue
    if (variableNames.has(name)) continue
    issues.push({
      code: 'dangling-placeholder',
      name,
      message: `Prompt references {${name}} but no workflow variable is defined.`,
    })
  }

  for (const variable of variables) {
    if (!variable.required) continue
    if (!placeholders.includes(variable.name)) {
      issues.push({
        code: 'missing-in-prompt',
        name: variable.name,
        message: `Required workflow variable "${variable.name}" is not used in the prompt.`,
      })
    }
  }

  return issues
}

export function estimateTokenCount(text: string): number {
  if (!text.trim()) return 0
  return Math.max(1, Math.ceil(text.length / 4))
}

export function formatAgentLines(agents: AgentPromptInfo[]): string[] {
  return agents.map((agent) => {
    const description = agent.description?.trim()
    return description ? `${agent.label}\n- ${description}` : agent.label
  })
}

export function generatePlannerPrompt(
  template: string,
  context: PlannerContextSelection,
  agents: AgentPromptInfo[],
): string {
  const selected = agents.filter(agent => context.selectedAgents.includes(agent.id))
  const block = context.injectAgents ? formatAgentLines(selected).join('\n\n') : ''
  if (template.includes(`{${PLANNER_MACRO_AGENTS}}`)) {
    return template.replaceAll(`{${PLANNER_MACRO_AGENTS}}`, block || '(none)').trim()
  }
  return context.injectAgents ? appendPromptSection(template, INTERNAL_AGENTS_HEADING, block) : template.trim()
}

export function plannerAutocompleteOptions(
  workflowVariableNames: string[],
): { value: string; label: string; group: string }[] {
  const options = workflowVariableNames.map((name) => ({
    value: `{${name}}`,
    label: name,
    group: 'Workflow variables',
  }))
  options.push(
    { value: `{${PLANNER_MACRO_AGENTS}}`, label: PLANNER_MACRO_AGENTS, group: 'Macros' },
  )
  return options
}
