/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { HumanStepOption, HumanStepParameter } from '../../lib/builderHumanStep'
import {
  isBooleanWidget,
  isNumberWidget,
  isSelectWidget,
  isTextareaWidget,
  resolveHumanInputWidget,
} from '../../lib/humanInputWidget'

export interface BuilderHumanInputCardProps {
  humanPromptMessage: string
  humanStepFooterActions: HumanStepOption[]
  humanStepParameters: HumanStepParameter[]
  humanPluginName: string | null
  humanTaskId: string
  usesPluginForm: boolean
  humanFieldValues: Record<string, string>
  setHumanFieldValue: (id: string, value: string) => void
  pluginFormValid: boolean
  submittingHumanInput: boolean
  onSubmit: (approved: boolean, message: string) => void
}

function groupedParameters(parameters: HumanStepParameter[]): Array<{ group: string; items: HumanStepParameter[] }> {
  const groups = new Map<string, HumanStepParameter[]>()
  for (const param of parameters) {
    const group = param.ui?.group ?? 'Input'
    const bucket = groups.get(group)
    if (bucket) bucket.push(param)
    else groups.set(group, [param])
  }
  return Array.from(groups.entries()).map(([group, items]) => ({ group, items }))
}

function ApprovalToggleField({
  param,
  value,
  onChange,
  disabled,
}: {
  param: HumanStepParameter
  value: string
  onChange: (v: string) => void
  disabled: boolean
}) {
  return (
    <div className="builder-run-human-approval-toggle" role="group" aria-label={param.label ?? param.id}>
      <button
        type="button"
        className={`builder-run-human-toggle-btn${value === 'true' ? ' active' : ''}`}
        disabled={disabled}
        onClick={() => onChange('true')}
      >
        Yes
      </button>
      <button
        type="button"
        className={`builder-run-human-toggle-btn${value === 'false' ? ' active' : ''}`}
        disabled={disabled}
        onClick={() => onChange('false')}
      >
        No
      </button>
    </div>
  )
}

function renderParameterField(
  param: HumanStepParameter,
  humanFieldValues: Record<string, string>,
  setHumanFieldValue: (id: string, value: string) => void,
  submittingHumanInput: boolean,
) {
  const widget = resolveHumanInputWidget(param)
  const value = humanFieldValues[param.id] ?? ''

  if (widget === 'APPROVAL_TOGGLE') {
    return (
      <ApprovalToggleField
        param={param}
        value={value}
        onChange={(v) => setHumanFieldValue(param.id, v)}
        disabled={submittingHumanInput}
      />
    )
  }
  if (isBooleanWidget(param)) {
    return (
      <label className="builder-run-human-checkbox">
        <input
          type="checkbox"
          checked={value === 'true'}
          onChange={(e) => setHumanFieldValue(param.id, e.target.checked ? 'true' : 'false')}
          disabled={submittingHumanInput}
        />
        <span>{param.label ?? param.id}</span>
      </label>
    )
  }
  if (isSelectWidget(param)) {
    return (
      <select
        className="builder-run-select"
        value={value}
        onChange={(e) => setHumanFieldValue(param.id, e.target.value)}
        disabled={submittingHumanInput}
      >
        <option value="">Select…</option>
        {(param.values ?? []).map((optionValue) => (
          <option key={optionValue} value={optionValue}>
            {optionValue}
          </option>
        ))}
      </select>
    )
  }
  if (isTextareaWidget(param)) {
    return (
      <textarea
        className="builder-run-input builder-run-human-textarea"
        placeholder={param.ui?.placeholder ?? ''}
        value={value}
        onChange={(e) => setHumanFieldValue(param.id, e.target.value)}
        disabled={submittingHumanInput}
        rows={3}
      />
    )
  }
  if (isNumberWidget(param)) {
    return (
      <input
        type="number"
        className="builder-run-select"
        placeholder={param.ui?.placeholder ?? ''}
        value={value}
        onChange={(e) => setHumanFieldValue(param.id, e.target.value)}
        disabled={submittingHumanInput}
      />
    )
  }
  return (
    <input
      type="text"
      className="builder-run-select"
      placeholder={param.ui?.placeholder ?? ''}
      value={value}
      onChange={(e) => setHumanFieldValue(param.id, e.target.value)}
      disabled={submittingHumanInput}
    />
  )
}

function fieldUsesInlineLabel(param: HumanStepParameter): boolean {
  return isBooleanWidget(param)
}

export function BuilderHumanInputCard({
  humanPromptMessage,
  humanStepFooterActions,
  humanStepParameters,
  humanPluginName,
  humanTaskId,
  usesPluginForm,
  humanFieldValues,
  setHumanFieldValue,
  pluginFormValid,
  submittingHumanInput,
  onSubmit,
}: BuilderHumanInputCardProps) {
  const parameterGroups = groupedParameters(humanStepParameters)

  const handleAction = (opt: HumanStepOption, index: number) => {
    const approved = opt.approved !== undefined ? opt.approved : index === 0
    const replyText = opt.message?.trim() || opt.label
    if (!approved) {
      onSubmit(false, replyText)
      return
    }
    onSubmit(true, replyText)
  }

  return (
    <div className="builder-run-human-card" role="region" aria-live="polite">
      <div className="builder-run-human-banner">Action required</div>
      <p className="builder-run-human-step-line">User Input Step: {humanPromptMessage}</p>
      {humanPluginName ? <p className="builder-run-meta">Form: {humanPluginName}</p> : null}
      <p className="builder-run-meta">Task: {humanTaskId}</p>

      {usesPluginForm && humanStepParameters.length > 0 && (
        <div className="builder-run-human-form">
          {parameterGroups.map(({ group, items }) => (
            <fieldset key={group} className="builder-run-human-fieldset">
              <legend>{group}</legend>
              {items.map((param) => (
                <label key={param.id} className="builder-run-human-field">
                  {!fieldUsesInlineLabel(param) ? (
                    <span className="builder-run-label">
                      {param.label ?? param.id}
                      {param.required ? ' *' : ''}
                    </span>
                  ) : null}
                  {(param.description || param.ui?.help) && !fieldUsesInlineLabel(param) ? (
                    <span className="builder-run-meta">{param.ui?.help ?? param.description}</span>
                  ) : null}
                  {renderParameterField(
                    param,
                    humanFieldValues,
                    setHumanFieldValue,
                    submittingHumanInput,
                  )}
                </label>
              ))}
            </fieldset>
          ))}
        </div>
      )}

      <div className="builder-run-human-actions">
        {humanStepFooterActions.map((opt, i) => (
          <button
            key={`${opt.label}-${i}`}
            type="button"
            className={`tenant-config-btn${i === 0 ? ' primary' : ''} builder-run-human-btn`}
            disabled={submittingHumanInput || (opt.approved !== false && !pluginFormValid)}
            onClick={() => handleAction(opt, i)}
          >
            {submittingHumanInput ? 'Submitting…' : opt.label}
          </button>
        ))}
      </div>
    </div>
  )
}
