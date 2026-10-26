/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

/** Worker progress panel below composer: expanded + height persist across refresh (same idea as panel widths in `ui` store). */
export const CHAT_PROGRESS_EXPANDED_KEY = 'olo:chat-progress-expanded'
export const CHAT_PROGRESS_HEIGHT_KEY = 'olo:chat-progress-height'
export const CHAT_PROGRESS_HEIGHT_MIN = 120
export const CHAT_PROGRESS_HEIGHT_MAX = 600
export const CHAT_PROGRESS_HEIGHT_DEFAULT = 220

export function readStoredProgressExpanded(): boolean {
  if (typeof window === 'undefined') return false
  try {
    return window.localStorage.getItem(CHAT_PROGRESS_EXPANDED_KEY) === '1'
  } catch {
    return false
  }
}

export function readStoredProgressHeight(): number {
  if (typeof window === 'undefined') return CHAT_PROGRESS_HEIGHT_DEFAULT
  try {
    const raw = window.localStorage.getItem(CHAT_PROGRESS_HEIGHT_KEY)
    const n = raw != null ? Number(raw) : NaN
    if (Number.isFinite(n) && n >= CHAT_PROGRESS_HEIGHT_MIN && n <= CHAT_PROGRESS_HEIGHT_MAX) {
      return Math.round(n)
    }
  } catch {
    /* ignore */
  }
  return CHAT_PROGRESS_HEIGHT_DEFAULT
}

export function profileByRunStorageKey(sessionId: string): string {
  return `olo:chat-run-profiles:${sessionId}`
}
