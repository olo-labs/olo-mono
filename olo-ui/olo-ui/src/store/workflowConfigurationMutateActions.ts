/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import * as api from '../api/rest'
import { normalizeWorkflowBoundaries } from '../lib/boundaryNodes'
import { ensureWorkflowModelInfrastructure } from '../lib/workflowModelProviders'
import {
  copyWorkflowPath,
  duplicateWorkflowDocument,
} from '../lib/workflowConfiguration'
import { normalizeWorkflowDocumentEmoji } from '../lib/workflowEmoji'
import { getLastWorkflowFileName, setLastWorkflowFileName } from '../lib/lastWorkflow'
import {
  isWorkflowDraftDirty,
  normalizeWorkflowDraft,
  workflowDraftSnapshot,
} from '../lib/workflowDraftSnapshot'
import type { WorkflowDocument } from '../types/workflow'
import type { WorkflowConfigurationState } from './workflowConfigurationStore'

type SetState = (
  partial:
    | Partial<WorkflowConfigurationState>
    | ((state: WorkflowConfigurationState) => Partial<WorkflowConfigurationState>),
) => void
type GetState = () => WorkflowConfigurationState

export function createWorkflowMutateActions(set: SetState, get: GetState) {
  return {
    updateDraft: (document: WorkflowDocument) => {
      const { savedSnapshot } = get()
      set({
        draft: document,
        dirty: isWorkflowDraftDirty(document, savedSnapshot),
      })
    },

    setSelectedCanvasNodeId: (nodeId: string | null) => set({ selectedCanvasNodeId: nodeId }),

    saveDraft: async () => {
      const { draft, selectedFileName } = get()
      if (!draft || !selectedFileName) return
      set({ loading: true, error: null })
      try {
        const normalized = normalizeWorkflowDraft(draft)
        const savedSnapshot = workflowDraftSnapshot(normalized)
        await api.saveWorkflowConfiguration(selectedFileName, normalized)
        set({ draft: normalized, dirty: false, savedSnapshot, loading: false })
        await get().loadWorkflows()
      } catch (e) {
        set({
          loading: false,
          error: e instanceof Error ? e.message : 'Failed to save workflow',
        })
        throw e
      }
    },

    exportSelected: () => get().draft,

    deleteSelected: async () => {
      const { selectedFileName } = get()
      if (!selectedFileName) return
      await get().deleteWorkflow(selectedFileName)
    },

    deleteWorkflow: async (fileName: string) => {
      set({ loading: true, error: null })
      try {
        await api.deleteWorkflowConfiguration(fileName)
        const { selectedFileName } = get()
        if (selectedFileName === fileName) {
          set({ selectedFileName: null, draft: null, dirty: false, savedSnapshot: null })
          setLastWorkflowFileName('')
        } else if (getLastWorkflowFileName() === fileName) {
          setLastWorkflowFileName('')
        }
        set({ loading: false })
        await get().loadWorkflows()
      } catch (e) {
        set({
          loading: false,
          error: e instanceof Error ? e.message : 'Failed to delete workflow',
        })
        throw e
      }
    },

    copyWorkflow: async (fileName: string) => {
      set({ loading: true, error: null })
      try {
        const document = normalizeWorkflowBoundaries(await api.getWorkflowConfiguration(fileName))
        const { workflows } = get()
        const target = copyWorkflowPath(
          fileName,
          workflows.map((workflow) => workflow.fileName),
        )
        const cloned = duplicateWorkflowDocument(document, target)
        await api.saveWorkflowConfiguration(
          target,
          normalizeWorkflowDocumentEmoji(
            ensureWorkflowModelInfrastructure(normalizeWorkflowBoundaries(cloned)),
          ),
        )
        await get().loadWorkflows()
        await get().selectWorkflow(target)
      } catch (e) {
        set({
          loading: false,
          error: e instanceof Error ? e.message : 'Failed to duplicate workflow',
        })
        throw e
      }
    },

    renameWorkflow: async (fileName: string, newFileName: string) => {
      if (fileName === newFileName) return
      set({ loading: true, error: null })
      try {
        const document = await api.getWorkflowConfiguration(fileName)
        await api.saveWorkflowConfiguration(newFileName, document)
        await api.deleteWorkflowConfiguration(fileName)
        await get().loadWorkflows()
        await get().selectWorkflow(newFileName)
      } catch (e) {
        set({
          loading: false,
          error: e instanceof Error ? e.message : 'Failed to rename workflow',
        })
        throw e
      }
    },

    clearSelection: () =>
      set({ selectedFileName: null, draft: null, dirty: false, savedSnapshot: null, selectedCanvasNodeId: null }),
  }
}
