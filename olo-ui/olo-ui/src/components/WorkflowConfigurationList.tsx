/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { useRef } from 'react'
import type { WorkflowSummary } from '../types/workflow'

export interface WorkflowConfigurationListProps {
  workflows: WorkflowSummary[]
  configurationRoot: string
  loading: boolean
  error: string | null
  selectedFileName: string | null
  onSelect: (fileName: string) => void
  onImportFile: (file: File) => Promise<void>
  onExportSelected: () => void
  exportDisabled: boolean
}

function displayLabel(workflow: WorkflowSummary): string {
  if (workflow.label?.trim()) return workflow.label.trim()
  if (workflow.id?.trim()) return workflow.id.trim()
  return workflow.fileName
}

export function WorkflowConfigurationList({
  workflows,
  configurationRoot,
  loading,
  error,
  selectedFileName,
  onSelect,
  onImportFile,
  onExportSelected,
  exportDisabled,
}: WorkflowConfigurationListProps) {
  const fileInputRef = useRef<HTMLInputElement>(null)

  const handleImportClick = () => fileInputRef.current?.click()

  const handleFileChange = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    await onImportFile(file)
  }

  return (
    <div className="tenant-config-list workflow-config-list">
      <div className="tenant-config-list-header">
        <span className="tenant-config-list-title">Workflow presets</span>
        <div className="workflow-config-actions">
          <button
            type="button"
            className="tenant-config-btn small"
            onClick={handleImportClick}
            title="Import JSON from disk or Drive folder"
          >
            Import
          </button>
          <button
            type="button"
            className="tenant-config-btn small"
            onClick={onExportSelected}
            disabled={exportDisabled}
            title="Export selected workflow JSON"
          >
            Export
          </button>
        </div>
        <input
          ref={fileInputRef}
          type="file"
          accept="application/json,.json"
          className="workflow-config-file-input"
          onChange={handleFileChange}
        />
      </div>
      {configurationRoot ? (
        <p className="workflow-config-root" title={configurationRoot}>
          Folder: {configurationRoot}
        </p>
      ) : null}
      {error ? <p className="tenant-config-error">{error}</p> : null}
      {loading ? (
        <p className="tenant-config-message">Loading…</p>
      ) : workflows.length === 0 ? (
        <p className="tenant-config-message">No workflows. Import a JSON preset to begin.</p>
      ) : (
        <ul className="tenant-config-list-ul">
          {workflows.map((workflow) => (
            <li
              key={workflow.fileName}
              className={`tenant-config-list-item ${selectedFileName === workflow.fileName ? 'selected' : ''}`}
              onClick={() => onSelect(workflow.fileName)}
            >
              <span className="workflow-config-item-label">{displayLabel(workflow)}</span>
              <span className="workflow-config-item-meta">{workflow.fileName}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
