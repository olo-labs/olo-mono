/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { getOloRuntimeApiPrefix } from '../lib/oloRuntimeBase'

const API = getOloRuntimeApiPrefix()

export interface RunEventDto {
  runId: string
  nodeId: string
  parentNodeId?: string | null
  nodeType: string
  status: string
  eventType?: string
  timestamp: number
  sequenceNumber?: number
  input?: Record<string, unknown>
  output?: Record<string, unknown>
  metadata?: Record<string, unknown>
}

export interface HumanInputRequestDto {
  approved: boolean
  message?: string
  historyText?: string
}

export async function checkOloRuntimeHealth(): Promise<void> {
  const res = await fetch(`${API}/health`)
  if (!res.ok) throw new Error(`Olo server health check failed: HTTP ${res.status}`)
  const text = await res.text()
  if (!text.includes('OK')) throw new Error('Olo server health check returned unexpected response')
}

export async function createRuntimeSession(body: {
  tenantId: string
  queueName?: string
  pipelineId?: string
}): Promise<{ sessionId: string }> {
  const res = await fetch(`${API}/sessions`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) throw new Error(`Create session failed: HTTP ${res.status}`)
  return res.json()
}

export async function sendRuntimeMessage(
  sessionId: string,
  content: string,
  options?: { taskQueue?: string },
): Promise<{ messageId: string; runId: string }> {
  const res = await fetch(`${API}/sessions/${encodeURIComponent(sessionId)}/messages`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ content, taskQueue: options?.taskQueue }),
  })
  if (!res.ok) throw new Error(`Start workflow failed: HTTP ${res.status}`)
  return res.json()
}

export async function getRuntimeRun(runId: string): Promise<{ runId: string; status: string } | null> {
  try {
    const res = await fetch(`${API}/runs/${encodeURIComponent(runId)}`)
    if (!res.ok) return null
    const data = await res.json()
    return { runId: data.runId ?? runId, status: data.status ?? 'running' }
  } catch {
    return null
  }
}

export async function cancelRuntimeRun(runId: string): Promise<void> {
  const res = await fetch(`${API}/runs/${encodeURIComponent(runId)}/cancel`, {
    method: 'POST',
  })
  if (res.status === 404) throw new Error('Run not found')
  if (res.status === 409) throw new Error('Run is no longer in progress')
  if (!res.ok) throw new Error(`Cancel run failed: HTTP ${res.status}`)
}

export async function submitRuntimeHumanInput(
  runId: string,
  body: HumanInputRequestDto,
): Promise<void> {
  const res = await fetch(`${API}/runs/${encodeURIComponent(runId)}/human-input`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      approved: !!body.approved,
      message: body.message ?? '',
      ...(body.historyText != null && body.historyText !== ''
        ? { historyText: body.historyText }
        : {}),
    }),
  })
  if (!res.ok) throw new Error(`Human input failed: HTTP ${res.status}`)
}

export async function getRuntimeRunResponse(runId: string): Promise<string> {
  try {
    const res = await fetch(`${API}/runs/${encodeURIComponent(runId)}/response`)
    if (!res.ok) return ''
    const data = await res.json()
    return typeof data.response === 'string' ? data.response : ''
  } catch {
    return ''
  }
}

export interface RuntimeMessageDto {
  messageId: string
  sessionId: string
  role: string
  content: string
  runId?: string
  createdAt?: number
}

export async function listRuntimeMessages(sessionId: string): Promise<RuntimeMessageDto[]> {
  const res = await fetch(`${API}/sessions/${encodeURIComponent(sessionId)}/messages`)
  if (!res.ok) throw new Error(`List messages failed: HTTP ${res.status}`)
  const data = await res.json()
  return Array.isArray(data) ? (data as RuntimeMessageDto[]) : []
}

/** Poll response endpoint — worker callback may land slightly after run status flips to completed. */
export async function fetchRunResponseWithRetry(
  runId: string,
  attempts = 6,
  delayMs = 500,
): Promise<string> {
  for (let i = 0; i < attempts; i++) {
    const text = (await getRuntimeRunResponse(runId)).trim()
    if (text) return text
    if (i < attempts - 1) {
      await new Promise((resolve) => window.setTimeout(resolve, delayMs))
    }
  }
  return ''
}

export function streamRuntimeRunEvents(
  runId: string,
  onEvent: (event: RunEventDto) => void,
  onError?: (err: unknown) => void,
): () => void {
  const ac = new AbortController()
  const url = `${API}/runs/${encodeURIComponent(runId)}/events`
  fetch(url, { signal: ac.signal, headers: { Accept: 'text/event-stream' } })
    .then(async (res) => {
      if (!res.ok || !res.body) {
        onError?.(new Error(`SSE failed: ${res.status}`))
        return
      }
      const reader = res.body.getReader()
      const decoder = new TextDecoder()
      let buf = ''
      try {
        while (true) {
          const { done, value } = await reader.read()
          if (done) break
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
              onEvent(JSON.parse(dataLine) as RunEventDto)
            } catch {
              // skip malformed chunk
            }
          }
        }
      } finally {
        reader.releaseLock()
      }
    })
    .catch((err) => {
      if (err?.name !== 'AbortError') onError?.(err)
    })
  return () => ac.abort()
}
