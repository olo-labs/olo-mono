/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { SECTIONS, type SectionId } from '../types/layout'
import { ChatView } from './ChatView'
import { KnowledgeView } from './KnowledgeView'
import { DocumentsUploadView } from './DocumentsUploadView'
import type { Tenant } from '../types/tenant'
import type { ChatProfileDto } from '../api/chatApi'
import { queueDisplayName } from '../lib/queueDisplayName'

export interface MainContentProps {
  sectionId: SectionId | null
  subId: string
  runSelected: boolean
  runId: string
  onRunIdChange: (id: string) => void
  tenantId?: string
  tenants?: Tenant[]
  tenantsLoading?: boolean
  configSelectedTenant?: Tenant | null
  onSelectTenant?: (tenant: Tenant) => void
  onAddNewTenant?: () => void
  onDeleteTenant?: (id: string) => void
  /** When this increments, ChatView starts a new chat (from Conversation panel button). */
  newChatTrigger?: number
  /** From GET /api/ui/context — presets (queue/pipeline) for ChatView. */
  chatProfiles?: ChatProfileDto[]
  /** GET /api/health; same signal as ToolsPanel offline state. */
  backendReachable?: boolean
}

export function MainContent({
  sectionId,
  subId,
  runSelected: _runSelected,
  runId: _runId,
  onRunIdChange: _onRunIdChange,
  tenantId = '',
  tenants: _tenants = [],
  tenantsLoading: _tenantsLoading = false,
  configSelectedTenant: _configSelectedTenant = null,
  onSelectTenant: _onSelectTenant,
  onAddNewTenant: _onAddNewTenant,
  onDeleteTenant: _onDeleteTenant,
  newChatTrigger = 0,
  chatProfiles = [],
  backendReachable = false,
}: MainContentProps) {
  const section = sectionId ? SECTIONS.find((s) => s.id === sectionId) : null

  if (!section) {
    return (
      <main className="main-content">
        <div className="main-content-placeholder">
          <p>Select a category from the left panel.</p>
        </div>
      </main>
    )
  }

  if (sectionId === 'chat') {
    return (
      <main className="main-content main-content-chat">
        <div className="main-content-header">
          <h1 className="main-content-title">Chat</h1>
          <span className="main-content-subtitle"> → Conversation with Olo AI</span>
        </div>
        <div className="main-content-body main-content-body-chat">
          <ChatView
            tenantId={tenantId || undefined}
            newChatTrigger={newChatTrigger}
            chatProfiles={chatProfiles}
            backendReachable={backendReachable}
          />
        </div>
      </main>
    )
  }

  if (sectionId === 'knowledge') {
    const options = section.subOptions
    const resolvedLabel = options.find((o) => o.id === subId)?.label ?? (subId || section.label)
    return (
      <main className="main-content">
        <div className="main-content-header">
          <h1 className="main-content-title">Knowledge</h1>
          <span className="main-content-subtitle"> → {resolvedLabel}</span>
        </div>
        <div className="main-content-body">
          <KnowledgeView subId={subId} />
        </div>
      </main>
    )
  }

  const options = section.subOptions
  const resolvedLabel = options.find((o) => o.id === subId)?.label ?? (subId || section.label)
  const currentLabel = resolvedLabel?.includes(':') ? queueDisplayName(resolvedLabel) : resolvedLabel

  return (
    <main className="main-content">
      <div className="main-content-header">
        <h1 className="main-content-title">
          {section.label}
          <span className="main-content-subtitle"> → {currentLabel}</span>
        </h1>
      </div>
      <div className="main-content-body">
        {sectionId === 'documents' && subId === 'upload' ? (
          <DocumentsUploadView />
        ) : sectionId === 'documents' ? (
          <div className="main-content-placeholder-inner">
            <p>Upload / Manage Raw Files.</p>
          </div>
        ) : (
          <div className="main-content-placeholder-inner">
            <p>Select a category from the left panel.</p>
          </div>
        )}
      </div>
    </main>
  )
}
