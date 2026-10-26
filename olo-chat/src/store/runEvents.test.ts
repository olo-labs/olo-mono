/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { afterEach, describe, expect, it } from 'vitest'
import { getActiveRunStorageKey, isActiveRunForSession } from './runEvents'

describe('isActiveRunForSession', () => {
  afterEach(() => {
    sessionStorage.clear()
  })

  it('returns true when session storage tracks the active run', () => {
    sessionStorage.setItem(getActiveRunStorageKey('session-a'), 'run-1')
    expect(isActiveRunForSession('session-a', 'run-1')).toBe(true)
  })

  it('returns false for a different session or run', () => {
    sessionStorage.setItem(getActiveRunStorageKey('session-a'), 'run-1')
    expect(isActiveRunForSession('session-b', 'run-1')).toBe(false)
    expect(isActiveRunForSession('session-a', 'run-2')).toBe(false)
  })
})
