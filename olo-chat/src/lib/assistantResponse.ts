/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { RunEventDto } from '../api/chatApi'

/** Shown only after a completed/failed run when no workflow return text could be resolved. */
export const EMPTY_RESPONSE_MESSAGE = "Apologise, Couldn't generate the response for your query."

function isWorkflowResultEvent(event: RunEventDto): boolean {
  const output = event.output as Record<string, unknown> | undefined
  const metadata = event.metadata as Record<string, unknown> | undefined
  return output?.status === 'WORKFLOW_RESULT' || metadata?.phase === 'kernel-result'
}

function isMetadataOnlyOutput(o: Record<string, unknown>): boolean {
  const keys = Object.keys(o)
  if (keys.length === 0) return true
  const metadataKeys = new Set([
    'source',
    'status',
    'phase',
    'queue',
    'graphReady',
    'variables',
    'usedAdminFallback',
    'returnVariable',
  ])
  return keys.every((k) => metadataKeys.has(k))
}

function formatConversationSummary(summary: string): string | null {
  const trimmed = summary.trim()
  if (!trimmed) return null
  return trimmed
    .replace(/\s+assistant:\s*/gi, '\n\nAssistant: ')
    .replace(/^user:\s*/i, 'User: ')
    .replace(/\s+user:\s*/gi, '\n\nUser: ')
    .trim()
}

function extractJavaMapValue(text: string, key: string): string | null {
  const marker = `${key}=`
  const start = text.indexOf(marker)
  if (start < 0) return null
  const valueStart = start + marker.length
  const nextKey = text.slice(valueStart).search(/,\s*[A-Za-z][A-Za-z0-9_]*=/)
  const value = nextKey >= 0 ? text.slice(valueStart, valueStart + nextKey) : text.slice(valueStart)
  return value.replace(/}$/, '').trim() || null
}

function normalizeJavaMapText(text: string): string | null {
  const trimmed = text.trim()
  if (!trimmed.startsWith('{') || !trimmed.endsWith('}') || !trimmed.includes('=')) return null
  const summary = extractJavaMapValue(trimmed, 'summary')
  if (summary) return formatConversationSummary(summary)
  const response = extractJavaMapValue(trimmed, 'response') ?? extractJavaMapValue(trimmed, 'message')
  return response?.trim() || null
}

/** Extract workflow return / assistant text from node output. */
export function extractAssistantText(output: unknown): string | null {
  if (output == null) return null
  if (typeof output === 'string') return output.trim() || null
  if (typeof output !== 'object') return null
  const o = output as Record<string, unknown>

  for (const key of ['response', 'content', 'text', 'result', 'message'] as const) {
    const value = o[key]
    if (typeof value === 'string' && value.trim()) return value.trim()
  }

  const nestedMessage = o.message
  if (nestedMessage != null && typeof nestedMessage === 'object') {
    const mc = (nestedMessage as Record<string, unknown>).content
    if (typeof mc === 'string' && mc.trim()) return mc.trim()
  }

  const returnValue = o.returnValue
  if (returnValue != null) {
    const asText = String(returnValue).trim()
    const normalized = normalizeJavaMapText(asText)
    if (normalized) return normalized
    if (asText && asText !== 'null' && asText !== 'undefined') return asText
  }

  const choices = o.choices
  if (Array.isArray(choices) && choices.length > 0) {
    const first = choices[0] as Record<string, unknown> | undefined
    const msg = first?.message as Record<string, unknown> | undefined
    const c = msg?.content
    if (typeof c === 'string' && c.trim()) return c.trim()
  }

  return null
}

/** Resolve workflow return text from run events (kernel WORKFLOW_RESULT first). */
export function pickResponseFromEvents(events: RunEventDto[]): string | null {
  const withOutput = events.filter((e) => e.output && Object.keys(e.output).length > 0)
  const reversed = [...withOutput].reverse()

  for (const event of reversed) {
    if (!isWorkflowResultEvent(event)) continue
    const text = extractAssistantText(event.output)
    if (text) return text
  }

  for (const type of ['MODEL', 'AGENT'] as const) {
    for (const event of reversed) {
      if (event.nodeType?.toUpperCase() !== type) continue
      if (event.status?.toUpperCase() === 'FAILED') continue
      const text = extractAssistantText(event.output)
      if (text) return text
    }
  }

  for (const event of reversed) {
    if (event.nodeType?.toUpperCase() !== 'SYSTEM') continue
    if (event.status?.toUpperCase() === 'FAILED') continue
    const output = event.output as Record<string, unknown>
    if (isMetadataOnlyOutput(output)) continue
    const text = extractAssistantText(output)
    if (text) return text
  }

  return null
}

