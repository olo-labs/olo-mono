/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { describe, expect, it } from 'vitest'
import { emojiForProfile } from './chatProfileUi'
import type { ChatProfileDto } from '../api/chatApi'

function p(overrides: Partial<ChatProfileDto> & Pick<ChatProfileDto, 'id'>): ChatProfileDto {
  return {
    displayName: 'X',
    queue: 'q',
    pipeline: 'p',
    ...overrides,
  }
}

describe('emojiForProfile', () => {
  it('uses API emoji when set', () => {
    expect(emojiForProfile(p({ id: 'a', emoji: '🎯' }), 0)).toBe('🎯')
  })

  it('infers from id or display name', () => {
    expect(emojiForProfile(p({ id: 'smart', displayName: 'Assistant' }), 0)).toBe('🧠')
    expect(emojiForProfile(p({ id: 'x', displayName: 'Fast Response' }), 0)).toBe('⚡')
    expect(emojiForProfile(p({ id: 'debug', displayName: 'Verbose' }), 0)).toBe('🐞')
  })
})
