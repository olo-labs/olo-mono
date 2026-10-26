/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { useCallback, useState } from 'react'
import { cancelRun, listMessages } from '../api/chatApi'
import { getActiveRunStorageKey } from '../store/runEvents'

export interface UseChatCancelRunOptions {
  sessionId: string | null
  activeRunId: string | null
  sending: boolean
  setSending: React.Dispatch<React.SetStateAction<boolean>>
  setError: React.Dispatch<React.SetStateAction<string | null>>
  setMessages: React.Dispatch<React.SetStateAction<import('../api/chatApi').ChatMessageDto[]>>
  unsubscribeRunRef: React.MutableRefObject<(() => void) | null>
  setActiveRunId: (id: string | null) => void
}

export function useChatCancelRun({
  sessionId,
  activeRunId,
  sending,
  setSending,
  setError,
  setMessages,
  unsubscribeRunRef,
  setActiveRunId,
}: UseChatCancelRunOptions) {
  const [cancelling, setCancelling] = useState(false)

  const handleCancelRun = useCallback(async () => {
    if (!activeRunId || !sending || cancelling) return
    setCancelling(true)
    setError(null)
    try {
      await cancelRun(activeRunId)
      unsubscribeRunRef.current?.()
      unsubscribeRunRef.current = null
      setSending(false)
      setActiveRunId(null)
      if (sessionId) {
        try {
          sessionStorage.removeItem(getActiveRunStorageKey(sessionId))
        } catch {
          /* quota */
        }
        listMessages(sessionId).then(setMessages).catch(() => {})
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Cancel failed')
    } finally {
      setCancelling(false)
    }
  }, [
    activeRunId,
    cancelling,
    sending,
    sessionId,
    setActiveRunId,
    setError,
    setMessages,
    setSending,
    unsubscribeRunRef,
  ])

  return { cancelling, handleCancelRun }
}
