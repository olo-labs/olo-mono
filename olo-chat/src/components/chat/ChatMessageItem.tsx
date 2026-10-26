/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import type { ChatMessageDto, ChatProfileDto } from '../../api/chatApi'
import { formatProfileOptionLabel } from '../../lib/chatProfileUi'
import { isHumanStepReplyMessage, normalizeHumanStepHistoryContent } from '../../lib/chatHumanStep'

export interface ChatMessageItemProps {
  message: ChatMessageDto
  index: number
  messages: ChatMessageDto[]
  profileByRunId: Record<string, { profileId: string; label: string }>
  runAgainProfiles: ChatProfileDto[]
  chatProfiles: ChatProfileDto[]
  assistantText: string | null
  sending: boolean
  onResend: (content: string) => void
  onResendWithProfile: (profileId: string, content: string) => void
}

export function ChatMessageItem({
  message: m,
  index,
  messages,
  profileByRunId,
  runAgainProfiles,
  chatProfiles,
  assistantText,
  sending,
  onResend,
  onResendWithProfile,
}: ChatMessageItemProps) {
  const resendContent =
    m.role === 'user'
      ? m.content
      : messages
          .slice(0, index)
          .filter((msg) => msg.role === 'user')
          .pop()?.content
  const usedProfileId = m.runId ? profileByRunId[m.runId]?.profileId : undefined
  const runAgainTargets =
    m.role === 'user' && !isHumanStepReplyMessage(messages, index) && runAgainProfiles.length > 0
      ? runAgainProfiles.filter((p) => p.id !== usedProfileId)
      : []

  return (
    <div className={`chat-view-message-wrap chat-view-message-wrap-${m.role}`}>
      <div className={`chat-view-message chat-view-message-${m.role}`}>
        <div className="chat-view-message-header">
          <div className="chat-view-message-header-main">
            <span className="chat-view-message-role">{m.role}</span>
            {m.runId && profileByRunId[m.runId] && (
              <span className="chat-view-message-config" title="Preset used for this turn">
                {profileByRunId[m.runId].label}
              </span>
            )}
          </div>
          {m.role === 'user' && resendContent && (
            <button
              type="button"
              className="chat-view-message-resend"
              onClick={() => onResend(resendContent)}
              disabled={sending}
              aria-label="Resend"
              title="Resend"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
                <path d="M1 4v6h6" />
                <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10" />
              </svg>
            </button>
          )}
        </div>
        <div className="chat-view-message-content">
          {m.role === 'assistant' ? assistantText : m.content}
        </div>
        {m.role === 'user' && runAgainTargets.length > 0 && (
          <div className="chat-view-msg-runagain-wrap">
            <details className="chat-view-msg-runagain">
              <summary
                className="chat-view-msg-runagain-trigger"
                aria-label="Run again with another preset"
                title="Run again with another preset"
              >
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  aria-hidden
                >
                  <path d="M17 1l4 4-4 4" />
                  <path d="M3 11V9a4 4 0 0 1 4-4h14" />
                  <path d="M7 23l-4-4 4-4" />
                  <path d="M21 13v2a4 4 0 0 1-4 4H3" />
                </svg>
              </summary>
              <div className="chat-view-msg-runagain-menu" role="menu">
                {runAgainTargets.map((p) => {
                  const idx = chatProfiles.findIndex((x) => x.id === p.id)
                  const label = formatProfileOptionLabel(p, idx >= 0 ? idx : 0)
                  return (
                    <button
                      key={p.id}
                      type="button"
                      className="chat-view-msg-runagain-opt"
                      role="menuitem"
                      title={label}
                      disabled={sending}
                      onClick={(e) => {
                        onResendWithProfile(p.id, m.content)
                        const details = (e.currentTarget as HTMLElement).closest('details')
                        if (details) details.open = false
                      }}
                    >
                      {label}
                    </button>
                  )
                })}
              </div>
            </details>
          </div>
        )}
      </div>
    </div>
  )
}

export function ChatInlineAssistant({
  text,
  profileLabel,
}: {
  text: string
  profileLabel?: string
}) {
  return (
    <div className="chat-view-message-wrap chat-view-message-wrap-assistant">
      <div className="chat-view-message chat-view-message-assistant">
        <div className="chat-view-message-header">
          <div className="chat-view-message-header-main">
            <span className="chat-view-message-role">assistant</span>
            {profileLabel && (
              <span className="chat-view-message-config" title="Preset for this reply">
                {profileLabel}
              </span>
            )}
          </div>
        </div>
        <div className="chat-view-message-content">
          {normalizeHumanStepHistoryContent(text) ?? text}
        </div>
      </div>
    </div>
  )
}
