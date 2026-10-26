/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { CHAT_API, withAuth } from './chatApiClient'
import type { TenantDto, UiContextDto } from './chatTypes'

export async function getUiContext(): Promise<UiContextDto | null> {
  try {
    const res = await fetch(`${CHAT_API}/ui/context`, { headers: withAuth() })
    if (!res.ok) return null
    return await res.json()
  } catch {
    return null
  }
}

/** Fetches tenant list. Uses GET /api/tenants (proxied to http://localhost:7080/api/tenants). */
export async function getTenants(): Promise<TenantDto[]> {
  try {
    const res = await fetch(`${CHAT_API}/tenants`, { headers: withAuth() })
    if (!res.ok) return []
    const data = await res.json()
    return Array.isArray(data) ? data : []
  } catch {
    return []
  }
}

/** Health check for olo backend (plain "OK"). */
export async function getChatBackendHealth(): Promise<boolean> {
  try {
    const res = await fetch(`${CHAT_API}/health`)
    return res.ok
  } catch {
    return false
  }
}
