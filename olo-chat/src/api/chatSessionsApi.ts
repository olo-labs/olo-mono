/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { CHAT_API, tenantIdForApiPath, withAuth } from './chatApiClient'
import type {
  ChatMessageDto,
  CreateSessionBody,
  CreateSessionResponse,
  SendMessageResponse,
  SessionSummaryDto,
} from './chatTypes'

/** List chat sessions for a tenant, optionally scoped by queue and pipeline (per workflow queue and pipeline view). */
export async function listSessions(
  tenantId: string,
  options?: { queue?: string; pipeline?: string }
): Promise<SessionSummaryDto[]> {
  const tid = tenantIdForApiPath(tenantId)
  if (!tid) return []
  try {
    const params = new URLSearchParams()
    if (options?.queue) params.set('queue', options.queue)
    if (options?.pipeline) params.set('pipeline', options.pipeline)
    const qs = params.toString()
    const url = `${CHAT_API}/tenants/${encodeURIComponent(tid)}/sessions` + (qs ? `?${qs}` : '')
    const res = await fetch(url, { headers: withAuth() })
    if (!res.ok) return []
    const data = await res.json()
    return Array.isArray(data) ? data : []
  } catch {
    return []
  }
}

/** Delete all chat sessions for the tenant, optionally scoped by queue and pipeline (current view). */
export async function deleteAllSessions(
  tenantId: string,
  options?: { queue?: string; pipeline?: string }
): Promise<void> {
  const tid = tenantIdForApiPath(tenantId)
  if (!tid) return
  const params = new URLSearchParams()
  if (options?.queue) params.set('queue', options.queue)
  if (options?.pipeline) params.set('pipeline', options.pipeline)
  const qs = params.toString()
  const url = `${CHAT_API}/tenants/${encodeURIComponent(tid)}/sessions` + (qs ? `?${qs}` : '')
  const res = await fetch(url, { method: 'DELETE', headers: withAuth() })
  if (!res.ok) throw new Error(`Delete all sessions failed: ${res.status}`)
}

/** Delete one chat session and its messages. Used by Conversation per-conversation delete button. */
export async function deleteSession(sessionId: string): Promise<void> {
  if (!sessionId) return
  const res = await fetch(`${CHAT_API}/sessions/${encodeURIComponent(sessionId)}`, {
    method: 'DELETE',
    headers: withAuth(),
  })
  if (!res.ok) throw new Error(`Delete session failed: ${res.status}`)
}

export async function createSession(
  tenantId: string,
  options?: { taskQueue?: string; queueName?: string; pipelineId?: string; overrides?: Record<string, unknown> }
): Promise<CreateSessionResponse> {
  const body: CreateSessionBody = {
    tenantId: tenantIdForApiPath(tenantId),
    taskQueue: options?.taskQueue,
    queueName: options?.queueName ?? options?.taskQueue,
    pipelineId: options?.pipelineId,
    overrides: options?.overrides,
  }
  const res = await fetch(`${CHAT_API}/sessions`, {
    method: 'POST',
    headers: withAuth({ 'Content-Type': 'application/json' }),
    body: JSON.stringify(body),
  })
  if (!res.ok) throw new Error(`Create session failed: ${res.status}`)
  return res.json()
}

export async function sendMessage(
  sessionId: string,
  content: string,
  options?: { taskQueue?: string; capabilitySource?: string }
): Promise<SendMessageResponse> {
  const body: Record<string, unknown> = { content, taskQueue: options?.taskQueue }
  const src = options?.capabilitySource?.trim()
  if (src) body.capabilitySource = src
  const res = await fetch(`${CHAT_API}/sessions/${encodeURIComponent(sessionId)}/messages`, {
    method: 'POST',
    headers: withAuth({ 'Content-Type': 'application/json' }),
    body: JSON.stringify(body),
  })
  if (!res.ok) throw new Error(`Send message failed: ${res.status}`)
  return res.json()
}

export async function listMessages(sessionId: string): Promise<ChatMessageDto[]> {
  const res = await fetch(`${CHAT_API}/sessions/${encodeURIComponent(sessionId)}/messages`, {
    headers: withAuth(),
  })
  if (!res.ok) throw new Error(`List messages failed: ${res.status}`)
  return res.json()
}
