/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { useEffect } from 'react'
import type { WorkflowDocument } from '../types/workflow'
import {
  catalogQueues,
  catalogWorkflowTypes,
  findCatalogQueue,
} from '../lib/temporalCatalog'
import { catalogStore } from '../store/catalogStore'
import { WorkflowEmojiPicker } from './WorkflowEmojiPicker'

export interface WorkflowGlobalPropertiesProps {
  workflow: WorkflowDocument
  onChange: (workflow: WorkflowDocument) => void
}

function patchWorkflow(
  workflow: WorkflowDocument,
  patch: Partial<WorkflowDocument>,
): WorkflowDocument {
  return { ...workflow, ...patch }
}

export function WorkflowGlobalProperties({ workflow, onChange }: WorkflowGlobalPropertiesProps) {
  const catalog = catalogStore((s) => s.catalog)
  const catalogLoading = catalogStore((s) => s.loading)

  useEffect(() => {
    if (!catalog && !catalogLoading) {
      void catalogStore.getState().loadCatalog()
    }
  }, [catalog, catalogLoading])

  const queueOptions = catalogQueues(catalog)
  const workflowTypeOptions = catalogWorkflowTypes(catalog)
  const currentQueue = workflow.queue ?? ''
  const currentWorkflowType = workflow.workflowType ?? ''

  const handleQueueChange = (queueName: string) => {
    const queue = findCatalogQueue(catalog, queueName)
    onChange(
      patchWorkflow(workflow, {
        queue: queueName,
        workflowType: queue?.workflowType ?? workflow.workflowType,
      }),
    )
  }

  return (
    <section className="workflow-config-section">
      <h3 className="workflow-config-section-title">Workflow</h3>

      <label className="tenant-config-form-row workflow-param-row">
        <span className="tenant-config-label">ID</span>
        <input className="tenant-config-input" type="text" value={workflow.id} readOnly />
      </label>

      <label className="tenant-config-form-row workflow-param-row">
        <span className="tenant-config-label">Enabled</span>
        <input
          type="checkbox"
          checked={workflow.enabled !== false}
          onChange={(e) => onChange(patchWorkflow(workflow, { enabled: e.target.checked }))}
        />
      </label>

      <label className="tenant-config-form-row workflow-param-row">
        <span className="tenant-config-label">Label</span>
        <input
          className="tenant-config-input"
          type="text"
          value={workflow.label ?? ''}
          onChange={(e) => onChange(patchWorkflow(workflow, { label: e.target.value }))}
        />
      </label>

      <label className="tenant-config-form-row workflow-param-row">
        <span className="tenant-config-label">Role</span>
        <input
          className="tenant-config-input"
          type="text"
          value={workflow.role ?? ''}
          onChange={(e) => onChange(patchWorkflow(workflow, { role: e.target.value }))}
        />
      </label>

      <label className="tenant-config-form-row workflow-param-row">
        <span className="tenant-config-label">Short description</span>
        <input
          className="tenant-config-input"
          type="text"
          value={workflow.shortDescription ?? ''}
          onChange={(e) => onChange(patchWorkflow(workflow, { shortDescription: e.target.value }))}
        />
      </label>

      <div className="tenant-config-form-row workflow-param-row workflow-emoji-field">
        <span className="tenant-config-label">Emoji</span>
        <WorkflowEmojiPicker
          value={workflow.emoji}
          onChange={(emoji) => onChange(patchWorkflow(workflow, { emoji }))}
        />
      </div>

      <label className="tenant-config-form-row workflow-param-row">
        <span className="tenant-config-label">Queue</span>
        {queueOptions.length > 0 ? (
          <select
            className="tenant-config-input"
            value={currentQueue}
            onChange={(e) => handleQueueChange(e.target.value)}
          >
            <option value="">Select queue…</option>
            {queueOptions.map((queue) => (
              <option key={queue.name} value={queue.name}>
                {queue.label} ({queue.name})
              </option>
            ))}
          </select>
        ) : (
          <input
            className="tenant-config-input"
            type="text"
            value={currentQueue}
            onChange={(e) => onChange(patchWorkflow(workflow, { queue: e.target.value }))}
            placeholder={catalogLoading ? 'Loading queues…' : 'Queue name'}
          />
        )}
      </label>

      <label className="tenant-config-form-row workflow-param-row">
        <span className="tenant-config-label">Workflow type</span>
        {workflowTypeOptions.length > 0 ? (
          <select
            className="tenant-config-input"
            value={currentWorkflowType}
            onChange={(e) => onChange(patchWorkflow(workflow, { workflowType: e.target.value }))}
          >
            <option value="">Select type…</option>
            {workflowTypeOptions.map((type) => (
              <option key={type.id} value={type.id}>
                {type.label}
              </option>
            ))}
          </select>
        ) : (
          <input
            className="tenant-config-input"
            type="text"
            value={currentWorkflowType}
            onChange={(e) => onChange(patchWorkflow(workflow, { workflowType: e.target.value }))}
            placeholder={catalogLoading ? 'Loading types…' : 'Workflow type'}
          />
        )}
      </label>

      <label className="tenant-config-form-row workflow-param-row">
        <span className="tenant-config-label">Run again</span>
        <input
          type="checkbox"
          checked={workflow.runAgain === true}
          onChange={(e) => onChange(patchWorkflow(workflow, { runAgain: e.target.checked }))}
        />
      </label>

      <label className="tenant-config-form-row workflow-param-row">
        <span className="tenant-config-label">Version</span>
        <input
          className="tenant-config-input"
          type="text"
          value={workflow.version ?? ''}
          onChange={(e) => onChange(patchWorkflow(workflow, { version: e.target.value }))}
        />
      </label>
    </section>
  )
}
