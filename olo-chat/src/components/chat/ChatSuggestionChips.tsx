/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { COMMON_MESSAGES } from '../../lib/chatConstants'

export function ChatSuggestionChips({
  disabled,
  onSelect,
}: {
  disabled: boolean
  onSelect: (msg: string) => void
}) {
  return (
    <div className="chat-view-suggestions">
      {COMMON_MESSAGES.map((msg) => (
        <button
          key={msg}
          type="button"
          className="chat-view-suggestion-chip"
          onClick={() => onSelect(msg)}
          disabled={disabled}
        >
          {msg}
        </button>
      ))}
    </div>
  )
}
