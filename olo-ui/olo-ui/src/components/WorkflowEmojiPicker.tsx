/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { useEffect, useId, useRef, useState } from 'react'
import EmojiPicker, { Theme, type EmojiClickData } from 'emoji-picker-react'
import { normalizeWorkflowEmoji } from '../lib/workflowEmoji'

export interface WorkflowEmojiPickerProps {
  value?: string
  onChange: (emoji: string | undefined) => void
}

export function WorkflowEmojiPicker({ value, onChange }: WorkflowEmojiPickerProps) {
  const [open, setOpen] = useState(false)
  const pickerId = useId()
  const rootRef = useRef<HTMLDivElement>(null)
  const selected = normalizeWorkflowEmoji(value) ?? ''

  useEffect(() => {
    if (!open) return

    const handlePointerDown = (event: MouseEvent) => {
      if (!rootRef.current?.contains(event.target as Node)) {
        setOpen(false)
      }
    }

    document.addEventListener('mousedown', handlePointerDown)
    return () => document.removeEventListener('mousedown', handlePointerDown)
  }, [open])

  const handleEmojiClick = (emojiData: EmojiClickData) => {
    onChange(normalizeWorkflowEmoji(emojiData.emoji))
    setOpen(false)
  }

  return (
    <div className="workflow-emoji-picker" ref={rootRef}>
      <div className="workflow-emoji-picker-row">
        <span className="workflow-emoji-picker-preview" aria-hidden>
          {selected || '—'}
        </span>
        <button
          type="button"
          className="tenant-config-btn"
          aria-expanded={open}
          aria-controls={pickerId}
          onClick={() => setOpen((prev) => !prev)}
        >
          {open ? 'Close' : 'Choose emoji'}
        </button>
        {selected ? (
          <button
            type="button"
            className="tenant-config-btn"
            onClick={() => {
              onChange(undefined)
              setOpen(false)
            }}
          >
            Clear
          </button>
        ) : null}
      </div>
      {open ? (
        <div id={pickerId} className="workflow-emoji-picker-panel">
          <EmojiPicker
            onEmojiClick={handleEmojiClick}
            width="100%"
            height={380}
            lazyLoadEmojis
            searchPlaceholder="Search all emojis"
            previewConfig={{ showPreview: true }}
            theme={Theme.AUTO}
          />
        </div>
      ) : null}
    </div>
  )
}
