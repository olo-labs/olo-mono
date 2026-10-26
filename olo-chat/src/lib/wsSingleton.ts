/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Single shared WebSocket connection for the app. Ensures only one live connection;
 * reconnects on close. Access token is passed in URL for backend validation.
 */
import { getWebSocketUrl } from './wsUrl'

let socket: WebSocket | null = null
let currentUrl: string | null = null

export function getSharedWebSocket(accessToken: string | null | undefined): WebSocket | null {
  const url = getWebSocketUrl(accessToken)
  if (!url) return null
  if (socket?.readyState === WebSocket.OPEN && url === currentUrl) return socket
  if (socket) {
    if (socket.readyState === WebSocket.CONNECTING) {
      socket = null
      currentUrl = null
    } else {
      try {
        socket.close()
      } catch {
        // ignore
      }
      socket = null
      currentUrl = null
    }
  }
  currentUrl = url
  socket = new WebSocket(url)
  return socket
}

export function closeSharedWebSocket(): void {
  if (socket) {
    try {
      socket.close()
    } catch {
      // ignore
    }
    socket = null
  }
  currentUrl = null
}

export function getCurrentSocket(): WebSocket | null {
  return socket?.readyState === WebSocket.OPEN ? socket : null
}

/** Subscribe to run events over the shared WebSocket. Call when you have a runId (e.g. after sendMessage). Returns true if message was sent. */
export function subscribeToRun(runId: string): boolean {
  const s = getCurrentSocket()
  if (!s || !runId?.trim()) return false
  try {
    s.send(JSON.stringify({ type: 'SUBSCRIBE_RUN', runId: runId.trim() }))
    return true
  } catch {
    return false
  }
}
