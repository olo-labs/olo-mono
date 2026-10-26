/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * WebSocket URL for the olo backend (/ws).
 * Uses VITE_API_BASE when set; otherwise same-origin /ws (Vite or nginx proxy to the backend container).
 */
export function getWebSocketUrl(accessToken?: string | null): string | null {
  const base = import.meta.env.VITE_API_BASE
  let wsBase: string
  if (base && typeof base === 'string' && base.trim()) {
    wsBase = base.trim().replace(/^http/, 'ws').replace(/\/$/, '')
  } else if (typeof window !== 'undefined' && window.location?.host) {
    const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    wsBase = `${proto}//${window.location.host}`
  } else {
    return null
  }
  let url = `${wsBase}/ws`
  if (accessToken && accessToken.trim()) {
    url += (url.includes('?') ? '&' : '?') + 'accessToken=' + encodeURIComponent(accessToken.trim())
  }
  return url
}

/** Reads access token for WebSocket: sessionStorage.accessToken or VITE_WS_ACCESS_TOKEN. Set token before connecting (e.g. after login). */
export function getWsAccessToken(): string | null {
  try {
    const fromStorage = typeof sessionStorage !== 'undefined' ? sessionStorage.getItem('accessToken') : null
    if (fromStorage) return fromStorage
    const fromEnv = import.meta.env.VITE_WS_ACCESS_TOKEN
    return typeof fromEnv === 'string' && fromEnv ? fromEnv : null
  } catch {
    return null
  }
}

/** Authorization header for REST calls so the backend can resolve tenantId from the JWT (same token as WebSocket). */
export function getApiAuthHeaders(): Record<string, string> {
  const token = getWsAccessToken()
  if (!token) return {}
  return { Authorization: `Bearer ${token}` }
}
