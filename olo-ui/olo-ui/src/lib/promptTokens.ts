/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
export const SYSTEM_PROMPT_PARAMETER_ID = 'systemPrompt'

/** Canonical planner macro names (keep in sync with plannerContext). */
const PLANNER_MACRO_CAPABILITIES = 'CAPABILITIES'
const PLANNER_MACRO_AGENTS = 'AGENTS'

/** Friendly token inserted in UI; resolved at runtime (see planner macro expansion). */
export const PROMPT_TOKEN_TOOLS = 'tools'
export const PROMPT_TOKEN_AGENTS = 'agents'
export const PROMPT_TOKEN_MESSAGE = 'message'

export interface PromptTokenInsertOption {
  value: string
  label: string
  group: string
  description?: string
}

export const RUNTIME_PROMPT_TOKENS: PromptTokenInsertOption[] = [
  {
    value: `{${PROMPT_TOKEN_MESSAGE}}`,
    label: PROMPT_TOKEN_MESSAGE,
    group: 'Runtime',
    description: 'Caller message at run time',
  },
  {
    value: `{${PROMPT_TOKEN_TOOLS}}`,
    label: PROMPT_TOKEN_TOOLS,
    group: 'Runtime',
    description: 'Available tools (resolved from planner context)',
  },
  {
    value: `{${PROMPT_TOKEN_AGENTS}}`,
    label: PROMPT_TOKEN_AGENTS,
    group: 'Runtime',
    description: 'Delegatable agents — expanded when {agents} appears in the prompt',
  },
  {
    value: `{${PLANNER_MACRO_CAPABILITIES}}`,
    label: PLANNER_MACRO_CAPABILITIES,
    group: 'Runtime',
    description: 'Same as tools — capability/tool catalog block',
  },
  {
    value: `{${PLANNER_MACRO_AGENTS}}`,
    label: PLANNER_MACRO_AGENTS,
    group: 'Runtime',
    description: 'Same as agents — delegatable agent block',
  },
]

export function buildPromptInsertOptions(variableNames: string[]): PromptTokenInsertOption[] {
  const variables = variableNames.map((name) => ({
    value: `{${name}}`,
    label: name,
    group: 'Variables',
    description: `Workflow variable ${name}`,
  }))
  return [...variables, ...RUNTIME_PROMPT_TOKENS]
}

export function insertTextAtCursor(
  element: HTMLTextAreaElement | null,
  currentValue: string,
  text: string,
  onChange: (next: string) => void,
): void {
  if (!element) {
    onChange(`${currentValue}${text}`)
    return
  }
  const start = element.selectionStart ?? currentValue.length
  const end = element.selectionEnd ?? currentValue.length
  const next = `${currentValue.slice(0, start)}${text}${currentValue.slice(end)}`
  onChange(next)
  requestAnimationFrame(() => {
    element.focus()
    const position = start + text.length
    element.setSelectionRange(position, position)
  })
}

/** Normalize friendly aliases before macro expansion. */
export function normalizePromptMacroAliases(template: string): string {
  return template
    .replaceAll(`{${PROMPT_TOKEN_TOOLS}}`, `{${PLANNER_MACRO_CAPABILITIES}}`)
    .replaceAll(`{${PROMPT_TOKEN_AGENTS}}`, `{${PLANNER_MACRO_AGENTS}}`)
}
