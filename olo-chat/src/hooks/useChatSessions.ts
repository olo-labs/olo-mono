/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { useCallback, useEffect, useLayoutEffect, useMemo, useRef } from 'react'
import { listSessions } from '../api/chatApi'
import type { ChatProfileDto } from '../api/chatApi'
import { chatSessionsStore } from '../store/chatSessions'
import { conversationPanelStore } from '../store/conversationPanel'

export function useChatSessions(tenantId: string, chatProfiles: ChatProfileDto[]) {
  const sessions = chatSessionsStore((s) => s.sessions)
  const sessionId = chatSessionsStore((s) => s.selectedSessionId)
  const setSessions = chatSessionsStore((s) => s.setSessions)
  const setSelectedSessionId = chatSessionsStore((s) => s.setSelectedSessionId)
  const selectedProfileId = conversationPanelStore((s) => s.selectedProfileId)
  const setSelectedProfileId = conversationPanelStore((s) => s.setSelectedProfileId)
  const setSelectedQueueId = conversationPanelStore((s) => s.setSelectedQueueId)
  const setSelectedPipelineId = conversationPanelStore((s) => s.setSelectedPipelineId)
  const lastCreatedSessionIdRef = useRef<string | null>(null)

  useLayoutEffect(() => {
    if (chatProfiles.length === 0) return
    const validIds = new Set(chatProfiles.map((p) => p.id))
    const current = conversationPanelStore.getState().selectedProfileId
    if (current && validIds.has(current)) return
    const first = chatProfiles[0]
    setSelectedProfileId(first.id)
    setSelectedQueueId(first.queue)
    setSelectedPipelineId(first.pipeline)
  }, [chatProfiles, setSelectedProfileId, setSelectedQueueId, setSelectedPipelineId])

  const selectedProfile = useMemo(() => {
    if (chatProfiles.length === 0) return null
    return chatProfiles.find((p) => p.id === selectedProfileId) ?? chatProfiles[0]
  }, [chatProfiles, selectedProfileId])

  const runAgainProfiles = useMemo(
    () => chatProfiles.filter((p) => p.runAgain === true),
    [chatProfiles]
  )

  const selectedProfileIndex = useMemo(() => {
    if (!selectedProfile) return 0
    const i = chatProfiles.findIndex((p) => p.id === selectedProfile.id)
    return i >= 0 ? i : 0
  }, [chatProfiles, selectedProfile])

  const fetchSessions = useCallback(() => {
    if (!tenantId || chatProfiles.length === 0) return
    listSessions(tenantId)
      .then((data) => {
        setSessions(data)
        const current = chatSessionsStore.getState().selectedSessionId
        const inList = data.some((s) => s.sessionId === current)
        if (data.length > 0 && (!current || !inList)) {
          setSelectedSessionId(data[0].sessionId)
        } else if (current && !inList) {
          if (current !== lastCreatedSessionIdRef.current) {
            setSelectedSessionId(null)
          } else {
            lastCreatedSessionIdRef.current = null
          }
        }
      })
      .catch(() => setSessions([]))
  }, [tenantId, chatProfiles.length, setSessions, setSelectedSessionId])

  useEffect(() => {
    if (!tenantId || chatProfiles.length === 0) {
      setSessions([])
      setSelectedSessionId(null)
      return
    }
    fetchSessions()
  }, [tenantId, chatProfiles.length, fetchSessions, setSelectedSessionId, setSessions])

  useEffect(() => {
    if (sessions.length > 0 && !sessionId) {
      setSelectedSessionId(sessions[0].sessionId)
    }
  }, [sessions, sessionId, setSelectedSessionId])

  return {
    sessions,
    sessionId,
    selectedProfile,
    selectedProfileId,
    selectedProfileIndex,
    runAgainProfiles,
    setSelectedProfileId,
    setSelectedQueueId,
    setSelectedPipelineId,
    fetchSessions,
    lastCreatedSessionIdRef,
  }
}
