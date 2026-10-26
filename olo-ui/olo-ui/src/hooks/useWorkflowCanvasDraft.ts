/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { catalogStore } from '../store/catalogStore'
import { graphLogStore } from '../store/graphLogStore'
import { workflowConfigurationStore } from '../store/workflowConfigurationStore'

export function useWorkflowCanvasDraft(mode: 'builder' | 'log') {
  const isLogMode = mode === 'log'
  const workflowDraft = workflowConfigurationStore((s) => s.draft)
  const workflowSelectedFileName = workflowConfigurationStore((s) => s.selectedFileName)
  const logDraft = graphLogStore((s) => s.draft)
  const logSelectedFileName = graphLogStore((s) => s.selectedFileName)
  const catalog = catalogStore((s) => s.catalog)
  return {
    isLogMode,
    draft: isLogMode ? logDraft : workflowDraft,
    selectedFileName: isLogMode ? logSelectedFileName : workflowSelectedFileName,
    catalog,
  }
}
