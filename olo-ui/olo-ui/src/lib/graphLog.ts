/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { GraphLogSummary } from '../types/graphLog'

export function graphLogOptionLabel(entry: GraphLogSummary): string {
  if (entry.label?.trim()) return entry.label.trim()
  const parts = [entry.kind, entry.workflowId].filter(Boolean)
  if (parts.length > 0) return parts.join(' · ')
  return entry.fileName.replace(/\.json$/i, '')
}

export function graphLogShortTimestamp(timestamp: string | null): string {
  if (!timestamp?.trim()) return ''
  const parsed = Date.parse(timestamp)
  if (Number.isNaN(parsed)) return timestamp
  return new Date(parsed).toLocaleString()
}
