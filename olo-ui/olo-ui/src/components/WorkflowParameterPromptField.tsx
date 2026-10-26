/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { CatalogComponentBase, CatalogParameter } from '../types/catalog'
import type { WorkflowDocument } from '../types/workflow'
import { agentModelOptions } from '../lib/workflowModelProviders'
import { catalogStore } from '../store/catalogStore'
import { readPlannerContext, updatePlannerContext } from '../lib/plannerContext'
import { PromptTokenTextarea } from './PromptTokenTextarea'
import { buildPromptInsertOptions } from '../lib/promptTokens'

export function WorkflowParameterPromptField({
  descriptor,
  value,
  onChange,
  compact = false,
  workflowVariableNames = [],
  workflow,
  onWorkflowChange,
  catalogTools,
}: {
  descriptor: CatalogParameter
  value: unknown
  onChange: (value: unknown) => void
  compact?: boolean
  workflowVariableNames?: string[]
  workflow?: WorkflowDocument | null
  onWorkflowChange?: (workflow: WorkflowDocument) => void
  catalogTools?: CatalogComponentBase[]
}) {
  const stringValue = value == null ? '' : String(value)
  const label = descriptor.label ?? descriptor.id
  const plannerContext = workflow ? readPlannerContext(workflow) : null
  const resolvedCatalogTools = catalogTools ?? catalogStore.getState().catalog?.tools ?? []

  const patchPlannerContext = (patch: { injectCapabilities?: boolean; injectAgents?: boolean }) => {
    if (!workflow || !onWorkflowChange) return
    onWorkflowChange(updatePlannerContext(workflow, patch, resolvedCatalogTools))
  }

  return (
    <div className={compact ? 'catalog-flow-param-row catalog-flow-param-row-stack' : 'workflow-param-row-stack'}>
      <span className={compact ? 'catalog-flow-param-label' : 'tenant-config-label'}>{label}</span>
      <PromptTokenTextarea
        compact={compact}
        rows={compact ? 3 : 6}
        value={stringValue}
        placeholder={descriptor.ui?.placeholder}
        workflowVariableNames={workflowVariableNames}
        insertOptions={buildPromptInsertOptions(workflowVariableNames)}
        textareaClassName={compact ? 'catalog-flow-param-textarea' : 'tenant-config-input workflow-param-textarea'}
        onChange={(next) => onChange(next)}
      />
      {workflow && onWorkflowChange && plannerContext ? (
        <div className="prompt-inject-options">
          <label className="prompt-inject-option">
            <input
              type="checkbox"
              checked={plannerContext.injectCapabilities}
              onChange={(e) => patchPlannerContext({ injectCapabilities: e.target.checked })}
            />
            <span>Tools</span>
          </label>
          <label className="prompt-inject-option">
            <input
              type="checkbox"
              checked={plannerContext.injectAgents}
              onChange={(e) => patchPlannerContext({ injectAgents: e.target.checked })}
            />
            <span>Agents</span>
          </label>
          <p className="prompt-inject-hint">
            When enabled, tool and agent definitions are appended to the prompt at run time, in
            addition to {'{tools}'} and {'{agents}'} placeholders in your template.
            {plannerContext.selectedTools.length > 0 || plannerContext.selectedAgents.length > 0 ? (
              <>
                {' '}
                Using {plannerContext.selectedTools.length} tool
                {plannerContext.selectedTools.length === 1 ? '' : 's'} and{' '}
                {plannerContext.selectedAgents.length} agent
                {plannerContext.selectedAgents.length === 1 ? '' : 's'} from planner context.
              </>
            ) : null}
          </p>
        </div>
      ) : null}
    </div>
  )
}

export function WorkflowParameterModelField({
  descriptor,
  value,
  onChange,
  compact = false,
  workflow,
}: {
  descriptor: CatalogParameter
  value: unknown
  onChange: (value: unknown) => void
  compact?: boolean
  workflow: WorkflowDocument
}) {
  const stringValue = value == null ? '' : String(value)
  const label = descriptor.label ?? descriptor.id
  const rowClass = compact ? 'catalog-flow-param-row' : 'tenant-config-form-row workflow-param-row'
  const inputClass = compact ? 'catalog-flow-param-input' : 'tenant-config-input'

  return (
    <label className={rowClass}>
      <span className="catalog-flow-param-label">{label}</span>
      <select
        className={inputClass}
        value={stringValue}
        onChange={(e) => onChange(e.target.value)}
      >
        <option value="">Select…</option>
        {agentModelOptions(workflow).map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </label>
  )
}
