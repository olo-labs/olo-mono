/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { RunEventDto } from '../api/oloRuntime'
import { isWorkflowCancelled, isWorkflowFinished } from './assistantResponse'
import { hasNonButtonInputControls, resolveHumanInputWidget } from './humanInputWidget'

function stringValue(v: unknown): string | null {
  return typeof v === 'string' && v.trim().length > 0 ? v.trim() : null
}

function readEventMap(
  ev: RunEventDto | null | undefined,
  key: 'input' | 'output' | 'metadata',
): Record<string, unknown> | undefined {
  const map = ev?.[key]
  return map != null && typeof map === 'object' ? map : undefined
}

export function humanStepPromptFromEvent(ev: RunEventDto | null | undefined): string {
  if (!ev) return 'This run needs your input.'
  const output = readEventMap(ev, 'output')
  const input = readEventMap(ev, 'input')
  const meta = readEventMap(ev, 'metadata')
  return (
    stringValue(output?.prompt) ??
    stringValue(output?.title) ??
    stringValue(input?.prompt) ??
    stringValue(input?.message) ??
    stringValue(output?.message) ??
    stringValue(output?.description) ??
    stringValue(meta?.message) ??
    stringValue(meta?.prompt) ??
    'This run needs your input.'
  )
}

export type HumanStepParameter = {
  id: string
  label?: string
  type?: string
  required?: boolean
  description?: string
  defaultValue?: unknown
  values?: string[]
  ui?: {
    widget?: string
    group?: string
    help?: string
    placeholder?: string
    order?: number
  }
}

function readParameters(raw: unknown): HumanStepParameter[] {
  if (!Array.isArray(raw) || raw.length === 0) return []
  return raw
    .map((item) => {
      if (item == null || typeof item !== 'object') return null
      const o = item as Record<string, unknown>
      const id = stringValue(o.id)
      if (!id) return null
      const param: HumanStepParameter = { id }
      const label = stringValue(o.label) ?? stringValue(o.name)
      if (label) param.label = label
      const type = stringValue(o.type)
      if (type) param.type = type
      if (typeof o.required === 'boolean') param.required = o.required
      const description = stringValue(o.description)
      if (description) param.description = description
      if (o.defaultValue !== undefined) param.defaultValue = o.defaultValue
      if (Array.isArray(o.values)) param.values = o.values.map((v) => String(v))
      if (o.ui != null && typeof o.ui === 'object') {
        const ui = o.ui as Record<string, unknown>
        param.ui = {
          widget: stringValue(ui.widget) ?? undefined,
          group: stringValue(ui.group) ?? undefined,
          help: stringValue(ui.help) ?? undefined,
          placeholder: stringValue(ui.placeholder) ?? undefined,
          order: typeof ui.order === 'number' ? ui.order : undefined,
        }
      }
      return param
    })
    .filter((p): p is HumanStepParameter => p != null)
    .sort((a, b) => (a.ui?.order ?? 0) - (b.ui?.order ?? 0))
}

export function humanStepParametersFromEvent(ev: RunEventDto | null | undefined): HumanStepParameter[] {
  if (!ev) return []
  for (const source of [
    readEventMap(ev, 'output')?.parameters,
    readEventMap(ev, 'input')?.parameters,
    readEventMap(ev, 'metadata')?.parameters,
  ]) {
    const parsed = readParameters(source)
    if (parsed.length > 0) return parsed
  }
  return []
}

export function humanStepPluginNameFromEvent(ev: RunEventDto | null | undefined): string | null {
  if (!ev) return null
  const output = readEventMap(ev, 'output')
  const input = readEventMap(ev, 'input')
  return (
    stringValue(output?.pluginName) ??
    stringValue(output?.inputPluginId) ??
    stringValue(input?.inputPluginId) ??
    null
  )
}

export function humanStepUsesPluginForm(ev: RunEventDto | null | undefined): boolean {
  return humanStepParametersFromEvent(ev).length > 0
}

export type HumanStepOption = {
  label: string
  approved?: boolean
  message?: string
}

export function humanStepOptionsFromEvent(ev: RunEventDto | null | undefined): HumanStepOption[] {
  if (!ev) return []
  const raw =
    readEventMap(ev, 'output')?.options ??
    readEventMap(ev, 'input')?.options ??
    readEventMap(ev, 'metadata')?.options
  if (!Array.isArray(raw) || raw.length === 0) return []
  return raw.map((item, i) => {
    if (typeof item === 'string') {
      const s = item.trim()
      return { label: s, message: s, approved: true }
    }
    if (item != null && typeof item === 'object') {
      const o = item as Record<string, unknown>
      const label = stringValue(o.label) ?? stringValue(o.text) ?? `Option ${i + 1}`
      const approved = typeof o.approved === 'boolean' ? o.approved : undefined
      const msg = stringValue(o.message)
      const out: HumanStepOption = { label, approved }
      if (msg !== null) out.message = msg
      return out
    }
    return { label: String(item), approved: true }
  })
}

export const DEFAULT_HUMAN_APPROVE_OPTION: HumanStepOption = { label: 'Approve', approved: true }
export const DEFAULT_HUMAN_SUBMIT_OPTION: HumanStepOption = { label: 'Submit', approved: true }
export const DEFAULT_HUMAN_CANCEL_OPTION: HumanStepOption = {
  label: 'Cancel',
  approved: false,
  message: 'Cancelled by operator',
}