/** True when the run was cancelled by the user. */
export function isWorkflowCancelled(events: RunEventDto[]): boolean {
  return events.some((e) => {
    if (e.nodeType?.toUpperCase() !== 'SYSTEM' || e.status?.toUpperCase() !== 'FAILED') return false
    const output = e.output as Record<string, unknown> | undefined
    return output?.status === 'CANCELLED'
  })
}

/** True when the run has a final workflow result or terminal failure (not CONTEXT_READY alone). */
export function isWorkflowFinished(events: RunEventDto[]): boolean {
  if (isWorkflowCancelled(events)) return true
  if (
    events.some(
      (e) => e.nodeType?.toUpperCase() === 'SYSTEM' && e.status?.toUpperCase() === 'FAILED',
    )
  ) {
    return true
  }
  return events.some((e) => {
    if (e.nodeType?.toUpperCase() !== 'SYSTEM' || e.status?.toUpperCase() !== 'COMPLETED') {
      return false
    }
    const output = e.output as Record<string, unknown> | undefined
    const metadata = e.metadata as Record<string, unknown> | undefined
    return (
      output?.status === 'WORKFLOW_RESULT' ||
      metadata?.phase === 'kernel-result' ||
      output?.source === 'temporal'
    )
  })
}

/** Normalize display text; returns null when there is no user-facing message. */
export function normalizeResponseText(text: string | null | undefined): string | null {
  const trimmed = text?.trim()
  if (!trimmed) return null
  const normalizedMapText = normalizeJavaMapText(trimmed)
  if (normalizedMapText) return normalizedMapText
  if (trimmed.startsWith('{') && trimmed.endsWith('}')) {
    try {
      const o = JSON.parse(trimmed) as Record<string, unknown>
      if (isMetadataOnlyOutput(o)) return null
      const nested = extractAssistantText(o)
      if (nested) return nested
    } catch {
      // show raw text
    }
  }
  return trimmed
}

export function fallbackResponseMessage(runStatus: string | undefined): string {
  if (runStatus === 'cancelled') {
    return 'Run cancelled.'
  }
  if (runStatus === 'failed') {
    return "Apologise, the workflow failed before a response could be generated."
  }
  return EMPTY_RESPONSE_MESSAGE
}

/** True when GET /runs/{id} status means the workflow actually finished (not CONTEXT_READY). */
export function isRunTerminalFromApi(
  runStatus: string | undefined,
  events: RunEventDto[],
): boolean {
  if (!runStatus) return false
  if (runStatus === 'cancelled') return true
  if (runStatus === 'failed') return true
  if (runStatus === 'completed') return isWorkflowFinished(events)
  return false
}

export interface AssistantMessageContext {
  runId?: string
  activeRunId: string | null
  sending: boolean
  events: RunEventDto[]
}

/**
 * Persisted assistant bubble text. Returns null while the run is still in progress with no text yet
 * (hide placeholder). Fallback only when the run is finished and content is still empty.
 */
export function resolvePersistedAssistantContent(
  content: string | null | undefined,
  ctx: AssistantMessageContext,
): string | null {
  const normalized = normalizeResponseText(content)
  if (normalized) return normalized
  const messageRunId = ctx.runId?.trim()
  if (
    messageRunId &&
    messageRunId === ctx.activeRunId &&
    (ctx.sending || !isWorkflowFinished(ctx.events))
  ) {
    return null
  }
  return fallbackResponseMessage('completed')
}

/** @deprecated use resolvePersistedAssistantContent */
export function formatPersistedAssistantContent(content: string | null | undefined): string {
  return normalizeResponseText(content) ?? EMPTY_RESPONSE_MESSAGE
}
