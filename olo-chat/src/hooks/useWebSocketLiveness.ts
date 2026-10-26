/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { useEffect, useRef } from 'react'
import { getWsAccessToken } from '../lib/wsUrl'
import { getSharedWebSocket } from '../lib/wsSingleton'
import { runEventsStore } from '../store/runEvents'
import type { RunEventDto } from '../api/chatApi'

const DEFAULT_PING_INTERVAL_SEC = 10

function getPingIntervalMs(): number {
  const raw = import.meta.env.VITE_WS_PING_INTERVAL_SEC
  if (raw === undefined || raw === '') return DEFAULT_PING_INTERVAL_SEC * 1000
  const n = Number(raw)
  return Number.isFinite(n) && n > 0 ? Math.round(n) * 1000 : DEFAULT_PING_INTERVAL_SEC * 1000
}

function makeLivenessEvent(status: 'ping' | 'pong', sequenceNumber: number): RunEventDto {
  return {
    runId: '__liveness__',
    nodeId: 'liveness',
    parentNodeId: null,
    nodeType: 'liveness',
    status,
    timestamp: Date.now(),
    sequenceNumber,
    metadata: { direction: status === 'ping' ? 'sent' : 'received' },
  }
}

/**
 * Uses the single shared WebSocket (validated with access token). Sends PING at the configured
 * interval and pushes ping/pong events to runEventsStore. Only one live connection for the app.
 * Token: sessionStorage.accessToken or VITE_WS_ACCESS_TOKEN.
 */
export function useWebSocketLiveness() {
  const seqRef = useRef(0)
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null)
  const reconnectRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const mountedRef = useRef(true)

  useEffect(() => {
    mountedRef.current = true
    const token = getWsAccessToken()
    const ws = getSharedWebSocket(token)
    if (!ws) return

    const intervalMs = getPingIntervalMs()

    const startPingInterval = (socket: WebSocket) => {
      const sendPing = () => {
        if (socket.readyState !== WebSocket.OPEN) return
        seqRef.current += 1
        const seq = seqRef.current
        runEventsStore.getState().addEvent(makeLivenessEvent('ping', seq))
        socket.send(JSON.stringify({ type: 'PING' }))
      }
      sendPing()
      intervalRef.current = setInterval(sendPing, intervalMs)
    }

    if (ws.readyState === WebSocket.OPEN) {
      startPingInterval(ws)
      return () => {
        mountedRef.current = false
        if (intervalRef.current) {
          clearInterval(intervalRef.current)
          intervalRef.current = null
        }
      }
    }

    ws.onopen = () => {
      if (!mountedRef.current) return
      startPingInterval(ws)
    }

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        if (data?.type === 'PONG') {
          seqRef.current += 1
          runEventsStore.getState().addEvent(makeLivenessEvent('pong', seqRef.current))
        } else if (data?.type === 'RUN_EVENT' && data?.payload) {
          runEventsStore.getState().addEvent(data.payload)
        } else if (data?.type === 'ERROR') {
          console.warn('[WS] ERROR from server:', data.code, data.message)
        }
      } catch {
        // ignore
      }
    }

    ws.onclose = () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current)
        intervalRef.current = null
      }
      if (!mountedRef.current) return
      reconnectRef.current = setTimeout(() => {
        reconnectRef.current = null
        const w = getSharedWebSocket(getWsAccessToken())
        if (w && w !== ws) {
          w.onopen = () => mountedRef.current && startPingInterval(w)
          w.onmessage = (event) => {
            try {
              const data = JSON.parse(event.data)
              if (data?.type === 'PONG') {
                seqRef.current += 1
                runEventsStore.getState().addEvent(makeLivenessEvent('pong', seqRef.current))
              } else if (data?.type === 'RUN_EVENT' && data?.payload) {
                runEventsStore.getState().addEvent(data.payload)
              } else if (data?.type === 'ERROR') {
                console.warn('[WS] ERROR from server:', data.code, data.message)
              }
            } catch {
              // ignore
            }
          }
          w.onclose = ws.onclose
          w.onerror = ws.onerror
        }
      }, intervalMs)
    }

    ws.onerror = () => {
      ws.close()
    }

    return () => {
      mountedRef.current = false
      if (reconnectRef.current) {
        clearTimeout(reconnectRef.current)
        reconnectRef.current = null
      }
      if (intervalRef.current) {
        clearInterval(intervalRef.current)
        intervalRef.current = null
      }
      // Do not close the shared socket here. React Strict Mode runs effect → cleanup → effect again;
      // closing here would create a second connection on remount. Keep one live connection; the
      // browser closes it on tab close. Use closeSharedWebSocket() only when token changes or app teardown.
    }
  }, [])
}
