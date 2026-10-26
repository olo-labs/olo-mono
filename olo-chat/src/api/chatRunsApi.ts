/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { CHAT_API, withAuth } from './chatApiClient'
import type { HumanInputRequestDto, RunResponseDto, RunStatusDto } from './chatTypes'

export async function getRun(runId: string): Promise<RunStatusDto | null> {
  try {
    const res = await fetch(`${CHAT_API}/runs/${encodeURIComponent(runId)}`, { headers: withAuth() })
    if (!res.ok) return null
    const data = await res.json()
    return { runId: data.runId ?? runId, status: data.status ?? 'running' }
  } catch {
    return null
  }
}

export async function getRunResponse(runId: string): Promise<RunResponseDto | null> {
  try {
    const res = await fetch(`${CHAT_API}/runs/${encodeURIComponent(runId)}/response`, { headers: withAuth() })
    if (!res.ok) return null
    const data = await res.json()
    return { runId: data.runId ?? runId, response: data.response ?? '' }
  } catch {
    return null
  }
}

export async function submitHumanInput(runId: string, body: HumanInputRequestDto): Promise<void> {
  const res = await fetch(`${CHAT_API}/runs/${encodeURIComponent(runId)}/human-input`, {
    method: 'POST',
    headers: withAuth({ 'Content-Type': 'application/json' }),
    body: JSON.stringify({
      approved: !!body.approved,
      message: body.message ?? '',
      ...(body.historyText != null && body.historyText !== ''
        ? { historyText: body.historyText }
        : {}),
    }),
  })
  if (!res.ok) throw new Error(`Human input failed: ${res.status}`)
}

export async function cancelRun(runId: string): Promise<void> {
  const res = await fetch(`${CHAT_API}/runs/${encodeURIComponent(runId)}/cancel`, {
    method: 'POST',
    headers: withAuth(),
  })
  if (res.status === 404) throw new Error('Run not found')
  if (res.status === 409) throw new Error('Run is no longer in progress')
  if (!res.ok) throw new Error(`Cancel run failed: ${res.status}`)
}
