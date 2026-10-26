/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import type { RunEventDto } from '../../api/chatApi'
import type { HumanStepOption, HumanStepParameter } from '../../lib/chatHumanStep'
import {
  isBooleanWidget,
  isNumberWidget,
  isSelectWidget,
  isTextareaWidget,
  resolveHumanInputWidget,
} from '../../lib/humanInputWidget'

export interface ChatHumanInputCardProps {
  pendingHumanEvent: RunEventDto | null
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
    <div className="chat-view-human-approval-toggle" role="group" aria-label={param.label ?? param.id}>
      <button
        type="button"
        className={`chat-view-human-toggle-btn ${value === 'true' ? 'chat-view-human-toggle-btn-active' : ''}`}
        disabled={disabled}
        onClick={() => onChange('true')}
      >
        Yes
      </button>
      <button
        type="button"
        className={`chat-view-human-toggle-btn ${value === 'false' ? 'chat-view-human-toggle-btn-active' : ''}`}
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
  submittingHumanInput: boolean
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
      <label className="chat-view-human-checkbox">
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
        className="chat-view-human-select"
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
        className="chat-view-human-textarea"
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
        className="chat-view-human-text-input"
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
      className="chat-view-human-text-input"
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

export function ChatHumanInputCard({
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
}: ChatHumanInputCardProps) {
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
    <div className="chat-view-human-card" role="region" aria-live="polite">
      <div className="chat-view-human-message">
        <p className="chat-view-human-step-line">User Input Step: {humanPromptMessage}</p>
        {humanPluginName ? (
          <p className="chat-view-human-plugin">Form: {humanPluginName}</p>
        ) : null}
      </div>
      <div className="chat-view-human-task">Task: {humanTaskId}</div>

      {usesPluginForm && humanStepParameters.length > 0 && (
        <div className="chat-view-human-controls chat-view-human-plugin-form">
          {parameterGroups.map(({ group, items }) => (
            <fieldset key={group} className="chat-view-human-fieldset">
              <legend>{group}</legend>
              {items.map((param) => (
                <label key={param.id} className="chat-view-human-field">
                  {!fieldUsesInlineLabel(param) ? (
                    <span className="chat-view-human-field-label">
                      {param.label ?? param.id}
                      {param.required ? ' *' : ''}
                    </span>
                  ) : null}
                  {(param.description || param.ui?.help) && !fieldUsesInlineLabel(param) ? (
                    <span className="chat-view-human-field-help">
                      {param.ui?.help ?? param.description}
                    </span>
                  ) : null}
                  {renderParameterField(
                    param,
                    humanFieldValues,
                    setHumanFieldValue,
                    submittingHumanInput
                  )}
                </label>
              ))}
            </fieldset>
          ))}
        </div>
      )}

      <div className="chat-view-human-controls chat-view-human-options-row">
        {humanStepFooterActions.map((opt, i) => (
          <button
            key={`${opt.label}-${i}`}
            type="button"
            className={`chat-view-human-btn ${i === 0 ? 'chat-view-human-btn-primary' : ''}`}
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
