/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { useEffect, useRef } from 'react'
import type { AgentsCanvasActions, AgentsFileActions } from './AgentsContextMenu'

export function AgentsCanvasToolbar({
  configurationRoot,
  loading,
  selectedFileName,
  exportDisabled,
  fileActions,
  canvasActions,
  onImportFile,
  onImportDialogReady,
}: {
  configurationRoot: string
  loading: boolean
  selectedFileName: string | null
  exportDisabled: boolean
  fileActions: AgentsFileActions
  canvasActions: AgentsCanvasActions
  onImportFile: (file: File) => Promise<void>
  onImportDialogReady: (openImportDialog: () => void) => void
}) {
  const fileInputRef = useRef<HTMLInputElement>(null)

  const openImportDialog = () => {
    fileInputRef.current?.click()
  }

  useEffect(() => {
    onImportDialogReady(openImportDialog)
  }, [onImportDialogReady])

  const handleFileChange = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    await onImportFile(file)
  }

  return (
    <>
      <div className="agents-canvas-toolbar">
        <div className="agents-canvas-toolbar-left">
          <span className="agents-canvas-title">Agent presets</span>
          {configurationRoot ? (
            <span className="agents-canvas-root" title={configurationRoot}>
              {configurationRoot}
            </span>
          ) : null}
        </div>
        <div className="agents-canvas-actions">
          <button
            type="button"
            className="tenant-config-btn small"
            onClick={() => void canvasActions.onReload()}
            disabled={loading}
            title="Reload all files from configuration folders"
          >
            Reload
          </button>
          <button
            type="button"
            className="tenant-config-btn small"
            onClick={openImportDialog}
            title="Import JSON from disk"
          >
            Import
          </button>
          <button
            type="button"
            className="tenant-config-btn small"
            onClick={() => {
              if (selectedFileName) void fileActions.onExport(selectedFileName)
            }}
            disabled={exportDisabled && !selectedFileName}
            title="Export selected workflow JSON"
          >
            Export
          </button>
          <input
            ref={fileInputRef}
            type="file"
            accept="application/json,.json"
            className="workflow-config-file-input"
            onChange={handleFileChange}
          />
        </div>
      </div>

      <p className="agents-canvas-hint">
        Active configuration: <code>current-active/</code> under the scenario catalog. Switch scenarios in{' '}
        <strong>Administration → Scenarios</strong> (Activate copies a folder and refreshes worker + studio).
        Until integrated versioning rolls out in the UI (SCHEDULED-V6), use git on{' '}
        <code>olo-configuration/</code> for history.
      </p>
    </>
  )
}
