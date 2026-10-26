/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { Tenant } from '../types/tenant'
import { API_BASE } from './restClient'

export type { Tenant } from '../types/tenant'

/** Display label: name if non-empty, else id (UUID). Use from lib/tenantDisplay in components. */
export function tenantDisplayName(t: Tenant): string {
  return (t.name != null && t.name.trim() !== '') ? t.name.trim() : t.id
}

/** Tenants from Redis key olo:tenants (JSON array of { id, name }) */
export async function getTenants(): Promise<Tenant[]> {
  const url = `${API_BASE}/tenants`
  console.log('[olo-ui] getTenants: fetching', url)
  const res = await fetch(url)
  console.log('[olo-ui] getTenants: response status=', res.status, res.statusText)
  if (!res.ok) {
    console.error('[olo-ui] getTenants: request failed', res.status, await res.text())
    throw new Error(`HTTP ${res.status}`)
  }
  const data = await res.json()
  console.log('[olo-ui] getTenants: received', Array.isArray(data) ? data.length : 'non-array', data)
  return Array.isArray(data) ? data : []
}

export async function saveTenant(tenant: Tenant): Promise<Tenant> {
  const res = await fetch(`${API_BASE}/tenants`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(tenant),
  })
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.json()
}

export async function updateTenant(id: string, tenant: Partial<Tenant>): Promise<Tenant> {
  const res = await fetch(`${API_BASE}/tenants/${encodeURIComponent(id)}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ ...tenant, id }),
  })
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.json()
}

export async function deleteTenant(id: string): Promise<void> {
  const res = await fetch(`${API_BASE}/tenants/${encodeURIComponent(id)}`, { method: 'DELETE' })
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
}

export async function getEnvironments(tenantId: string): Promise<string[]> {
  const res = await fetch(`${API_BASE}/tenants/${encodeURIComponent(tenantId)}/environments`)
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.json()
}

export async function getRunIds(tenantId: string, environment: string): Promise<string[]> {
  const res = await fetch(
    `${API_BASE}/tenants/${encodeURIComponent(tenantId)}/environments/${encodeURIComponent(environment)}/runs`,
  )
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.json()
}

export interface DropdownDetails {
  tenants: Tenant[]
  environments: string[]
  runIds: string[]
}

export async function getDropdownDetails(
  tenantId?: string,
  environment?: string,
): Promise<DropdownDetails> {
  const params = new URLSearchParams()
  if (tenantId) params.set('tenantId', tenantId)
  if (environment) params.set('environment', environment)
  const q = params.toString()
  const res = await fetch(`${API_BASE}/dropdown-details${q ? `?${q}` : ''}`)
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.json()
}
