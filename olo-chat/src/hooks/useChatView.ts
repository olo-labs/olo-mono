/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { useCallback, useEffect, useRef, useState } from 'react'
import { listMessages } from '../api/chatApi'
import type { ChatMessageDto, ChatProfileDto } from '../api/chatApi'
import { sessionDisplayStore } from '../store/sessionDisplay'
import { useChatSessions } from './useChatSessions'
import { useChatProfileByRun } from './useChatProfileByRun'
import { useChatRunTracking } from './useChatRunTracking'
import { useChatMessaging } from './useChatMessaging'
import { useChatCancelRun } from './useChatCancelRun'
import { useChatHumanInput } from './useChatHumanInput'
import { useChatProgressPanel } from './useChatProgressPanel'
import { useChatNewChat } from './useChatNewChat'
import { computeChatAssistantDisplay, useChatWorkflowComplete } from './useChatAssistantDisplay'

export interface UseChatViewOptions {
  tenantId: string
  newChatTrigger?: number
  chatProfiles: ChatProfileDto[]
}

export function useChatView({ tenantId, newChatTrigger = 0, chatProfiles }: UseChatViewOptions) {
  const [messages, setMessages] = useState<ChatMessageDto[]>([])
  const [loading, setLoading] = useState(true)
  const [sending, setSending] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [input, setInput] = useState('')
  const [selectedRagSource, setSelectedRagSource] = useState('')
  const messagesEndRef = useRef<HTMLDivElement>(null)

  const sessionsApi = useChatSessions(tenantId, chatProfiles)
  const {
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
  } = sessionsApi

  const { profileByRunId, recordProfileForRun } = useChatProfileByRun(sessionId, chatProfiles)
  const runTracking = useChatRunTracking({ sessionId, loading, setMessages, setSending })

  const humanInput = useChatHumanInput(
    runTracking.currentRunEvents,
    runTracking.activeRunId,
    sessionId,
    !!sessionId,
    setMessages,
    setError
  )
  const progress = useChatProgressPanel()

  const { handleSend, handleResend } = useChatMessaging({
    sessionId,
    sending,
    selectedRagSource,
    setSending,
    setError,
    setMessages,
    setInput,
    setActiveRunId: runTracking.setActiveRunId,
    setRunCompletedFromPoll: runTracking.setRunCompletedFromPoll,
    setQueriedResponse: runTracking.setQueriedResponse,
    fetchSessions,
    recordProfileForRun,
    handleRunEvent: runTracking.handleRunEvent,
    unsubscribeRunRef: runTracking.unsubscribeRunRef,
    lastOutboundRunIdRef: runTracking.lastOutboundRunIdRef,
  })

  const { cancelling, handleCancelRun } = useChatCancelRun({
    sessionId,
    activeRunId: runTracking.activeRunId,
    sending,
    setSending,
    setError,
    setMessages,
    unsubscribeRunRef: runTracking.unsubscribeRunRef,
    setActiveRunId: runTracking.setActiveRunId,
  })

  useChatNewChat({
    tenantId,
    newChatTrigger,
    sending,
    setError,
    setMessages,
    fetchSessions,
    lastCreatedSessionIdRef,
    runTracking,
  })

  useChatWorkflowComplete(
    sending,
    runTracking.activeRunId,
    runTracking.currentRunEvents,
    setSending
  )

  const assistant = computeChatAssistantDisplay({
    messages,
    sending,
    runCompletedFromPoll: runTracking.runCompletedFromPoll,
    queriedResponse: runTracking.queriedResponse,
    currentRunEvents: runTracking.currentRunEvents,
    runEvents: runTracking.runEvents,
    activeRunId: runTracking.activeRunId,
    lastOutboundRunId: runTracking.lastOutboundRunIdRef.current,
    profileByRunId,
  })

  const scrollToBottom = useCallback(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [])

  useEffect(() => {
    if (!sessionId) return
    setLoading(true)
    setError(null)
    listMessages(sessionId)
      .then(setMessages)
      .catch((e) => setError(String(e.message)))
      .finally(() => setLoading(false))
  }, [sessionId])

  useEffect(() => {
    if (!sessionId || messages.length === 0) return
    const firstUser = messages.find((m) => m.role === 'user')
    if (firstUser?.content?.trim()) {
      sessionDisplayStore.getState().setFirstMessagePreview(sessionId, firstUser.content)
    }
  }, [sessionId, messages])

  useEffect(() => {
    scrollToBottom()
  }, [messages, runTracking.runEvents, scrollToBottom])

  const handleResendWithProfile = useCallback(
    (profileId: string, content: string) => {
      const prof = chatProfiles.find((p) => p.id === profileId)
      if (!prof || !content?.trim()) return
      setSelectedProfileId(profileId)
      setSelectedQueueId(prof.queue)
      setSelectedPipelineId(prof.pipeline)
      handleResend(content.trim())
    },
    [chatProfiles, setSelectedProfileId, setSelectedQueueId, setSelectedPipelineId, handleResend]
  )

  return {
    sessions,
    sessionId,
    messages,
    loading,
    sending,
    error,
    input,
    setInput,
    selectedRagSource,
    setSelectedRagSource,
    chatProfiles,
    selectedProfile,
    selectedProfileId,
    selectedProfileIndex,
    runAgainProfiles,
    setSelectedProfileId,
    setSelectedQueueId,
    setSelectedPipelineId,
    profileByRunId,
    resolveAssistantBubbleText: assistant.resolveAssistantBubbleText,
    showInlineAssistant: assistant.showInlineAssistant,
    inlineAssistantText: assistant.inlineAssistantText,
    inlineAssistantProfile: assistant.inlineAssistantProfile,
    messagesEndRef,
    handleSend: () => handleSend(input),
    handleResend,
    handleResendWithProfile,
    handleCancelRun,
    cancelling,
    humanInput,
    progress,
    progressEvents: runTracking.progressEvents,
  }
}
