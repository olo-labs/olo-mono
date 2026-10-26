/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { useCallback } from 'react'
import { listMessages, sendMessage, streamRunEvents } from '../api/chatApi'
import type { ChatMessageDto, RunEventDto } from '../api/chatApi'
import { getActiveRunStorageKey, runEventsStore } from '../store/runEvents'
import { conversationPanelStore } from '../store/conversationPanel'
import { queueDisplayName } from '../lib/queueDisplayName'
import { getCurrentSocket, subscribeToRun } from '../lib/wsSingleton'

export interface UseChatMessagingOptions {
  sessionId: string | null
  sending: boolean
  selectedRagSource: string
  setSending: React.Dispatch<React.SetStateAction<boolean>>
  setError: React.Dispatch<React.SetStateAction<string | null>>
  setMessages: React.Dispatch<React.SetStateAction<ChatMessageDto[]>>
  setInput: React.Dispatch<React.SetStateAction<string>>
  setActiveRunId: (id: string | null) => void
  setRunCompletedFromPoll: (v: boolean) => void
  setQueriedResponse: (v: string | null) => void
  fetchSessions: () => void
  recordProfileForRun: (runId: string) => void
  handleRunEvent: (rid: string, ev: RunEventDto) => void
  unsubscribeRunRef: React.MutableRefObject<(() => void) | null>
  lastOutboundRunIdRef: React.MutableRefObject<string | null>
}

function subscribeToRunEvents(
  runId: string,
  onEvent: (rid: string, ev: RunEventDto) => void,
  onStreamError: (err: unknown) => void
): () => void {
  runEventsStore.getState().setOnRunEventCallback(onEvent)
  if (getCurrentSocket()) {
    console.log('[Chat] C. WebSocket SUBSCRIBE_RUN', { runId })
    subscribeToRun(runId)
    return () => {}
  }
  console.log('[Chat] C. streamRunEvents subscribe (SSE)', { runId })
  return streamRunEvents(
    runId,
    (ev) => {
      runEventsStore.getState().addEvent(ev)
      onEvent(runId, ev)
    },
    onStreamError
  )
}

export function useChatMessaging(opts: UseChatMessagingOptions) {
  const {
    sessionId,
    sending,
    selectedRagSource,
    setSending,
    setError,
    setMessages,
    setInput,
    setActiveRunId,
    setRunCompletedFromPoll,
    setQueriedResponse,
    fetchSessions,
    recordProfileForRun,
    handleRunEvent,
    unsubscribeRunRef,
    lastOutboundRunIdRef,
  } = opts

  const beginRun = useCallback(
    (runId: string) => {
      if (!sessionId) return
      lastOutboundRunIdRef.current = runId
      recordProfileForRun(runId)
      try {
        sessionStorage.setItem(getActiveRunStorageKey(sessionId), runId)
      } catch {
        /* quota */
      }
      setActiveRunId(runId)
      runEventsStore.getState().setRun(runId)
      unsubscribeRunRef.current?.()
      unsubscribeRunRef.current = subscribeToRunEvents(runId, handleRunEvent, (err) => {
        console.log('[Chat] E. stream onError', err)
        setError(String(err))
        setSending(false)
      })
    },
    [
      sessionId,
      recordProfileForRun,
      setActiveRunId,
      handleRunEvent,
      unsubscribeRunRef,
      lastOutboundRunIdRef,
      setError,
      setSending,
    ]
  )

  const handleResend = useCallback(
    (content: string, logPrefix = '[Chat Resend]') => {
      if (!content?.trim() || !sessionId || sending) return
      const { selectedQueueId: q } = conversationPanelStore.getState()
      setSending(true)
      setError(null)
      setActiveRunId(null)
      setRunCompletedFromPoll(false)
      setQueriedResponse(null)
      sendMessage(sessionId, content.trim(), {
        taskQueue: q ? queueDisplayName(q) : undefined,
        capabilitySource: selectedRagSource.trim() || undefined,
      })
        .then(({ runId }) => {
          console.log(`${logPrefix} B. sendMessage HTTP resolved`, { runId })
          listMessages(sessionId).then(setMessages).catch(() => {})
          fetchSessions()
          beginRun(runId)
        })
        .catch((e) => {
          console.log(`${logPrefix} F. sendMessage HTTP catch`, e?.message)
          setError(String(e.message))
          setSending(false)
        })
    },
    [
      sessionId,
      sending,
      setSending,
      setError,
      setActiveRunId,
      setRunCompletedFromPoll,
      setQueriedResponse,
      setMessages,
      selectedRagSource,
      fetchSessions,
      beginRun,
    ]
  )

  const handleSend = useCallback(
    (input: string) => {
      const text = input.trim()
      if (!text || !sessionId || sending) return
      setInput('')
      setSending(true)
      setError(null)
      setActiveRunId(null)
      setRunCompletedFromPoll(false)
      setQueriedResponse(null)
      const optimisticUser: ChatMessageDto = {
        messageId: `opt-${Date.now()}`,
        sessionId,
        role: 'user',
        content: text,
        runId: '',
        createdAt: Date.now(),
      }
      setMessages((prev) => [...prev, optimisticUser])
      const { selectedQueueId: q } = conversationPanelStore.getState()
      console.log('[Chat] A. sendMessage HTTP start')
      sendMessage(sessionId, text, {
        taskQueue: q ? queueDisplayName(q) : undefined,
        capabilitySource: selectedRagSource.trim() || undefined,
      })
        .then(({ runId }) => {
          console.log('[Chat] B. sendMessage HTTP resolved', { runId })
          listMessages(sessionId)
            .then((data) => {
              setMessages((prev) => {
                if (data.length > 0) return data
                const hasOptimistic = prev.some((m) => String(m.messageId).startsWith('opt-'))
                if (hasOptimistic) return prev
                return data
              })
            })
            .catch(() => {})
          fetchSessions()
          beginRun(runId)
        })
        .catch((e) => {
          console.log('[Chat] F. sendMessage HTTP catch', e?.message)
          setError(String(e.message))
          setSending(false)
        })
    },
    [
      sessionId,
      sending,
      setInput,
      setSending,
      setError,
      setActiveRunId,
      setRunCompletedFromPoll,
      setQueriedResponse,
      setMessages,
      selectedRagSource,
      fetchSessions,
      beginRun,
    ]
  )

  return { handleSend, handleResend }
}
