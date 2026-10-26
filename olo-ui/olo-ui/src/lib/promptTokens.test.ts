/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { describe, expect, it } from 'vitest'
import {
  buildPromptInsertOptions,
  normalizePromptMacroAliases,
  PROMPT_TOKEN_AGENTS,
  PROMPT_TOKEN_TOOLS,
} from './promptTokens'
describe('promptTokens', () => {
  it('builds variable and runtime insert options', () => {
    const options = buildPromptInsertOptions(['message', 'topic'])
    expect(options.filter((option) => option.group === 'Variables').map((option) => option.label)).toEqual([
      'message',
      'topic',
    ])
    expect(options.some((option) => option.value === '{tools}')).toBe(true)
    expect(options.some((option) => option.value === '{agents}')).toBe(true)
  })

  it('normalizes friendly macro aliases', () => {
    const template = `Use {${PROMPT_TOKEN_TOOLS}} and {${PROMPT_TOKEN_AGENTS}}`
    expect(normalizePromptMacroAliases(template)).toBe('Use {CAPABILITIES} and {AGENTS}')
  })
})
