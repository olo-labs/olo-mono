/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { describe, expect, it } from 'vitest'
import { normalizeWorkflowDocumentEmoji, normalizeWorkflowEmoji } from './workflowEmoji'

describe('workflowEmoji', () => {
  it('normalizes emoji to NFC', () => {
    const nfd = '🏗️'.normalize('NFD')
    expect(normalizeWorkflowEmoji(nfd)).toBe('🏗️')
  })

  it('returns undefined for blank emoji', () => {
    expect(normalizeWorkflowEmoji('   ')).toBeUndefined()
  })

  it('normalizes emoji on workflow documents before save', () => {
    const nfd = '🏗️'.normalize('NFD')
    const doc = normalizeWorkflowDocumentEmoji({ id: 'architect', emoji: nfd })
    expect(doc.emoji).toBe('🏗️')
  })
})
