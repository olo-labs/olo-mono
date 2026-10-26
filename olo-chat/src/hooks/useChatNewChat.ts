/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { useCallback, useEffect, useRef } from 'react'
import { createSession } from '../api/chatApi'
import { chatSessionsStore } from '../store/chatSessions'
import { runEventsStore } from '../store/runEvents'
import type { useChatRunTracking } from './useChatRunTracking'

export function useChatNewChat({
  tenantId,
  newChatTrigger,
  sending,
  setError,
  setMessages,
  fetchSessions,
  lastCreatedSessionIdRef,
  runTracking,
}: {
  tenantId: string
  newChatTrigger: number
  sending: boolean
  setError: React.Dispatch<React.SetStateAction<string | null>>
  setMessages: React.Dispatch<React.SetStateAction<import('../api/chatApi').ChatMessageDto[]>>
  fetchSessions: () => void
  lastCreatedSessionIdRef: React.MutableRefObject<string | null>
  runTracking: ReturnType<typeof useChatRunTracking>
}) {
  const handleNewChat = useCallback(() => {
    if (!tenantId || sending) return
    setError(null)
    runTracking.setRunCompletedFromPoll(false)
    runTracking.setQueriedResponse(null)
    runTracking.setActiveRunId(null)
    runEventsStore.getState().clear()
    runTracking.unsubscribeRunRef.current?.()
    createSession(tenantId, {})
      .then((r) => {
        lastCreatedSessionIdRef.current = r.sessionId
        const now = Date.now()
        const prev = chatSessionsStore.getState().sessions
        chatSessionsStore.getState().setSessions([
          { sessionId: r.sessionId, tenantId, createdAt: now, lastActivityAt: now },
          ...prev,
        ])
        chatSessionsStore.getState().setSelectedSessionId(r.sessionId)
        setMessages([])
        fetchSessions()
      })
      .catch((e) => setError(String(e.message)))
  }, [tenantId, sending, fetchSessions, lastCreatedSessionIdRef, runTracking, setError, setMessages])

  const newChatTriggerRef = useRef(0)
  useEffect(() => {
    if (newChatTrigger > newChatTriggerRef.current) {
      newChatTriggerRef.current = newChatTrigger
      handleNewChat()
    }
  }, [newChatTrigger, handleNewChat])
}
