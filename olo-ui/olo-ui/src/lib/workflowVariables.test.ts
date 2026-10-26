/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { describe, expect, it } from 'vitest'
import { normalizeVariableScope, normalizeWorkflowVariable } from './workflowVariables'

describe('workflowVariables', () => {
  it('normalizes known scopes', () => {
    expect(normalizeVariableScope('EXTERNAL')).toBe('EXTERNAL')
    expect(normalizeVariableScope('READONLY_EXTERNAL')).toBe('READONLY_EXTERNAL')
  })

  it('defaults invalid scope to LOCAL', () => {
    expect(normalizeVariableScope('PRIVATE')).toBe('LOCAL')
    expect(normalizeVariableScope(undefined)).toBe('LOCAL')
  })

  it('normalizes workflow variables on read', () => {
    expect(
      normalizeWorkflowVariable({
        name: 'message',
        type: 'string',
        scope: 'GLOBAL',
      }).scope,
    ).toBe('GLOBAL')
  })
})
