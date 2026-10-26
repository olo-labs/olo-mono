/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

export function summarizeValue(v: unknown, maxLen = 140): string | null {
  if (v == null) return null
  let text: string
  if (typeof v === 'string') text = v.trim()
  else if (typeof v === 'number' || typeof v === 'boolean') text = String(v)
  else text = JSON.stringify(v)
  if (!text) return null
  return text.length > maxLen ? `${text.slice(0, maxLen)}...` : text
}

export function summarizeMap(m?: Record<string, unknown>): string | null {
  if (!m || Object.keys(m).length === 0) return null
  const entries = Object.entries(m)
  if (entries.length === 0) return null
  const parts: string[] = []
  for (const [k, v] of entries.slice(0, 3)) {
    const sv = summarizeValue(v, 80)
    if (sv) parts.push(`${k}: ${sv}`)
  }
  return parts.length > 0 ? parts.join(' | ') : null
}
