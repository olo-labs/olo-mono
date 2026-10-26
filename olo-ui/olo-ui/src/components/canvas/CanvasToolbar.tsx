/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { useUIStore } from '../../store/ui'
import { graphLogOptionLabel, graphLogShortTimestamp } from '../../lib/graphLog'
import { BuilderRunDialog } from '../builder/BuilderRunDialog'
import { DiscardIcon, RefreshIcon, RunIcon, SaveIcon } from './CanvasToolbarIcons'
import { workflowOptionLabel, useCanvasToolbar } from '../../hooks/useCanvasToolbar'
import type { WorkflowCanvasMode } from './workflowCanvasConstants'

export interface CanvasToolbarProps {
  readOnly?: boolean
  mode?: WorkflowCanvasMode
}

export function CanvasToolbar({ readOnly = false, mode = 'builder' }: CanvasToolbarProps) {
  const toolbar = useCanvasToolbar(mode, readOnly)
  const tenantId = useUIStore((s) => s.tenantId)
  const taskQueue = toolbar.draft?.queue?.trim() ?? ''
  const runDisabled = !toolbar.draft

  return (
    <>
      <div className="workflow-canvas-toolbar">
        <div className="workflow-canvas-toolbar-left">
          <label className="workflow-canvas-select-wrap">
            <span className="visually-hidden">{toolbar.isLogMode ? 'Logged graph' : 'Workflow'}</span>
            <select
              className="workflow-canvas-select"
              value={toolbar.selectedFileName ?? ''}
              disabled={toolbar.entriesLoading || toolbar.entries.length === 0}
              onChange={(e) => void toolbar.handleWorkflowChange(e.target.value)}
              aria-label={toolbar.isLogMode ? 'Select logged graph' : 'Select workflow'}
            >
              <option value="" disabled>
                {toolbar.entriesLoading
                  ? (toolbar.isLogMode ? 'Loading graph logs…' : 'Loading workflows…')
                  : (toolbar.isLogMode ? 'Select logged graph…' : 'Select workflow…')}
              </option>
              {toolbar.isLogMode
                ? toolbar.logs.map((entry) => (
                    <option key={entry.fileName} value={entry.fileName}>
                      {graphLogOptionLabel(entry)}
                      {entry.timestamp ? ` (${graphLogShortTimestamp(entry.timestamp)})` : ''}
                    </option>
                  ))
                : toolbar.workflows.map((workflow) => (
                    <option key={workflow.fileName} value={workflow.fileName}>
                      {workflowOptionLabel(workflow.label, workflow.id, workflow.fileName)}
                    </option>
                  ))}
            </select>
          </label>
          {toolbar.isLogMode ? (
            <span className="workflow-canvas-toolbar-readonly-badge" title="Drag nodes to rearrange for visibility; changes are not saved">
              Read-only
            </span>
          ) : null}
          {toolbar.draft?.emoji ? (
            <span className="workflow-canvas-toolbar-emoji" aria-hidden>
              {toolbar.draft.emoji}
            </span>
          ) : null}
        </div>

        {!readOnly ? (
          <div className="workflow-canvas-toolbar-right">
            {toolbar.error ? (
              <span className="workflow-canvas-toolbar-error" title={toolbar.error}>
                Save failed
              </span>
            ) : null}
            <button
              type="button"
              className="workflow-canvas-icon-btn"
              onClick={() => void toolbar.handleRefreshServer()}
              disabled={toolbar.signalingWorker}
              title="Refresh OLO stack (worker, runtime, studio)"
              aria-label="Refresh OLO stack (worker, runtime, studio)"
            >
              <RefreshIcon />
            </button>
            <button
              type="button"
              className="workflow-canvas-run-btn"
              onClick={() => toolbar.setRunOpen(true)}
              disabled={runDisabled}
              title={runDisabled ? 'Select a workflow first' : 'Run workflow'}
            >
              <RunIcon />
              <span>Run</span>
            </button>
            <div className="workflow-canvas-edit-actions">
              <button
                type="button"
                className={`workflow-canvas-discard-btn${toolbar.dirty ? ' dirty' : ''}`}
                onClick={() => void toolbar.handleReloadEntries()}
                disabled={toolbar.discardDisabled}
                title={toolbar.discardTitle}
                aria-label={toolbar.discardTitle}
              >
                <DiscardIcon />
              </button>
              <button
                type="button"
                className={`workflow-canvas-save-btn${toolbar.dirty ? ' dirty' : ''}`}
                onClick={() => void toolbar.handleSave()}
                disabled={toolbar.saveDisabled}
                title={toolbar.saveTitle}
                aria-label={toolbar.saveTitle}
              >
                <SaveIcon />
                {toolbar.dirty ? <span className="workflow-canvas-save-dot" aria-hidden /> : null}
              </button>
            </div>
          </div>
        ) : (
          <div className="workflow-canvas-toolbar-right">
            {toolbar.error ? (
              <span className="workflow-canvas-toolbar-error" title={toolbar.error}>
                {toolbar.isLogMode ? 'Load failed' : 'Save failed'}
              </span>
            ) : null}
            <button
              type="button"
              className="workflow-canvas-icon-btn"
              onClick={() => void toolbar.handleReloadEntries()}
              disabled={toolbar.reloadingUi || toolbar.entriesLoading}
              title={toolbar.discardTitle}
              aria-label={toolbar.discardTitle}
            >
              <RefreshIcon />
            </button>
          </div>
        )}
      </div>

      {!toolbar.isLogMode ? (
        <BuilderRunDialog
          open={toolbar.runOpen}
          initialWorkflowLabel={toolbar.draft?.label ?? toolbar.draft?.id ?? 'Workflow'}
          initialWorkflowId={toolbar.draft?.id?.trim() ?? ''}
          initialTaskQueue={taskQueue}
          tenantId={tenantId}
          onClose={() => toolbar.setRunOpen(false)}
        />
      ) : null}
    </>
  )
}
