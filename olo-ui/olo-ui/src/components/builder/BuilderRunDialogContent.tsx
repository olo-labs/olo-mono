/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { BUILDER_RUN_PROMPT_PRESETS, DEFAULT_BUILDER_RUN_PROMPT } from '../../lib/builderRunPrompts'
import { catalogQueues, catalogWorkflowTypes } from '../../lib/temporalCatalog'
import { catalogStore } from '../../store/catalogStore'
import { workflowConfigurationStore } from '../../store/workflowConfigurationStore'
import type { useBuilderRunDialog } from '../../hooks/useBuilderRunDialog'
import { BuilderHumanInputCard } from './BuilderHumanInputCard'

type RunState = ReturnType<typeof useBuilderRunDialog>

export interface BuilderRunDialogContentProps {
  onClose: () => void
  initialWorkflowLabel: string
  run: RunState
}

export function BuilderRunDialogContent({
  onClose,
  initialWorkflowLabel,
  run,
}: BuilderRunDialogContentProps) {
  const catalogError = catalogStore((s) => s.error)
  const workflowsError = workflowConfigurationStore((s) => s.error)
  const workflowLabel = run.selectedWorkflow?.label ?? initialWorkflowLabel ?? run.selectedWorkflowId
  const taskQueue = run.selectedQueue.trim()
  const workflowId = run.selectedWorkflowId.trim()
  const queues = catalogQueues(run.catalog)
  const workflowTypes = catalogWorkflowTypes(run.catalog)
  const runDisabled =
    run.running ||
    run.catalogLoading ||
    run.workflowsLoading ||
    !run.catalog ||
    queues.length === 0 ||
    !taskQueue ||
    !workflowId ||
    !run.prompt.trim()

  const showHumanInput = !!run.humanInput.pendingHumanEvent
  const showRunSetup = !showHumanInput

  return (
    <>
      <div className="builder-run-backdrop" onClick={run.running ? undefined : onClose} aria-hidden />
      <div className="builder-run-dialog" role="dialog" aria-labelledby="builder-run-title">
        <div className="builder-run-header">
          <h2 id="builder-run-title" className="builder-run-title">
            Run workflow
          </h2>
          <button
            type="button"
            className="builder-run-close"
            onClick={onClose}
            disabled={run.running}
            aria-label="Close"
          >
            ×
          </button>
        </div>

        {showHumanInput ? (
          <BuilderHumanInputCard
            humanPromptMessage={run.humanInput.humanPromptMessage}
            humanStepFooterActions={run.humanInput.humanStepFooterActions}
            humanStepParameters={run.humanInput.humanStepParameters}
            humanPluginName={run.humanInput.humanPluginName}
            humanTaskId={run.humanInput.humanTaskId}
            usesPluginForm={run.humanInput.usesPluginForm}
            humanFieldValues={run.humanInput.humanFieldValues}
            setHumanFieldValue={run.humanInput.setHumanFieldValue}
            pluginFormValid={run.humanInput.pluginFormValid}
            submittingHumanInput={run.humanInput.submittingHumanInput}
            onSubmit={run.humanInput.handleSubmitHumanInput}
          />
        ) : null}

        {showRunSetup ? (
          <>
        {catalogError ? (
          <p className="builder-run-error">{catalogError}</p>
        ) : workflowsError ? (
          <p className="builder-run-error">{workflowsError}</p>
        ) : run.catalogLoading && !run.catalog ? (
          <p className="builder-run-meta">Loading queues and workflow types…</p>
        ) : run.workflowsLoading ? (
          <p className="builder-run-meta">Loading workflow presets…</p>
        ) : run.catalog ? (
          <div className="builder-run-target">
            <label className="builder-run-field">
              <span className="builder-run-label">Queue</span>
              <select
                className="builder-run-select tenant-config-input"
                value={run.selectedQueue}
                onChange={(e) => run.handleQueueChange(e.target.value)}
                disabled={run.running || queues.length === 0}
              >
                {queues.length === 0 ? (
                  <option value="">No queues in catalog</option>
                ) : (
                  <>
                    {!run.selectedQueue ? <option value="">Select queue…</option> : null}
                    {queues.map((queue) => (
                      <option key={queue.name} value={queue.name}>
                        {queue.label} ({queue.name})
                      </option>
                    ))}
                  </>
                )}
              </select>
            </label>

            <label className="builder-run-field">
              <span className="builder-run-label">Workflow</span>
              <select
                className="builder-run-select tenant-config-input"
                value={run.selectedWorkflowId}
                onChange={(e) => run.setSelectedWorkflowId(e.target.value)}
                disabled={run.running || run.queueWorkflows.length === 0}
              >
                {run.queueWorkflows.length === 0 ? (
                  <option value="">
                    {run.workflows.length === 0 ? 'No workflow presets loaded' : 'No workflows for this queue'}
                  </option>
                ) : (
                  <>
                    {!run.selectedWorkflowId ? <option value="">Select workflow…</option> : null}
                    {run.queueWorkflows.map((workflow) => (
                      <option key={workflow.fileName} value={workflow.id ?? workflow.fileName}>
                        {workflow.label ?? workflow.id ?? workflow.fileName}
                      </option>
                    ))}
                  </>
                )}
              </select>
            </label>

            <label className="builder-run-field">
              <span className="builder-run-label">Workflow type</span>
              <select className="builder-run-select tenant-config-input" value={run.workflowType} disabled aria-readonly>
                {(workflowTypes.length > 0 ? workflowTypes : [{ id: run.workflowType, label: run.workflowType }]).map(
                  (type) => (
                    <option key={type.id} value={type.id}>
                      {type.label}
                    </option>
                  ),
                )}
              </select>
            </label>
          </div>
        ) : null}

        <label className="builder-run-label" htmlFor="builder-run-prompt">
          Input
        </label>
        <div className="builder-run-quick-prompts" role="group" aria-label="Quick test messages">
          {BUILDER_RUN_PROMPT_PRESETS.map((preset) => (
            <button
              key={preset.id}
              type="button"
              className={`builder-run-quick-btn${run.prompt === preset.message ? ' active' : ''}`}
              onClick={() => run.setPrompt(preset.message)}
              disabled={run.running}
              title={preset.message}
            >
              {preset.label}
            </button>
          ))}
        </div>
        <textarea
          id="builder-run-prompt"
          className="builder-run-input"
          rows={4}
          value={run.prompt}
          onChange={(e) => run.setPrompt(e.target.value)}
          placeholder="Enter a message or task for the workflow…"
          disabled={run.running}
        />

        <div className="builder-run-actions">
          <button
            type="button"
            className="tenant-config-btn primary builder-run-submit"
            onClick={() => void run.handleRun(workflowLabel)}
            disabled={runDisabled}
          >
            {run.running ? (run.cancelling ? 'Cancelling…' : 'Running…') : 'Run'}
          </button>
          {run.running ? (
            <button
              type="button"
              className="tenant-config-btn danger builder-run-cancel"
              onClick={() => void run.handleCancel()}
              disabled={run.cancelling}
            >
              Cancel
            </button>
          ) : null}
          <button
            type="button"
            className="tenant-config-btn builder-run-quick-run"
            onClick={() => void run.handleRun(workflowLabel, DEFAULT_BUILDER_RUN_PROMPT)}
            disabled={runDisabled}
            title={`Run with "${DEFAULT_BUILDER_RUN_PROMPT}"`}
          >
            Quick test
          </button>
        </div>

          </>
        ) : null}

        {run.error ? <p className="builder-run-error">{run.error}</p> : null}

        {run.running ? (
          <div className="builder-run-actions">
            <button
              type="button"
              className="tenant-config-btn danger builder-run-cancel"
              onClick={() => void run.handleCancel()}
              disabled={run.cancelling}
            >
              {run.cancelling ? 'Cancelling…' : 'Cancel run'}
            </button>
          </div>
        ) : null}

        <div className="builder-run-log-wrap">
          <div className="builder-run-log-label">Progress</div>
          <div ref={run.logRef} className="builder-run-log">
            {run.logLines.length === 0 ? (
              <span className="builder-run-log-empty">Run output will appear here.</span>
            ) : (
              run.logLines.map((line, index) => (
                <div key={`${index}-${line}`} className="builder-run-log-line">
                  {line}
                </div>
              ))
            )}
          </div>
        </div>

        <div className="builder-run-response-wrap">
          <div className="builder-run-log-label">Final response</div>
          <div className="builder-run-response">
            {run.finalResponse ||
              (run.humanInput.pendingHumanEvent
                ? 'Waiting for your input…'
                : run.running
                  ? 'Waiting for completion…'
                  : '—')}
          </div>
        </div>
      </div>
    </>
  )
}
