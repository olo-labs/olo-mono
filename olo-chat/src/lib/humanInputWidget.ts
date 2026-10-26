/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import type { HumanStepParameter } from './chatHumanStep'

/** Widget ids emitted by human-input plugins (`parameters[].ui.widget`). */
export type HumanInputWidget =
  | 'STRING'
  | 'TEXTAREA'
  | 'NUMBER'
  | 'BOOLEAN'
  | 'APPROVAL_TOGGLE'
  | 'SELECT'
  | 'SECRET'
  | 'JSON'
  | 'CODE'
  | 'CRON'
  | 'MODEL_SELECTOR'
  | 'MULTI_SELECT'

const WIDGET_ALIASES: Record<string, HumanInputWidget> = {
  text: 'STRING',
  string: 'STRING',
  textarea: 'TEXTAREA',
  number: 'NUMBER',
  boolean: 'BOOLEAN',
  switch: 'BOOLEAN',
  approval_toggle: 'APPROVAL_TOGGLE',
  yes_no: 'APPROVAL_TOGGLE',
  'yes-no': 'APPROVAL_TOGGLE',
  select: 'SELECT',
  enum: 'SELECT',
  multi_select: 'MULTI_SELECT',
}

const KNOWN_WIDGETS = new Set<HumanInputWidget>([
  'STRING',
  'TEXTAREA',
  'NUMBER',
  'BOOLEAN',
  'APPROVAL_TOGGLE',
  'SELECT',
  'SECRET',
  'JSON',
  'CODE',
  'CRON',
  'MODEL_SELECTOR',
  'MULTI_SELECT',
])

function normalizeWidget(raw: string | undefined): HumanInputWidget | null {
  if (!raw) return null
  const trimmed = raw.trim()
  const alias = WIDGET_ALIASES[trimmed.toLowerCase()]
  if (alias) return alias
  const upper = trimmed.toUpperCase() as HumanInputWidget
  return KNOWN_WIDGETS.has(upper) ? upper : null
}

/** Resolves the UI control for a plugin parameter (prefers `ui.widget`, then `type`). */
export function resolveHumanInputWidget(param: HumanStepParameter): HumanInputWidget {
  const fromWidget = normalizeWidget(param.ui?.widget)
  if (fromWidget) return fromWidget

  const type = param.type?.trim().toLowerCase()
  if (type === 'boolean') return 'BOOLEAN'
  if (type === 'enum') return 'SELECT'
  if (type === 'number') return 'NUMBER'
  if (type === 'textarea') return 'TEXTAREA'
  return 'STRING'
}

export function isApprovalToggleWidget(param: HumanStepParameter): boolean {
  return resolveHumanInputWidget(param) === 'APPROVAL_TOGGLE'
}

export function isBooleanWidget(param: HumanStepParameter): boolean {
  return resolveHumanInputWidget(param) === 'BOOLEAN'
}

export function isTextareaWidget(param: HumanStepParameter): boolean {
  return resolveHumanInputWidget(param) === 'TEXTAREA'
}

export function isSelectWidget(param: HumanStepParameter): boolean {
  return resolveHumanInputWidget(param) === 'SELECT'
}

export function isNumberWidget(param: HumanStepParameter): boolean {
  return resolveHumanInputWidget(param) === 'NUMBER'
}

/** True when the form includes text/select/checkbox fields (not only Yes/No toggles). */
export function hasNonButtonInputControls(parameters: HumanStepParameter[]): boolean {
  return parameters.some((param) => resolveHumanInputWidget(param) !== 'APPROVAL_TOGGLE')
}

/** Whether a required parameter has a valid operator value. */
export function isParameterValueValid(param: HumanStepParameter, raw: string): boolean {
  const value = raw ?? ''
  const widget = resolveHumanInputWidget(param)
  if (!param.required) return true
  if (widget === 'APPROVAL_TOGGLE') {
    return value === 'true' || value === 'false'
  }
  if (widget === 'BOOLEAN') {
    return value === 'true' || value === 'false'
  }
  return value.trim().length > 0
}

/** Approve actions require approval-toggle fields to be Yes when present. */
export function approvalTogglesAllowApprove(
  parameters: HumanStepParameter[],
  fieldValues: Record<string, string>
): boolean {
  for (const param of parameters) {
    if (!isApprovalToggleWidget(param)) continue
    if ((fieldValues[param.id] ?? '') !== 'true') return false
  }
  return true
}

export function defaultFieldValue(param: HumanStepParameter): string {
  if (param.defaultValue !== undefined && param.defaultValue !== null) {
    return String(param.defaultValue)
  }
  const widget = resolveHumanInputWidget(param)
  if (widget === 'BOOLEAN') return 'false'
  if (widget === 'APPROVAL_TOGGLE') return ''
  return ''
}
