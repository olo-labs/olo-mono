/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Contextual tools panel: tools for the current view (section + sub-option).
 * For Chat: **New chat** and sessions list. Queue/pipeline for sends come from the selected profile in ChatView.
 */

import { useEffect } from 'react'
import type { SectionId } from '../types/layout'
import { getToolsForView, getToolComponent, type ToolContext } from '../config/toolRegistry'
import type { ChatProfileDto } from '../api/chatApi'
import { conversationPanelStore } from '../store/conversationPanel'
import { ConversationSessions } from './tools-panel/ConversationSessions'

export interface ToolsPanelProps {
  expanded: boolean
  onToggle: () => void
  sectionId: SectionId | null
  subId: string
  runSelected: boolean
  tenantId?: string
  /** Owning-store slice for the current section. Tools use this only. */
  storeContext?: Record<string, unknown>
  /** Called when user clicks "New chat" in Conversation (chat section only). */
  onNewChat?: () => void
  /** From GET /api/ui/context (used for layout; queue/pipeline live in ChatView). */
  chatProfiles?: ChatProfileDto[]
}

export function ToolsPanel({
  expanded,
  onToggle,
  sectionId,
  subId,
  runSelected,
  tenantId = '',
  storeContext = {},
  onNewChat,
  chatProfiles: _chatProfiles = [],
}: ToolsPanelProps) {
  const isChatView = sectionId === 'chat'
  const effectiveTenantId = tenantId?.trim() ?? ''

  useEffect(() => {
    if (!isChatView || !effectiveTenantId) {
      conversationPanelStore.getState().setSelectedQueueId('')
      conversationPanelStore.getState().setSelectedPipelineId('')
      conversationPanelStore.getState().setSelectedProfileId('')
    }
  }, [isChatView, effectiveTenantId])

  const tools = getToolsForView(sectionId, subId, runSelected)
  const context: ToolContext = {
    sectionId,
    subId,
    runSelected,
    storeContext,
  }

  return (
    <aside className={`tools-panel side-panel ${expanded ? 'expanded' : 'collapsed'}`}>
      {expanded && (
        <div className="side-panel-inner">
          <div className="side-panel-title">{isChatView ? 'Conversation' : 'Tools'}</div>
          {sectionId === 'chat' && isChatView && (
            <ConversationSessions tenantId={effectiveTenantId} onNewChat={onNewChat} />
          )}
          {tools.length > 0 && (
            <ul className="tools-list">
              {tools.map((t) => {
                const ToolComponent = getToolComponent(t.id)
                return (
                  <li key={t.id} className="tools-list-item">
                    {ToolComponent ? (
                      <ToolComponent context={context} />
                    ) : (
                      <>
                        <span className="tools-list-label">{t.label}</span>
                        {t.description && <span className="tools-list-desc">{t.description}</span>}
                      </>
                    )}
                  </li>
                )
              })}
            </ul>
          )}
        </div>
      )}
      <button
        type="button"
        className="side-panel-toggle"
        onClick={onToggle}
        title={expanded ? 'Collapse' : 'Expand'}
        aria-label={expanded ? 'Collapse tools' : 'Expand tools'}
      >
        {expanded ? '<' : <span className="side-panel-collapsed-label">Conversation</span>}
      </button>
    </aside>
  )
}
