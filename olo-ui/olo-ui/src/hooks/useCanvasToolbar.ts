/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { useState } from 'react'
import { refreshOloStack } from '../api/rest'
import { catalogStore } from '../store/catalogStore'
import { graphLogStore } from '../store/graphLogStore'
import { workflowConfigurationStore } from '../store/workflowConfigurationStore'
import type { WorkflowCanvasMode } from '../components/canvas/workflowCanvasConstants'

export function workflowOptionLabel(
  label: string | null,
  id: string | null,
  fileName: string,
): string {
  if (label) return label
  if (id) return id
  return fileName.replace(/\.json$/i, '')
}

export function useCanvasToolbar(mode: WorkflowCanvasMode, readOnly: boolean) {
  const isLogMode = mode === 'log'
  const workflows = workflowConfigurationStore((s) => s.workflows)
  const workflowsLoading = workflowConfigurationStore((s) => s.loading)
  const selectedWorkflowFile = workflowConfigurationStore((s) => s.selectedFileName)
  const workflowDraft = workflowConfigurationStore((s) => s.draft)
  const workflowDirty = workflowConfigurationStore((s) => s.dirty)
  const workflowError = workflowConfigurationStore((s) => s.error)
  const selectWorkflow = workflowConfigurationStore((s) => s.selectWorkflow)
  const saveDraft = workflowConfigurationStore((s) => s.saveDraft)

  const logs = graphLogStore((s) => s.logs)
  const logsLoading = graphLogStore((s) => s.loading)
  const selectedLogFile = graphLogStore((s) => s.selectedFileName)
  const logDraft = graphLogStore((s) => s.draft)
  const logError = graphLogStore((s) => s.error)
  const selectLog = graphLogStore((s) => s.selectLog)
  const reloadLogs = graphLogStore((s) => s.reloadFromDisk)

  const selectedFileName = isLogMode ? selectedLogFile : selectedWorkflowFile
  const draft = isLogMode ? logDraft : workflowDraft
  const dirty = isLogMode ? false : workflowDirty
  const error = isLogMode ? logError : workflowError
  const entriesLoading = isLogMode ? logsLoading : workflowsLoading
  const entries = isLogMode ? logs : workflows

  const [saving, setSaving] = useState(false)
  const [signalingWorker, setSignalingWorker] = useState(false)
  const [reloadingUi, setReloadingUi] = useState(false)
  const [runOpen, setRunOpen] = useState(false)

  const handleWorkflowChange = async (fileName: string) => {
    if (!fileName || fileName === selectedFileName) return
    if (isLogMode) {
      await selectLog(fileName)
      return
    }
    if (dirty) {
      const discard = window.confirm('You have unsaved changes. Discard them and switch workflow?')
      if (!discard) return
    }
    await selectWorkflow(fileName)
  }

  const handleReloadEntries = async () => {
    if (reloadingUi) return
    if (!isLogMode && dirty) {
      const discard = window.confirm('Discard unsaved changes and reload from disk?')
      if (!discard) return
    }
    setReloadingUi(true)
    try {
      if (isLogMode) {
        await reloadLogs()
      } else {
        await Promise.all([
          catalogStore.getState().loadCatalog(),
          workflowConfigurationStore.getState().reloadFromDisk(),
        ])
      }
    } catch (e) {
      const message = e instanceof Error ? e.message : 'Reload failed'
      window.alert(`Reload from disk failed: ${message}`)
    } finally {
      setReloadingUi(false)
    }
  }

  const handleSave = async () => {
    if (!draft || !selectedFileName || readOnly || saving) return
    setSaving(true)
    try {
      await saveDraft()
    } finally {
      setSaving(false)
    }
  }

  const handleRefreshServer = async () => {
    if (signalingWorker) return
    if (dirty) {
      const proceed = window.confirm(
        'You have unsaved changes. Refresh reloads configuration from disk and may overwrite the canvas. Continue?',
      )
      if (!proceed) return
    }
    setSignalingWorker(true)
    try {
      const result = await refreshOloStack()
      await Promise.all([
        catalogStore.getState().loadCatalog(),
        workflowConfigurationStore.getState().reloadFromDisk(),
      ])
      if (!result.ok) {
        window.alert(
          `Stack refresh incomplete:\n\n${result.steps.join('\n')}\n\n`
            + 'Ensure Redis is reachable, olo-worker has cache.enabled=true, and olo backend is on port 7080.',
        )
        return
      }
      if (!result.runtimeReloaded) {
        window.alert(
          `Worker reloaded. Olo runtime was not refreshed:\n\n${result.runtimeMessage ?? 'unknown error'}\n\n`
            + 'Start olo backend (port 7080) so chat pipeline context updates.',
        )
      }
    } catch (e) {
      const message = e instanceof Error ? e.message : 'Stack refresh failed'
      window.alert(
        `Could not refresh OLO stack:\n\n${message}\n\n`
          + 'Restart olo-be (port 8082) after pulling latest code, then try again.',
      )
    } finally {
      setSignalingWorker(false)
    }
  }

  const discardDisabled = readOnly ? reloadingUi || entriesLoading : reloadingUi || workflowsLoading
  const discardTitle = isLogMode
    ? (reloadingUi ? 'Reloading…' : 'Reload graph logs from disk')
    : reloadingUi
      ? 'Reloading…'
      : dirty
        ? 'Discard changes and reload from disk'
        : 'Reload from disk'
  const saveDisabled = readOnly || !draft || !selectedFileName || saving || !dirty
  const saveTitle = saving ? 'Saving…' : dirty ? 'Save workflow' : 'No unsaved changes'

  return {
    isLogMode,
    workflows,
    logs,
    selectedFileName,
    draft,
    dirty,
    error,
    entriesLoading,
    entries,
    saving,
    signalingWorker,
    reloadingUi,
    runOpen,
    setRunOpen,
    handleWorkflowChange,
    handleReloadEntries,
    handleSave,
    handleRefreshServer,
    discardDisabled,
    discardTitle,
    saveDisabled,
    saveTitle,
  }
}
