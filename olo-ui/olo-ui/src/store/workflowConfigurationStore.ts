/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { create } from 'zustand'
import type { WorkflowDocument, WorkflowSummary } from '../types/workflow'
import { createWorkflowConfigurationActions } from './workflowConfigurationActions'

export interface WorkflowConfigurationState {
  workflows: WorkflowSummary[]
  configurationRoot: string
  loading: boolean
  error: string | null
  selectedFileName: string | null
  draft: WorkflowDocument | null
  dirty: boolean
  savedSnapshot: string | null
  selectedCanvasNodeId: string | null

  loadWorkflows: () => Promise<void>
  reloadFromDisk: () => Promise<void>
  selectWorkflow: (fileName: string) => Promise<void>
  importWorkflow: (document: WorkflowDocument, fileName?: string) => Promise<void>
  updateDraft: (document: WorkflowDocument) => void
  setSelectedCanvasNodeId: (nodeId: string | null) => void
  saveDraft: () => Promise<void>
  exportSelected: () => WorkflowDocument | null
  deleteSelected: () => Promise<void>
  deleteWorkflow: (fileName: string) => Promise<void>
  copyWorkflow: (fileName: string) => Promise<void>
  renameWorkflow: (fileName: string, newFileName: string) => Promise<void>
  exportWorkflow: (fileName: string) => Promise<WorkflowDocument>
  clearSelection: () => void
}

export const workflowConfigurationStore = create<WorkflowConfigurationState>((set, get) => ({
  workflows: [],
  configurationRoot: '',
  loading: false,
  error: null,
  selectedFileName: null,
  draft: null,
  dirty: false,
  savedSnapshot: null,
  selectedCanvasNodeId: null,
  ...createWorkflowConfigurationActions(set, get),
}))
