/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import type { ChatProfileDto } from '../../api/chatApi'
import { emojiForProfile } from '../../lib/chatProfileUi'

export function ChatWaitingBanner({
  selectedProfile,
  selectedProfileIndex,
  onCancel,
  cancelling = false,
}: {
  selectedProfile: ChatProfileDto | null
  selectedProfileIndex: number
  onCancel?: () => void
  cancelling?: boolean
}) {
  return (
    <div className="chat-view-waiting" role="status" aria-live="polite">
      <div className="chat-view-waiting-body">
        {selectedProfile ? (
          <div className="chat-view-waiting-profile">
            <div className="chat-view-waiting-line1">
              <span className="chat-view-waiting-emoji" aria-hidden>
                {emojiForProfile(selectedProfile, selectedProfileIndex)}
              </span>
              <span className="chat-view-waiting-text">
                <strong>{selectedProfile.displayName || selectedProfile.id}</strong>{' '}
                {cancelling ? 'is stopping…' : 'is thinking…'}
              </span>
            </div>
            {selectedProfile.displaySummary?.trim() ? (
              <p className="chat-view-waiting-summary">{selectedProfile.displaySummary.trim()}</p>
            ) : null}
          </div>
        ) : (
          <span>{cancelling ? 'Stopping run…' : 'Waiting for response…'}</span>
        )}
      </div>
      {onCancel ? (
        <button
          type="button"
          className="chat-view-cancel-run"
          onClick={() => void onCancel()}
          disabled={cancelling}
        >
          {cancelling ? 'Cancelling…' : 'Cancel'}
        </button>
      ) : null}
    </div>
  )
}
