/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Per-session display info: custom title (user-edited) and first message preview (auto from first user message).
 * Persisted in localStorage so session list stays usable with many sessions.
 * Capped at MAX_ENTRIES; oldest-by-use entries are dropped when over limit.
 */

import { create } from 'zustand'
import { persist } from 'zustand/middleware'

const STORAGE_KEY = 'olo:session-display'
const MAX_ENTRIES = 80
const FIRST_MESSAGE_MAX_LEN = 300

export interface SessionDisplayEntry {
  customTitle?: string
  firstMessagePreview?: string
  lastTouched: number
}

function trimToMaxLen(s: string, max: number): string {
  if (s.length <= max) return s
  return s.slice(0, max).trim() + '…'
}

function evictIfNeeded(state: Record<string, SessionDisplayEntry> | undefined): Record<string, SessionDisplayEntry> {
  const entries = state ?? {}
  const keys = Object.keys(entries)
  if (keys.length <= MAX_ENTRIES) return entries
  const sorted = keys
    .map((id) => ({ id, lastTouched: entries[id]?.lastTouched ?? 0 }))
    .sort((a, b) => a.lastTouched - b.lastTouched)
  const toRemove = sorted.slice(0, keys.length - MAX_ENTRIES).map((x) => x.id)
  const next = { ...entries }
  toRemove.forEach((id) => delete next[id])
  return next
}

export interface SessionDisplayState {
  /** sessionId -> display entry */
  entries: Record<string, SessionDisplayEntry>
  getEntry: (sessionId: string) => SessionDisplayEntry | undefined
  setCustomTitle: (sessionId: string, title: string) => void
  setFirstMessagePreview: (sessionId: string, content: string) => void
  removeSession: (sessionId: string) => void
  /** Remove multiple (e.g. after delete all) */
  removeSessions: (sessionIds: string[]) => void
}

export const sessionDisplayStore = create<SessionDisplayState>()(
  persist(
    (set, get) => ({
      entries: {},

      getEntry: (sessionId: string) => get().entries?.[sessionId],

      setCustomTitle: (sessionId: string, title: string) => {
        set((state) => {
          const next = { ...(state.entries ?? {}) }
          const prev = next[sessionId] ?? { lastTouched: 0 }
          next[sessionId] = {
            ...prev,
            customTitle: title.trim() || undefined,
            lastTouched: Date.now(),
          }
          return { entries: evictIfNeeded(next) }
        })
      },

      setFirstMessagePreview: (sessionId: string, content: string) => {
        const preview = trimToMaxLen(content.trim(), FIRST_MESSAGE_MAX_LEN)
        if (!preview) return
        set((state) => {
          const next = { ...(state.entries ?? {}) }
          const prev = next[sessionId] ?? { lastTouched: 0 }
          next[sessionId] = {
            ...prev,
            firstMessagePreview: preview,
            lastTouched: Date.now(),
          }
          return { entries: evictIfNeeded(next) }
        })
      },

      removeSession: (sessionId: string) => {
        set((state) => {
          const next = { ...(state.entries ?? {}) }
          delete next[sessionId]
          return { entries: next }
        })
      },

      removeSessions: (sessionIds: string[]) => {
        if (sessionIds.length === 0) return
        set((state) => {
          const next = { ...(state.entries ?? {}) }
          sessionIds.forEach((id) => delete next[id])
          return { entries: next }
        })
      },
    }),
    { name: STORAGE_KEY }
  )
)

/** Truncate for single-line label (e.g. 48 chars). */
export function truncateLabel(text: string, maxLen: number = 48): string {
  const t = text.trim()
  if (t.length <= maxLen) return t
  return t.slice(0, maxLen).trim() + '…'
}
