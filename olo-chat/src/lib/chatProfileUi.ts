/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import type { ChatProfileDto } from '../api/chatApi'

const FALLBACK_EMOJIS = ['🧠', '⚡', '🔧', '💬', '🐞']

/**
 * Visual emoji for a profile. Uses {@link ChatProfileDto.emoji} when the API provides it;
 * otherwise infers from id/displayName (smart/deep → 🧠, fast/quick → ⚡, debug → 🐞) or falls back by index.
 */
export function emojiForProfile(p: ChatProfileDto, index: number): string {
  const fromApi = p.emoji?.trim()
  if (fromApi) return fromApi
  const hay = `${p.id} ${p.displayName}`.toLowerCase()
  if (/(smart|deep|reason|planner|think|heavy)/i.test(hay)) return '🧠'
  if (/(fast|quick|speed|minimal|lite)/i.test(hay)) return '⚡'
  if (/(debug|trace|verbose)/i.test(hay)) return '🐞'
  return FALLBACK_EMOJIS[index % FALLBACK_EMOJIS.length]
}

/** Single-line label for selects and chips: "🧠 Smart Assistant". */
export function formatProfileOptionLabel(p: ChatProfileDto, index: number): string {
  const e = emojiForProfile(p, index)
  const name = p.displayName?.trim() || p.id
  return `${e} ${name}`
}

/** Shorter chip label (same as option for now). */
export function formatProfileChipLabel(p: ChatProfileDto, index: number): string {
  return formatProfileOptionLabel(p, index)
}
