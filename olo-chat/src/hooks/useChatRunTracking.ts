/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { getRun, getRunResponse, listMessages } from '../api/chatApi'
import type { ChatMessageDto, RunEventDto } from '../api/chatApi'
import {
  eventsForRun,
  getActiveRunStorageKey,
  isActiveRunForSession,
  isLivenessEvent,
  loadPersistedRunEvents,
  RUN_EVENTS_PERSIST_MAX,
  runEventsStore,
} from '../store/runEvents'
import { isRunTerminalFromApi, isWorkflowFinished } from '../lib/assistantResponse'
import { getCurrentSocket, subscribeToRun } from '../lib/wsSingleton'

export interface UseChatRunTrackingOptions {
  sessionId: string | null
  loading: boolean
  setMessages: React.Dispatch<React.SetStateAction<ChatMessageDto[]>>
  setSending: React.Dispatch<React.SetStateAction<boolean>>
}

export function useChatRunTracking({
  sessionId,
  loading,
  setMessages,
  setSending,
}: UseChatRunTrackingOptions) {
  const runEvents = runEventsStore((s) => s.events)
  const [activeRunId, setActiveRunId] = useState<string | null>(null)
  const [runCompletedFromPoll, setRunCompletedFromPoll] = useState(false)
  const [queriedResponse, setQueriedResponse] = useState<string | null>(null)
  const unsubscribeRunRef = useRef<(() => void) | null>(null)
  const lastOutboundRunIdRef = useRef<string | null>(null)
  const shouldTryRestoreRunEventsRef = useRef(true)

  const handleRunEvent = useCallback(
    (rid: string, _ev: RunEventDto) => {
      if (!sessionId) return
      const active = lastOutboundRunIdRef.current?.trim()
      if (active && rid.trim() !== active) return
      getRunResponse(rid).then((r) => {
        if (r?.response?.trim()) setQueriedResponse(r.response.trim())
      })
      const events = eventsForRun(runEventsStore.getState().events, rid)
      getRun(rid).then((run) => {
        if (!run || !isRunTerminalFromApi(run.status, events)) return
        setRunCompletedFromPoll(true)
        setSending(false)
        listMessages(sessionId).then(setMessages).catch(() => {})
      })
      if (isWorkflowFinished(events)) {
        listMessages(sessionId).then(setMessages).catch(() => {})
      }
    },
    [sessionId, setMessages, setSending]
  )

  useEffect(() => {
    shouldTryRestoreRunEventsRef.current = true
  }, [sessionId])

  useEffect(() => {
    if (!sessionId) return
    setRunCompletedFromPoll(false)
    setQueriedResponse(null)
    setActiveRunId(null)
    runEventsStore.getState().clear()
    unsubscribeRunRef.current?.()
  }, [sessionId])

  useEffect(() => {
    if (!sessionId || loading || !shouldTryRestoreRunEventsRef.current) return
    const rid = sessionStorage.getItem(getActiveRunStorageKey(sessionId))?.trim()
    if (!rid) {
      shouldTryRestoreRunEventsRef.current = false
      return
    }
    const persisted = loadPersistedRunEvents(rid)
    if (!persisted?.length) {
      shouldTryRestoreRunEventsRef.current = false
      return
    }
    runEventsStore.getState().hydrate(rid, persisted)
    lastOutboundRunIdRef.current = rid
    setActiveRunId(rid)

    const onRestoredRunEvent = (eventRid: string, ev: RunEventDto) => {
      handleRunEvent(eventRid, ev)
    }
    runEventsStore.getState().setOnRunEventCallback(onRestoredRunEvent)

    if (getCurrentSocket()) {
      subscribeToRun(rid)
    }
    shouldTryRestoreRunEventsRef.current = false
  }, [sessionId, loading, handleRunEvent])

  useEffect(() => {
    return () => {
      unsubscribeRunRef.current?.()
    }
  }, [])

  const currentRunEvents = useMemo(
    () => eventsForRun(runEvents, activeRunId),
    [runEvents, activeRunId]
  )

  const humanWaitingRefetchKey = useMemo(() => {
    const w = [...currentRunEvents]
      .reverse()
      .find((e) => e.nodeType?.toUpperCase() === 'HUMAN' && e.status?.toUpperCase() === 'WAITING')
    return w ? `${w.sequenceNumber ?? 0}:${w.nodeId ?? ''}` : ''
  }, [currentRunEvents])

  useEffect(() => {
    if (!sessionId || !humanWaitingRefetchKey) return
    if (!isActiveRunForSession(sessionId, activeRunId)) return
    listMessages(sessionId).then(setMessages).catch(() => {})
  }, [sessionId, activeRunId, humanWaitingRefetchKey, setMessages])

  const progressEvents = runEvents.filter((e) => !isLivenessEvent(e)).slice(-RUN_EVENTS_PERSIST_MAX)

  return {
    runEvents,
    activeRunId,
    setActiveRunId,
    runCompletedFromPoll,
    setRunCompletedFromPoll,
    queriedResponse,
    setQueriedResponse,
    currentRunEvents,
    progressEvents,
    handleRunEvent,
    unsubscribeRunRef,
    lastOutboundRunIdRef,
  }
}
