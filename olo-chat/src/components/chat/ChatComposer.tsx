/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import type { ChatProfileDto } from '../../api/chatApi'
import { formatProfileOptionLabel } from '../../lib/chatProfileUi'
import { useChatRagSources } from '../../hooks/useChatRagSources'

export interface ChatComposerProps {
  chatProfiles: ChatProfileDto[]
  selectedProfileId: string
  selectedProfile: ChatProfileDto | null
  input: string
  setInput: (v: string) => void
  sending: boolean
  sessionId: string | null
  selectedRagSource: string
  onRagSourceChange: (source: string) => void
  onProfileChange: (id: string, prof: ChatProfileDto) => void
  onSend: () => void
}

export function ChatComposer({
  chatProfiles,
  selectedProfileId,
  selectedProfile,
  input,
  setInput,
  sending,
  sessionId,
  selectedRagSource,
  onRagSourceChange,
  onProfileChange,
  onSend,
}: ChatComposerProps) {
  const { sources: ragSources } = useChatRagSources()

  return (
    <div className="chat-view-composer">
      <form
        className="chat-view-input-bar"
        onSubmit={(e) => {
          e.preventDefault()
          onSend()
        }}
      >
        <div className="chat-view-profile-group">
          <select
            id="chat-profile-select"
            className="chat-view-profile-select chat-view-profile-select--pill"
            value={selectedProfileId || chatProfiles[0]?.id || ''}
            onChange={(e) => {
              const id = e.target.value
              const prof = chatProfiles.find((p) => p.id === id)
              if (prof) onProfileChange(id, prof)
            }}
            aria-label="Chat preset (queue and pipeline)"
          >
            {chatProfiles.map((p, i) => (
              <option key={p.id} value={p.id} title={p.displaySummary?.trim() ? p.displaySummary : undefined}>
                {formatProfileOptionLabel(p, i)}
              </option>
            ))}
          </select>
          <select
            id="chat-rag-source-select"
            className="chat-view-profile-select chat-view-profile-select--pill chat-view-rag-select"
            value={selectedRagSource}
            onChange={(e) => onRagSourceChange(e.target.value)}
            disabled={sending || !sessionId}
            aria-label="Knowledge source (RAG)"
            title="Optional indexed knowledge source for grounded answers"
          >
            <option value="">No RAG</option>
            {ragSources.map((source) => (
              <option key={source} value={source}>
                📚 {source}
              </option>
            ))}
          </select>
        </div>
        <input
          className="chat-view-input"
          placeholder={
            selectedProfile ? `Ask ${selectedProfile.displayName || 'assistant'}…` : 'Type a message…'
          }
          value={input}
          onChange={(e) => setInput(e.target.value)}
          disabled={sending || !sessionId}
          aria-label="Message"
        />
        <button
          type="submit"
          className="chat-view-send"
          disabled={sending || !sessionId || !input.trim()}
        >
          {sending ? '…' : 'Send'}
        </button>
      </form>
    </div>
  )
}
