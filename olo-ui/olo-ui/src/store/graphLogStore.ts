/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { create } from 'zustand'
import * as api from '../api/rest'
import { normalizeWorkflowDraft } from '../lib/workflowDraftSnapshot'
import type { GraphLogSummary } from '../types/graphLog'
import type { WorkflowDocument } from '../types/workflow'

export interface GraphLogState {
  logs: GraphLogSummary[]
  logRoot: string
  loading: boolean
  error: string | null
  selectedFileName: string | null
  draft: WorkflowDocument | null

  loadLogs: () => Promise<void>
  reloadFromDisk: () => Promise<void>
  selectLog: (fileName: string) => Promise<void>
  clearSelection: () => void
}

export const graphLogStore = create<GraphLogState>((set, get) => ({
  logs: [],
  logRoot: '',
  loading: false,
  error: null,
  selectedFileName: null,
  draft: null,

  loadLogs: async () => {
    set({ loading: true, error: null })
    try {
      const [logs, logRoot] = await Promise.all([
        api.listGraphLogs(),
        api.getGraphLogRoot(),
      ])
      set({ logs, logRoot, loading: false })
      const { selectedFileName } = get()
      if (!selectedFileName && logs.length > 0) {
        await get().selectLog(logs[0].fileName)
      } else if (
        selectedFileName
        && !logs.some((entry) => entry.fileName === selectedFileName)
      ) {
        set({ selectedFileName: null, draft: null })
        if (logs.length > 0) {
          await get().selectLog(logs[0].fileName)
        }
      }
    } catch (e) {
      set({
        logs: [],
        loading: false,
        error: e instanceof Error ? e.message : 'Failed to load graph logs',
        selectedFileName: null,
        draft: null,
      })
    }
  },

  reloadFromDisk: async () => {
    const { selectedFileName } = get()
    await get().loadLogs()
    if (selectedFileName) {
      await get().selectLog(selectedFileName)
    }
  },

  selectLog: async (fileName) => {
    set({ loading: true, error: null })
    try {
      const document = normalizeWorkflowDraft(await api.getGraphLog(fileName))
      set({
        selectedFileName: fileName,
        draft: document,
        loading: false,
      })
    } catch (e) {
      set({
        loading: false,
        error: e instanceof Error ? e.message : 'Failed to open graph log',
      })
    }
  },

  clearSelection: () => {
    set({
      selectedFileName: null,
      draft: null,
      error: null,
    })
  },
}))
