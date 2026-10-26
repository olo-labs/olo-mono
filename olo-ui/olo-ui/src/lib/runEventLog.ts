/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { RunEventDto } from '../api/oloRuntime'
import { extractAssistantText } from './assistantResponse'

function formatTime(timestamp: number): string {
  if (!timestamp) return '--:--:--'
  try {
    return new Date(timestamp).toLocaleTimeString()
  } catch {
    return '--:--:--'
  }
}

function summarizeOutput(output: Record<string, unknown> | undefined): string {
  if (!output || Object.keys(output).length === 0) return ''
  const text = extractAssistantText(output)
  if (!text) return ''
  const oneLine = text.replace(/\s+/g, ' ')
  return oneLine.length > 120 ? `${oneLine.slice(0, 117)}…` : oneLine
}

export function formatRunEventLogLine(event: RunEventDto): string {
  const parts = [
    formatTime(event.timestamp),
    `[${event.nodeType || 'SYSTEM'}]`,
    event.nodeId || '—',
    event.status || '—',
  ]
  const detail = summarizeOutput(event.output)
  return detail ? `${parts.join(' ')} — ${detail}` : parts.join(' ')
}
