/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { CHAT_API, withAuth } from './chatApiClient'
import type { RunEventDto } from './chatTypes'

const SSE_DEBUG = true // set false to disable run-events step logging

function sseLog(step: string, detail?: unknown) {
  if (SSE_DEBUG) console.log('[Chat SSE]', step, detail !== undefined ? detail : '')
}

/**
 * Subscribe to run events via SSE. Calls onEvent for each event (catch-up then live).
 * Returns an abort function to close the stream.
 */
export function streamRunEvents(
  runId: string,
  onEvent: (event: RunEventDto) => void,
  onError?: (err: unknown) => void
): () => void {
  const ac = new AbortController()
  const url = `${CHAT_API}/runs/${encodeURIComponent(runId)}/events`
  sseLog('1. SSE fetch start', { runId, url })
  fetch(url, { signal: ac.signal, headers: withAuth({ Accept: 'text/event-stream' }) })
    .then(async (res) => {
      sseLog('2. SSE response received', { ok: res.ok, status: res.status, hasBody: !!res.body })
      if (!res.ok || !res.body) {
        sseLog('2b. SSE failed (no body or !ok)', res.status)
        onError?.(new Error(`SSE failed: ${res.status}`))
        return
      }
      const reader = res.body.getReader()
      const decoder = new TextDecoder()
      let buf = ''
      let eventCount = 0
      try {
        while (true) {
          const { done, value } = await reader.read()
          sseLog('3. SSE read chunk', { done, chunkLen: value?.length ?? 0 })
          if (done) {
            sseLog('4. SSE stream done (connection closed)')
            break
          }
          buf += decoder.decode(value, { stream: true })
          while (/^\s*:/.test(buf)) {
            const nl = buf.indexOf('\n')
            if (nl === -1) break
            buf = buf.slice(nl + 1)
          }
          while (buf.includes('data: ')) {
            const dataIdx = buf.indexOf('data: ')
            const payloadStart = dataIdx + 6
            const firstBrace = buf.indexOf('{', payloadStart)
            if (firstBrace === -1) break
            let depth = 0
            let inString = false
            let stringChar = ''
            let escape = false
            let end = -1
            for (let i = firstBrace; i < buf.length; i++) {
              const c = buf[i]
              if (escape) {
                escape = false
                continue
              }
              if (c === '\\' && inString) {
                escape = true
                continue
              }
              if (inString) {
                if (c === stringChar) inString = false
                continue
              }
              if (c === '"' || c === "'") {
                inString = true
                stringChar = c
                continue
              }
              if (c === '{') depth++
              else if (c === '}') {
                depth--
                if (depth === 0) {
                  end = i + 1
                  break
                }
              }
            }
            if (end === -1) break
            const dataLine = buf.slice(firstBrace, end)
            buf = buf.slice(end).replace(/^\s*\n?/, '')
            if (dataLine === '[DONE]') continue
            try {
              const event = JSON.parse(dataLine) as RunEventDto
              eventCount++
              sseLog(`5. SSE event #${eventCount}`, {
                nodeType: event.nodeType,
                status: event.status,
                hasOutput: event.output != null,
                outputKeys: event.output && typeof event.output === 'object' ? Object.keys(event.output as object) : null,
              })
              onEvent(event)
            } catch (parseErr) {
              sseLog('5b. SSE parse error', parseErr)
            }
          }
        }
      } finally {
        reader.releaseLock()
        sseLog('6. SSE reader released', { totalEvents: eventCount })
      }
    })
    .catch((err) => {
      sseLog('7. SSE fetch/stream catch', { name: err?.name, message: err?.message })
      if (err?.name !== 'AbortError') onError?.(err)
    })
  return () => {
    sseLog('8. SSE abort() called')
    ac.abort()
  }
}
