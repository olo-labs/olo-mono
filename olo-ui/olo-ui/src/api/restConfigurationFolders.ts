/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { SystemRefreshResponse } from './restHealth'
import { API_BASE } from './restClient'

export interface ConfigurationFolderSummary {
  id: string
  active: boolean
}

export interface ConfigurationFolderListResponse {
  catalogRoot: string
  activeDirectory: string
  activeFolderId: string
  folders: ConfigurationFolderSummary[]
}

export interface ConfigurationFolderActivateResponse {
  folderId: string
  activeDirectory: string
  refresh: SystemRefreshResponse
}

export async function listConfigurationFolders(): Promise<ConfigurationFolderListResponse> {
  const res = await fetch(`${API_BASE}/configuration/folders`)
  if (!res.ok) {
    const detail = await res.text()
    throw new Error(detail || `HTTP ${res.status}`)
  }
  return res.json()
}

export async function activateConfigurationFolder(
  folderId: string,
): Promise<ConfigurationFolderActivateResponse> {
  const res = await fetch(`${API_BASE}/configuration/folders/${encodeURIComponent(folderId)}/activate`, {
    method: 'POST',
  })
  if (!res.ok) {
    const detail = await res.text()
    throw new Error(detail || `HTTP ${res.status}`)
  }
  return res.json()
}
