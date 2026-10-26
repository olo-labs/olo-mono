/* Copyright 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
import { describe, expect, it } from 'vitest'
import { buildPromptInsertOptions, insertTextAtCursor } from './promptTokens'
describe('promptTokens', () => {
  it('offers declared variables and canonical runtime tokens', () => {
    const options = buildPromptInsertOptions(['message', 'topic'])
    expect(options.filter(option => option.group === 'Variables').map(option => option.label)).toEqual(['message', 'topic'])
    expect(options.filter(option => option.group === 'Runtime').map(option => option.value)).toEqual(['{message}', '{availableAgentsJson}'])
  })
  it('appends a token when no editor is focused', () => {
    let value = ''
    insertTextAtCursor(null, 'Delegate to ', '{availableAgentsJson}', next => { value = next })
    expect(value).toBe('Delegate to {availableAgentsJson}')
  })
})
