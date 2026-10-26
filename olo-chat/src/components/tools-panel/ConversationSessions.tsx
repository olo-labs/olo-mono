/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { useEffect, useRef, useState } from 'react'
import { deleteAllSessions, deleteSession } from '../../api/chatApi'
import { chatSessionsStore } from '../../store/chatSessions'
import { conversationPanelStore } from '../../store/conversationPanel'
import { runEventsStore } from '../../store/runEvents'
import { sessionDisplayStore } from '../../store/sessionDisplay'
import { queueDisplayName } from '../../lib/queueDisplayName'
import { getSessionDisplay } from '../../lib/sessionDisplayUi'

export interface ConversationSessionsProps {
  tenantId: string
  onNewChat?: () => void
}

export function ConversationSessions({ tenantId, onNewChat }: ConversationSessionsProps) {
  const selectedQueueId = conversationPanelStore((s) => s.selectedQueueId)
  const selectedPipelineId = conversationPanelStore((s) => s.selectedPipelineId)
  const [deletingAll, setDeletingAll] = useState(false)
  const [deletingSessionId, setDeletingSessionId] = useState<string | null>(null)
  const [editingSessionId, setEditingSessionId] = useState<string | null>(null)
  const [editDraft, setEditDraft] = useState('')
  const editInputRef = useRef<HTMLInputElement>(null)
  const sessionDisplayEntries = sessionDisplayStore((s) => s.entries)
  const setCustomTitle = sessionDisplayStore((s) => s.setCustomTitle)
  const removeSessionDisplay = sessionDisplayStore((s) => s.removeSession)
  const removeSessionsDisplay = sessionDisplayStore((s) => s.removeSessions)
  const sessions = chatSessionsStore((s) => s.sessions)
  const selectedSessionId = chatSessionsStore((s) => s.selectedSessionId)
  const setSessions = chatSessionsStore((s) => s.setSessions)
  const setSelectedSessionId = chatSessionsStore((s) => s.setSelectedSessionId)

  useEffect(() => {
    if (editingSessionId) editInputRef.current?.focus()
  }, [editingSessionId])

  return (
    <>
      <div className="conversation-new-chat-wrap">
        <button
          type="button"
          className="conversation-new-chat"
          onClick={() => onNewChat?.()}
          aria-label="Start a new chat"
        >
          New chat
        </button>
      </div>
      <div className="conversation-sessions-block">
        <ul className="conversation-sessions-list" role="list">
          {sessions.map((s) => {
            const isEditing = editingSessionId === s.sessionId
            const display = getSessionDisplay(s)
            return (
              <li key={s.sessionId} className="conversation-session-item">
                <div className="conversation-session-btn-wrap">
                  {isEditing ? (
                    <input
                      ref={editInputRef}
                      type="text"
                      className="conversation-session-edit-input"
                      value={editDraft}
                      onChange={(e) => setEditDraft(e.target.value)}
                      onBlur={() => {
                        setCustomTitle(s.sessionId, editDraft)
                        setEditingSessionId(null)
                      }}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter') {
                          setCustomTitle(s.sessionId, editDraft)
                          setEditingSessionId(null)
                        } else if (e.key === 'Escape') {
                          setEditDraft(sessionDisplayEntries[s.sessionId]?.customTitle ?? '')
                          setEditingSessionId(null)
                        }
                      }}
                      onClick={(e) => e.stopPropagation()}
                      aria-label="Edit conversation name"
                    />
                  ) : (
                    <button
                      type="button"
                      className={`conversation-session-btn ${s.sessionId === selectedSessionId ? 'active' : ''}`}
                      onClick={() => setSelectedSessionId(s.sessionId)}
                    >
                      <span className="conversation-session-label">{display.primary}</span>
                      {display.subtitle && (
                        <span className="conversation-session-preview">{display.subtitle}</span>
                      )}
                    </button>
                  )}
                  {!isEditing && (
                    <button
                      type="button"
                      className="conversation-session-edit"
                      onClick={(e) => {
                        e.stopPropagation()
                        setEditingSessionId(s.sessionId)
                        setEditDraft(sessionDisplayEntries[s.sessionId]?.customTitle ?? '')
                      }}
                      aria-label="Edit name"
                      title="Edit name"
                    >
                      ✎
                    </button>
                  )}
                </div>
                <button
                  type="button"
                  className="conversation-session-delete"
                  onClick={(e) => {
                    e.stopPropagation()
                    if (deletingSessionId || deletingAll) return
                    setDeletingSessionId(s.sessionId)
                    deleteSession(s.sessionId)
                      .then(() => {
                        removeSessionDisplay(s.sessionId)
                        const wasSelected = s.sessionId === selectedSessionId
                        const current = chatSessionsStore.getState().sessions
                        const next = current.filter((sess) => sess.sessionId !== s.sessionId)
                        setSessions(next)
                        if (wasSelected) {
                          setSelectedSessionId(next[0]?.sessionId ?? null)
                          runEventsStore.getState().clear()
                        }
                      })
                      .finally(() => setDeletingSessionId(null))
                  }}
                  disabled={deletingAll}
                  aria-label={`Delete conversation ${display.primary}`}
                  title="Delete conversation"
                >
                  {deletingSessionId === s.sessionId ? '…' : '×'}
                </button>
              </li>
            )
          })}
        </ul>
        <div className="conversation-delete-all-wrap">
          <button
            type="button"
            className="conversation-delete-all"
            onClick={() => {
              if (!tenantId || deletingAll) return
              setDeletingAll(true)
              deleteAllSessions(tenantId, {
                queue: selectedQueueId ? queueDisplayName(selectedQueueId) : undefined,
                pipeline: selectedPipelineId || undefined,
              })
                .then(() => {
                  removeSessionsDisplay(sessions.map((s) => s.sessionId))
                  setSessions([])
                  setSelectedSessionId(null)
                  runEventsStore.getState().clear()
                })
                .finally(() => setDeletingAll(false))
            }}
            disabled={sessions.length === 0 || deletingAll}
            aria-label="Delete all conversations"
          >
            {deletingAll ? '…' : 'Delete all'}
          </button>
        </div>
      </div>
    </>
  )
}
