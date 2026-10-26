/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { API_BASE } from './restClient'

export interface HealthResponse {
  status: string
  service: string
}

export async function getHealth(): Promise<HealthResponse> {
  const res = await fetch(`${API_BASE}/health`)
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.json()
}

export interface WorkerRefreshResponse {
  key: string
  value: string
}

export interface SystemRefreshResponse {
  ok: boolean
  workerRefreshKey: string
  workerRefreshValue: string | null
  runtimeReloaded: boolean
  runtimeMessage: string | null
  steps: string[]
}

/** @deprecated Prefer {@link refreshOloStack} */
export async function signalWorkerRefresh(): Promise<WorkerRefreshResponse> {
  const res = await fetch(`${API_BASE}/worker/refresh`, { method: 'POST' })
  if (!res.ok) {
    const detail = await res.text()
    throw new Error(detail || `HTTP ${res.status}`)
  }
  return res.json()
}

/** Fallback when olo-be has not been rebuilt with POST /api/v1/system/refresh yet. */
async function refreshOloStackLegacy(): Promise<SystemRefreshResponse> {
  const steps: string[] = []
  let workerOk = false
  let workerValue: string | null = null
  let workerKey = 'olo:worker:refresh'

  try {
    const worker = await signalWorkerRefresh()
    workerOk = true
    workerValue = worker.value
    workerKey = worker.key
    steps.push(`worker: signaled via Redis key ${workerKey}`)
  } catch (e) {
    steps.push(`worker: failed — ${e instanceof Error ? e.message : String(e)}`)
  }

  let runtimeReloaded = false
  let runtimeMessage: string | null = null
  try {
    const runtimeRes = await fetch('/runtime-api/admin/configuration/reload', { method: 'POST' })
    if (runtimeRes.ok) {
      const body = (await runtimeRes.json()) as {
        ok?: boolean
        workflowCount?: number
        message?: string
      }
      runtimeReloaded = body.ok === true
      runtimeMessage = body.message ?? null
      if (runtimeReloaded) {
        steps.push(`runtime: reloaded ${body.workflowCount ?? 0} workflow(s)`)
      } else {
        steps.push(`runtime: not reloaded (${runtimeMessage ?? 'unknown'})`)
      }
    } else {
      runtimeMessage = await runtimeRes.text()
      steps.push(`runtime: HTTP ${runtimeRes.status} — restart olo backend (7080) if needed`)
    }
  } catch (e) {
    runtimeMessage = e instanceof Error ? e.message : String(e)
    steps.push(`runtime: not reloaded (${runtimeMessage})`)
  }

  steps.push('studio: reload catalog and workflows from disk in the browser')
  return {
    ok: workerOk,
    workerRefreshKey: workerKey,
    workerRefreshValue: workerValue,
    runtimeReloaded,
    runtimeMessage,
    steps,
  }
}

/** Full-stack refresh: worker (Redis), olo runtime config, then reload studio state in the browser. */
export async function refreshOloStack(): Promise<SystemRefreshResponse> {
  const res = await fetch(`${API_BASE}/system/refresh`, { method: 'POST' })
  if (res.status === 404) {
    return refreshOloStackLegacy()
  }
  if (!res.ok) {
    const detail = await res.text()
    throw new Error(detail || `HTTP ${res.status}`)
  }
  return res.json()
}
