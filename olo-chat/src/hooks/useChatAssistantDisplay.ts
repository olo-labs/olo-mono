/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { useEffect } from 'react'
import type { ChatMessageDto, RunEventDto } from '../api/chatApi'
import {
  fallbackResponseMessage,
  isWorkflowCancelled,
  isWorkflowFinished,
  normalizeResponseText,
  pickResponseFromEvents,
  resolvePersistedAssistantContent,
} from '../lib/assistantResponse'
import { resolveHumanStepAssistantDisplay } from '../lib/chatHumanStep'
import { eventsForRun } from '../store/runEvents'

export function useChatWorkflowComplete(
  sending: boolean,
  activeRunId: string | null,
  currentRunEvents: RunEventDto[],
  setSending: React.Dispatch<React.SetStateAction<boolean>>
) {
  useEffect(() => {
    if (!sending || !activeRunId) return
    const summary = currentRunEvents.map((e) => ({
      nodeType: e.nodeType,
      status: e.status,
      hasOutput: e.output != null,
      match:
        e.nodeType?.toUpperCase() === 'MODEL' &&
        (e.status?.toUpperCase() === 'COMPLETED' || e.status) &&
        e.output != null,
    }))
    const hasWorkflowComplete = isWorkflowFinished(currentRunEvents)
    console.log('[Chat] G. workflow-complete check', {
      runEventsCount: currentRunEvents.length,
      summary,
      hasWorkflowComplete,
    })
    if (hasWorkflowComplete) {
      console.log('[Chat] H. setSending(false) — workflow complete')
      setSending(false)
    }
  }, [sending, activeRunId, currentRunEvents, setSending])
}

export function computeChatAssistantDisplay({
  messages,
  sending,
  runCompletedFromPoll,
  queriedResponse,
  currentRunEvents,
  runEvents,
  activeRunId,
  lastOutboundRunId,
  profileByRunId,
}: {
  messages: ChatMessageDto[]
  sending: boolean
  runCompletedFromPoll: boolean
  queriedResponse: string | null
  currentRunEvents: RunEventDto[]
  runEvents: RunEventDto[]
  activeRunId: string | null
  lastOutboundRunId: string | null
  profileByRunId: Record<string, { profileId: string; label: string }>
}) {
  const runFailed = currentRunEvents.some(
    (e) => e.nodeType?.toUpperCase() === 'SYSTEM' && e.status?.toUpperCase() === 'FAILED'
  ) && !isWorkflowCancelled(currentRunEvents)
  const runCancelled = isWorkflowCancelled(currentRunEvents)
  const runTerminal = runCompletedFromPoll || (!sending && isWorkflowFinished(currentRunEvents))
  const workflowReturnText =
    normalizeResponseText(queriedResponse) ?? normalizeResponseText(pickResponseFromEvents(currentRunEvents))
  const inlineAssistantText =
    workflowReturnText ??
    (runTerminal && !sending
      ? fallbackResponseMessage(runCancelled ? 'cancelled' : runFailed ? 'failed' : 'completed')
      : null)

  const assistantMessageContext = { activeRunId, sending, events: currentRunEvents }

  const resolveAssistantBubbleText = (m: ChatMessageDto, index: number): string | null =>
    resolvePersistedAssistantContent(
      resolveHumanStepAssistantDisplay(messages, index, m.content),
      {
        ...assistantMessageContext,
        runId: m.runId,
        events: m.runId ? eventsForRun(runEvents, m.runId) : currentRunEvents,
      }
    )

  const lastMsg = messages.length > 0 ? messages[messages.length - 1] : null
  const lastMessageIsAssistant =
    lastMsg?.role === 'assistant' && resolveAssistantBubbleText(lastMsg, messages.length - 1) != null
  const showInlineAssistant = inlineAssistantText != null && !lastMessageIsAssistant
  const inlineAssistantRunId = runEvents[0]?.runId ?? lastOutboundRunId ?? ''
  const inlineAssistantProfile = inlineAssistantRunId ? profileByRunId[inlineAssistantRunId] : undefined

  return {
    resolveAssistantBubbleText,
    showInlineAssistant,
    inlineAssistantText,
    inlineAssistantProfile,
  }
}
