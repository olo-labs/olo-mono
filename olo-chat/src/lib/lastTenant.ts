/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Persist last selected tenant for "previous session" default when URL has no tenant.
 * Uses localStorage (cookie could be used instead if needed).
 */
const KEY = 'olo:lastTenantId'

export function getLastTenantId(): string {
  if (typeof window === 'undefined') return ''
  try {
    return window.localStorage.getItem(KEY) ?? ''
  } catch {
    return ''
  }
}

export function setLastTenantId(id: string): void {
  if (typeof window === 'undefined') return
  try {
    if (id) window.localStorage.setItem(KEY, id)
    else window.localStorage.removeItem(KEY)
  } catch {
    // ignore
  }
}
