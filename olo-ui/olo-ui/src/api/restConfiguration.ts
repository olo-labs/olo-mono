/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { GraphLogSummary } from '../types/graphLog'
import type { StudioCatalog } from '../types/catalog'
import type { ModelProvider, WorkflowDocument, WorkflowSummary } from '../types/workflow'
import { API_BASE, encodeWorkflowPath } from './restClient'

export interface ModelProviderTestResult {
  ok: boolean
  message: string
  latencyMs?: number
  model?: string
}

export async function getCatalog(): Promise<StudioCatalog> {
  const res = await fetch(`${API_BASE}/catalog`)
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.json()
}

export async function listWorkflowConfigurations(): Promise<WorkflowSummary[]> {
  const res = await fetch(`${API_BASE}/configuration/workflows`)
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.json()
}

export async function getWorkflowConfiguration(fileName: string): Promise<WorkflowDocument> {
  const res = await fetch(`${API_BASE}/configuration/workflows/${encodeWorkflowPath(fileName)}`)
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.json()
}

export async function saveWorkflowConfiguration(
  fileName: string,
  document: WorkflowDocument,
): Promise<WorkflowSummary> {
  const res = await fetch(`${API_BASE}/configuration/workflows/${encodeWorkflowPath(fileName)}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(document),
  })
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.json()
}

export async function deleteWorkflowConfiguration(fileName: string): Promise<void> {
  const res = await fetch(`${API_BASE}/configuration/workflows/${encodeWorkflowPath(fileName)}`, {
    method: 'DELETE',
  })
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
}

export async function testModelProvider(
  provider: Pick<ModelProvider, 'provider' | 'model' | 'configuration'>,
): Promise<ModelProviderTestResult> {
  const res = await fetch(`${API_BASE}/model-providers/test`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(provider),
  })
  if (res.status === 404) {
    return {
      ok: false,
      message: 'Test API not found — rebuild and restart olo-be (port 8082).',
    }
  }
  const data = (await res.json()) as ModelProviderTestResult & { message?: string }
  if (!res.ok) {
    return {
      ok: false,
      message: data.message ?? `HTTP ${res.status}`,
    }
  }
  return data
}

export async function getConfigurationRoot(): Promise<string> {
  const res = await fetch(`${API_BASE}/configuration/workflows/meta/root`)
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  const data = (await res.json()) as { directory?: string }
  return data.directory ?? ''
}

export async function listGraphLogs(): Promise<GraphLogSummary[]> {
  const res = await fetch(`${API_BASE}/configuration/logs`)
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.json()
}

export async function getGraphLogRoot(): Promise<string> {
  const res = await fetch(`${API_BASE}/configuration/logs/meta/root`)
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  const data = (await res.json()) as { directory?: string }
  return data.directory ?? ''
}

export async function getGraphLog(fileName: string): Promise<WorkflowDocument> {
  const res = await fetch(`${API_BASE}/configuration/logs/${encodeWorkflowPath(fileName)}`)
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.json()
}
