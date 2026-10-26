/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import {
  fetchRunResponseWithRetry,
  listRuntimeMessages,
  type RunEventDto,
} from '../api/oloRuntime'
import {
  normalizeResponseText,
  pickResponseFromEvents,
} from './assistantResponse'

export async function resolveWorkflowReturnText(
  runId: string,
  sessionId: string,
  events: RunEventDto[],
): Promise<string | null> {
  const fromEvents = normalizeResponseText(pickResponseFromEvents(events))
  if (fromEvents) return fromEvents

  const fromApi = normalizeResponseText(await fetchRunResponseWithRetry(runId))
  if (fromApi) return fromApi

  try {
    const messages = await listRuntimeMessages(sessionId)
    const assistant = [...messages]
      .reverse()
      .find((m) => m.role === 'assistant' && m.runId === runId && m.content?.trim())
    const fromHistory = normalizeResponseText(assistant?.content)
    if (fromHistory) return fromHistory
  } catch {
    // optional fallback
  }

  return null
}
