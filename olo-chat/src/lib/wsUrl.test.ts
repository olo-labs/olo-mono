/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { afterEach, describe, expect, it, vi } from 'vitest'
import { getWebSocketUrl } from './wsUrl'

describe('getWebSocketUrl', () => {
  afterEach(() => {
    vi.unstubAllEnvs()
  })

  it('uses VITE_API_BASE when set', () => {
    vi.stubEnv('VITE_API_BASE', 'http://api.example.com:7080')
    expect(getWebSocketUrl()).toBe('ws://api.example.com:7080/ws')
  })

  it('uses same-origin ws when VITE_API_BASE is unset', () => {
    vi.stubEnv('VITE_API_BASE', '')
    expect(getWebSocketUrl()).toBe('ws://localhost:3000/ws')
  })
})