export const DEFAULT_HUMAN_OPTION_BUTTONS: HumanStepOption[] = [
  DEFAULT_HUMAN_APPROVE_OPTION,
  DEFAULT_HUMAN_CANCEL_OPTION,
]

export function humanStepEventKey(ev: RunEventDto | null | undefined): string | null {
  if (!ev?.runId?.trim()) return null
  return `${ev.runId.trim()}:${ev.nodeId ?? ''}:${ev.sequenceNumber ?? 0}`
}

/** Build chat-history text from plugin form field values the operator entered. */
export function buildHumanStepHistoryText(
  parameters: HumanStepParameter[],
  fieldValues: Record<string, string>,
  fallback: string,
): string {
  if (parameters.length === 0) return fallback.trim()
  const lines: string[] = []
  for (const param of parameters) {
    const raw = (fieldValues[param.id] ?? '').trim()
    if (!raw && !param.required) continue
    const widget = resolveHumanInputWidget(param)
    let display = raw
    if (widget === 'BOOLEAN' || widget === 'APPROVAL_TOGGLE') {
      if (raw === 'true') display = 'Yes'
      else if (raw === 'false') display = 'No'
      else if (!raw) continue
    }
    if (display) lines.push(display)
  }
  return lines.length > 0 ? lines.join('\n') : fallback.trim()
}

function isHumanNodeType(nodeType: string | null | undefined): boolean {
  const t = nodeType?.toUpperCase()
  return t === 'HUMAN' || t === 'ADMIN' || t === 'HUMAN_APPROVAL'
}

function isHumanWaitingEvent(ev: RunEventDto): boolean {
  const output = readEventMap(ev, 'output')
  const meta = readEventMap(ev, 'metadata')
  const status = ev.status?.toUpperCase()
  const humanOutput =
    output?.status === 'HUMAN_WAITING' ||
    output?.approvalStatus === 'waiting' ||
    meta?.phase === 'human-wait'

  if (isHumanNodeType(ev.nodeType) && status === 'WAITING') {
    return true
  }
  if (humanOutput && isHumanNodeType(ev.nodeType)) {
    return true
  }
  if (humanOutput && (status === 'WAITING' || status === 'STARTED' || status === 'RUNNING')) {
    return true
  }
  return false
}

function resolveScopedRunId(
  runEvents: RunEventDto[],
  activeRunId?: string | null,
  allowRunIdFromEvents = false,
): string | null {
  const fromActive = activeRunId?.trim()
  if (fromActive) return fromActive
  if (!allowRunIdFromEvents) return null
  for (let i = runEvents.length - 1; i >= 0; i--) {
    const id = runEvents[i]?.runId?.trim()
    if (id) return id
  }
  return null
}

/** Fallback when API status is waiting_human but the HUMAN WAITING event has not arrived yet. */
export function syntheticHumanWaitingEvent(runId: string): RunEventDto {
  return {
    runId,
    nodeId: 'human-input',
    nodeType: 'HUMAN',
    status: 'WAITING',
    timestamp: Date.now(),
    output: { status: 'HUMAN_WAITING', approvalStatus: 'waiting' },
    metadata: { phase: 'human-wait' },
  }
}

export function findPendingHumanEvent(
  runEvents: RunEventDto[],
  activeRunId?: string | null,
  options?: { allowRunIdFromEvents?: boolean }
): RunEventDto | null {
  const runId = resolveScopedRunId(runEvents, activeRunId, options?.allowRunIdFromEvents === true)
  if (!runId) return null

  const scoped = runEvents.filter((e) => (e.runId ?? '').trim() === runId)
  if (scoped.length === 0) return null

  const latestHumanWaiting = [...scoped].reverse().find(isHumanWaitingEvent)
  const hasHumanCompletedAfterWait = latestHumanWaiting
    ? scoped.some(
        (e) =>
          e.nodeType?.toUpperCase() === 'HUMAN' &&
          e.status?.toUpperCase() === 'COMPLETED' &&
          e.nodeId === latestHumanWaiting.nodeId &&
          (e.sequenceNumber ?? 0) >= (latestHumanWaiting.sequenceNumber ?? 0),
      )
    : false
  if (latestHumanWaiting && !hasHumanCompletedAfterWait) {
    return latestHumanWaiting
  }

  if (isWorkflowCancelled(scoped) || isWorkflowFinished(scoped)) return null
  return null
}

export function resolveHumanStepFooterActions(
  parameters: HumanStepParameter[],
  pluginOptions: HumanStepOption[],
): HumanStepOption[] {
  const needsFormFooter = hasNonButtonInputControls(parameters)

  if (pluginOptions.length > 0) {
    if (!needsFormFooter) return pluginOptions
    const hasCancel = pluginOptions.some((o) => o.approved === false)
    return hasCancel ? pluginOptions : [...pluginOptions, DEFAULT_HUMAN_CANCEL_OPTION]
  }

  if (needsFormFooter) {
    return [DEFAULT_HUMAN_SUBMIT_OPTION, DEFAULT_HUMAN_CANCEL_OPTION]
  }

  return DEFAULT_HUMAN_OPTION_BUTTONS
}
