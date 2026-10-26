/* Copyright (c) 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
export const SYSTEM_PROMPT_PARAMETER_ID = 'systemPrompt'
export const PROMPT_TOKEN_AGENTS = 'availableAgentsJson'
export const PROMPT_TOKEN_MESSAGE = 'message'
export interface PromptTokenInsertOption {
  value: string
  label: string
  group: string
  description?: string
}
export const RUNTIME_PROMPT_TOKENS: PromptTokenInsertOption[] = [
  { value: '{message}', label: 'message', group: 'Runtime', description: 'Current user message' },
  { value: '{availableAgentsJson}', label: 'availableAgentsJson', group: 'Runtime', description: 'Available specialist agents' },
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
