/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
/** Normalize emoji for JSON storage (NFC, no stray variation-selector fragments). */
export function normalizeWorkflowEmoji(emoji: string | undefined | null): string | undefined {
  if (emoji == null) return undefined
  const trimmed = emoji.trim()
  if (!trimmed) return undefined
  return trimmed.normalize('NFC')
}

export function normalizeWorkflowDocumentEmoji<T extends { emoji?: string }>(doc: T): T {
  if (doc.emoji == null) return doc
  const normalized = normalizeWorkflowEmoji(doc.emoji)
  if (normalized === doc.emoji) return doc
  return { ...doc, emoji: normalized }
}
