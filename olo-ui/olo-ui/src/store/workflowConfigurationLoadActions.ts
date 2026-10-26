/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import * as api from '../api/rest'
import { normalizeWorkflowBoundaries } from '../lib/boundaryNodes'
import { ensureWorkflowModelInfrastructure } from '../lib/workflowModelProviders'
import {
  workflowFileName,
} from '../lib/workflowConfiguration'
import { normalizeWorkflowDocumentEmoji } from '../lib/workflowEmoji'
import { getLastWorkflowFileName, setLastWorkflowFileName } from '../lib/lastWorkflow'
import {
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

export function createWorkflowLoadActions(set: SetState, get: GetState) {
  return {
    loadWorkflows: async () => {
      set({ loading: true, error: null })
      try {
        const [workflows, configurationRoot] = await Promise.all([
          api.listWorkflowConfigurations(),
          api.getConfigurationRoot(),
        ])
        set({ workflows, configurationRoot, loading: false })
        const { selectedFileName } = get()
        if (!selectedFileName) {
          const lastFileName = getLastWorkflowFileName()
          if (lastFileName && workflows.some((workflow) => workflow.fileName === lastFileName)) {
            await get().selectWorkflow(lastFileName)
          }
        }
      } catch (e) {
        set({
          workflows: [],
          loading: false,
          error: e instanceof Error ? e.message : 'Failed to load workflows',
        })
      }
    },

    reloadFromDisk: async () => {
      const { selectedFileName } = get()
      await get().loadWorkflows()
      if (selectedFileName) {
        await get().selectWorkflow(selectedFileName)
      }
    },

    selectWorkflow: async (fileName: string) => {
      set({ loading: true, error: null })
      try {
        const document = normalizeWorkflowDraft(await api.getWorkflowConfiguration(fileName))
        const savedSnapshot = workflowDraftSnapshot(document)
        set({
          selectedFileName: fileName,
          draft: document,
          dirty: false,
          savedSnapshot,
          loading: false,
        })
        setLastWorkflowFileName(fileName)
      } catch (e) {
        set({
          loading: false,
          error: e instanceof Error ? e.message : 'Failed to open workflow',
        })
      }
    },

    importWorkflow: async (document: WorkflowDocument, fileName?: string) => {
      const target = fileName ?? workflowFileName(document)
      set({ loading: true, error: null })
      try {
        await api.saveWorkflowConfiguration(
          target,
          normalizeWorkflowDocumentEmoji(
            ensureWorkflowModelInfrastructure(normalizeWorkflowBoundaries(document)),
          ),
        )
        await get().loadWorkflows()
        await get().selectWorkflow(target)
      } catch (e) {
        set({
          loading: false,
          error: e instanceof Error ? e.message : 'Failed to import workflow',
        })
        throw e
      }
    },

    exportWorkflow: async (fileName: string) => {
      const { selectedFileName, draft } = get()
      if (selectedFileName === fileName && draft) {
        return draft
      }
      return ensureWorkflowModelInfrastructure(
        normalizeWorkflowBoundaries(await api.getWorkflowConfiguration(fileName)),
      )
    },
  }
}
