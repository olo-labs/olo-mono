/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { CatalogComponentBase, CatalogParameter } from '../types/catalog'
import type { WorkflowDocument } from '../types/workflow'
import { SYSTEM_PROMPT_PARAMETER_ID } from '../lib/promptTokens'
import {
  WorkflowParameterModelField,
  WorkflowParameterPromptField,
} from './WorkflowParameterPromptField'

export {
  workflowParameterToDescriptor,
  updateWorkflowParameterValue,
} from '../lib/workflowParameterHelpers'

export function WorkflowParameterField({
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
  const widget = descriptor.ui?.widget ?? 'STRING'
  const label = descriptor.label ?? descriptor.id
  const rowClass = compact ? 'catalog-flow-param-row' : 'tenant-config-form-row workflow-param-row'
  const inputClass = compact ? 'catalog-flow-param-input' : 'tenant-config-input'

  if (widget === 'BOOLEAN') {
    return (
      <label className={rowClass}>
        <span className="catalog-flow-param-label">{label}</span>
        <input
          type="checkbox"
          checked={Boolean(value)}
          onChange={(e) => onChange(e.target.checked)}
        />
      </label>
    )
  }

  if (
    widget === 'SLIDER'
    || widget === 'NUMBER'
    || descriptor.type === 'number'
    || descriptor.type === 'integer'
  ) {
    const min = descriptor.validation?.minimum
    const max = descriptor.validation?.maximum
    const step = descriptor.validation?.step ?? (descriptor.type === 'integer' ? 1 : 0.1)
    return (
      <label className={rowClass}>
        <span className="catalog-flow-param-label">{label}</span>
        <input
          className={inputClass}
          type="number"
          value={value === undefined || value === null ? '' : String(value)}
          min={min}
          max={max}
          step={step}
          onChange={(e) => {
            const raw = e.target.value
            if (raw === '') {
              onChange(null)
              return
            }
            onChange(descriptor.type === 'integer' ? parseInt(raw, 10) : parseFloat(raw))
          }}
        />
      </label>
    )
  }

  if (widget === 'MODEL_SELECTOR' && workflow) {
    return (
      <WorkflowParameterModelField
        descriptor={descriptor}
        value={value}
        onChange={onChange}
        compact={compact}
        workflow={workflow}
      />
    )
  }

  if (widget === 'TEXTAREA' && descriptor.id === SYSTEM_PROMPT_PARAMETER_ID) {
    return (
      <WorkflowParameterPromptField
        descriptor={descriptor}
        value={value}
        onChange={onChange}
        compact={compact}
        workflowVariableNames={workflowVariableNames}
        workflow={workflow}
        onWorkflowChange={onWorkflowChange}
        catalogTools={catalogTools}
      />
    )
  }

  if (widget === 'TEXTAREA') {
    return (
      <label className={rowClass}>
        <span className="catalog-flow-param-label">{label}</span>
        <textarea
          className={`${inputClass} catalog-flow-param-textarea`}
          value={value == null ? '' : String(value)}
          placeholder={descriptor.ui?.placeholder}
          rows={2}
          onChange={(e) => onChange(e.target.value)}
        />
      </label>
    )
  }

  return (
    <label className={rowClass}>
      <span className="catalog-flow-param-label">{label}</span>
      <input
        className={inputClass}
        type="text"
        value={value == null ? '' : String(value)}
        placeholder={descriptor.ui?.placeholder}
        onChange={(e) => onChange(e.target.value)}
      />
    </label>
  )
}
