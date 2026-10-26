/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

export function ChatDisconnectedState() {
  return (
    <div className="chat-view chat-view-disconnected">
      <p>Connecting to Olo backend…</p>
      <p className="chat-view-hint">Start the olo backend (port 7080) and refresh.</p>
    </div>
  )
}

export function ChatNoProfilesState() {
  return (
    <div className="chat-view chat-view-disconnected">
      <p>No chat profiles configured.</p>
      <p className="chat-view-hint">
        Add workflow JSON files under <code>olo.configuration.dir</code> (e.g.{' '}
        <code>olo-mono/olo-definition/olo-configuration/current-active/*.json</code>) so{' '}
        <code>GET /api/ui/context</code> returns <code>chatProfiles</code>.
      </p>
    </div>
  )
}

export function ChatEmptySessionState({ sessionsCount }: { sessionsCount: number }) {
  return (
    <div className="chat-view-messages">
      <div className="chat-view-placeholder chat-view-placeholder-empty">
        {sessionsCount === 0
          ? 'No conversations yet. Click "New chat" in the Conversation panel to start.'
          : 'Click "New chat" in the Conversation panel to start a new conversation.'}
      </div>
    </div>
  )
}
