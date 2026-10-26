/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import type { ChatProfileDto } from '../api/chatApi'
import { useChatView } from '../hooks/useChatView'
import { ChatComposer } from './chat/ChatComposer'
import { ChatHumanInputCard } from './chat/ChatHumanInputCard'
import { ChatMessagesList } from './chat/ChatMessagesList'
import {
  ChatDisconnectedState,
  ChatEmptySessionState,
  ChatNoProfilesState,
} from './chat/ChatPlaceholderStates'
import { ChatProgressPanel } from './chat/ChatProgressPanel'
import { ChatSuggestionChips } from './chat/ChatSuggestionChips'
import { ChatWaitingBanner } from './chat/ChatWaitingBanner'

export interface ChatViewProps {
  tenantId?: string
  /** When this increments, start a new chat (triggered by Conversation panel "New chat" button). */
  newChatTrigger?: number
  /** From GET /api/ui/context — each preset carries queue + pipeline for sends and sessions. */
  chatProfiles?: ChatProfileDto[]
  /** From App health poll; when false, show disconnected placeholder instead of chat. */
  backendReachable?: boolean
}

export function ChatView({
  tenantId: tenantIdProp,
  newChatTrigger = 0,
  chatProfiles = [],
  backendReachable = false,
}: ChatViewProps) {
  const tenantId = tenantIdProp?.trim() ?? ''
  const view = useChatView({ tenantId, newChatTrigger, chatProfiles })

  if (!backendReachable) return <ChatDisconnectedState />
  if (chatProfiles.length === 0) return <ChatNoProfilesState />

  if (!view.sessionId) {
    return (
      <div className="chat-view">
        <ChatEmptySessionState sessionsCount={view.sessions.length} />
      </div>
    )
  }

  return (
    <div className="chat-view">
      <ChatMessagesList
        loading={view.loading}
        messages={view.messages}
        messagesEndRef={view.messagesEndRef}
        resolveAssistantBubbleText={view.resolveAssistantBubbleText}
        profileByRunId={view.profileByRunId}
        runAgainProfiles={view.runAgainProfiles}
        chatProfiles={view.chatProfiles}
        sending={view.sending}
        showInlineAssistant={view.showInlineAssistant}
        inlineAssistantText={view.inlineAssistantText}
        inlineAssistantProfile={view.inlineAssistantProfile}
        onResend={view.handleResend}
        onResendWithProfile={view.handleResendWithProfile}
      />
      {view.error && (
        <div className="chat-view-error" role="alert">
          {view.error}
        </div>
      )}
      {view.sending && (
        <ChatWaitingBanner
          selectedProfile={view.selectedProfile}
          selectedProfileIndex={view.selectedProfileIndex}
          onCancel={view.handleCancelRun}
          cancelling={view.cancelling}
        />
      )}
      {view.humanInput.pendingHumanEvent && (
        <ChatHumanInputCard
          pendingHumanEvent={view.humanInput.pendingHumanEvent}
          humanPromptMessage={view.humanInput.humanPromptMessage}
          humanStepFooterActions={view.humanInput.humanStepFooterActions}
          humanStepParameters={view.humanInput.humanStepParameters}
          humanPluginName={view.humanInput.humanPluginName}
          humanTaskId={view.humanInput.humanTaskId}
          usesPluginForm={view.humanInput.usesPluginForm}
          humanFieldValues={view.humanInput.humanFieldValues}
          setHumanFieldValue={view.humanInput.setHumanFieldValue}
          pluginFormValid={view.humanInput.pluginFormValid}
          submittingHumanInput={view.humanInput.submittingHumanInput}
          onSubmit={view.humanInput.handleSubmitHumanInput}
        />
      )}
      <ChatSuggestionChips
        disabled={view.sending || !view.sessionId}
        onSelect={view.setInput}
      />
      <ChatComposer
        chatProfiles={view.chatProfiles}
        selectedProfileId={view.selectedProfileId}
        selectedProfile={view.selectedProfile}
        input={view.input}
        setInput={view.setInput}
        sending={view.sending}
        sessionId={view.sessionId}
        selectedRagSource={view.selectedRagSource}
        onRagSourceChange={view.setSelectedRagSource}
        onProfileChange={(id, prof) => {
          view.setSelectedProfileId(id)
          view.setSelectedQueueId(prof.queue)
          view.setSelectedPipelineId(prof.pipeline)
        }}
        onSend={view.handleSend}
      />
      <ChatProgressPanel
        expanded={view.progress.progressExpanded}
        height={view.progress.progressHeight}
        resizing={view.progress.progressResizing}
        events={view.progressEvents}
        onExpand={() => view.progress.setProgressExpanded(true)}
        onCollapse={() => view.progress.setProgressExpanded(false)}
        onResizeStart={view.progress.startResize}
      />
    </div>
  )
}
