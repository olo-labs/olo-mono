/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { create } from 'zustand'
import {
  activateConfigurationFolder,
  listConfigurationFolders,
  type ConfigurationFolderListResponse,
  type ConfigurationFolderSummary,
} from '../api/restConfigurationFolders'
import { refreshOloStack } from '../api/restHealth'
import { catalogStore } from './catalogStore'
import { workflowConfigurationStore } from './workflowConfigurationStore'

export interface ConfigurationFolderState {
  catalogRoot: string
  activeDirectory: string
  activeFolderId: string
  folders: ConfigurationFolderSummary[]
  loading: boolean
  activatingFolderId: string | null
  error: string | null
  lastRefreshSteps: string[]

  loadFolders: () => Promise<void>
  activateFolder: (folderId: string) => Promise<void>
}

export const configurationFolderStore = create<ConfigurationFolderState>((set, get) => ({
  catalogRoot: '',
  activeDirectory: '',
  activeFolderId: '',
  folders: [],
  loading: false,
  activatingFolderId: null,
  error: null,
  lastRefreshSteps: [],

  loadFolders: async () => {
    set({ loading: true, error: null })
    try {
      const response: ConfigurationFolderListResponse = await listConfigurationFolders()
      set({
        catalogRoot: response.catalogRoot,
        activeDirectory: response.activeDirectory,
        activeFolderId: response.activeFolderId,
        folders: response.folders,
        loading: false,
      })
    } catch (e) {
      set({
        loading: false,
        error: e instanceof Error ? e.message : 'Failed to load configuration folders',
      })
    }
  },

  activateFolder: async (folderId: string) => {
    if (get().activatingFolderId) return
    set({ activatingFolderId: folderId, error: null, lastRefreshSteps: [] })
    try {
      const response = await activateConfigurationFolder(folderId)
      set({
        activeFolderId: response.folderId,
        activeDirectory: response.activeDirectory,
        folders: get().folders.map((folder) => ({
          ...folder,
          active: folder.id === response.folderId,
        })),
        lastRefreshSteps: response.refresh.steps,
      })

      await Promise.all([
        catalogStore.getState().loadCatalog(),
        workflowConfigurationStore.getState().reloadFromDisk(),
      ])

      if (!response.refresh.ok) {
        set({
          error: `Activated ${response.folderId}, but stack refresh was incomplete. See refresh steps.`,
        })
      }
    } catch (e) {
      set({
        error: e instanceof Error ? e.message : 'Failed to activate configuration folder',
      })
    } finally {
      set({ activatingFolderId: null })
    }
  },
}))

/** Manual full-stack refresh from the scenarios screen. */
export async function refreshConfigurationStack(): Promise<void> {
  const result = await refreshOloStack()
  configurationFolderStore.setState({ lastRefreshSteps: result.steps })
  await Promise.all([
    catalogStore.getState().loadCatalog(),
    workflowConfigurationStore.getState().reloadFromDisk(),
    configurationFolderStore.getState().loadFolders(),
  ])
  if (!result.ok) {
    throw new Error(result.steps.join('\n'))
  }
}
