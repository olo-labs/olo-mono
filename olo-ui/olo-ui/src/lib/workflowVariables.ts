/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { WorkflowVariable } from '../types/workflow'

export const VARIABLE_SCOPES = [
  'LOCAL',
  'READONLY_EXTERNAL',
  'EXTERNAL',
  'GLOBAL',
  'CREDENTIAL',
] as const

export type VariableScope = (typeof VARIABLE_SCOPES)[number]

export const VARIABLE_SCOPE_LABELS: Record<VariableScope, string> = {
  LOCAL: 'Local',
  READONLY_EXTERNAL: 'Read-only external',
  EXTERNAL: 'External',
  GLOBAL: 'Global',
  CREDENTIAL: 'Credential',
}

export const VARIABLE_SCOPE_HINTS: Record<VariableScope, string> = {
  LOCAL: 'Workflow-local state only',
  READONLY_EXTERNAL: 'Provided by caller; not writable by the workflow',
  EXTERNAL: 'Caller input mapped into workflow state',
  GLOBAL: 'Shared across workflows in the same execution context',
  CREDENTIAL: 'Sensitive credential reference',
}

export function isVariableScope(value: string): value is VariableScope {
  return (VARIABLE_SCOPES as readonly string[]).includes(value)
}

export function normalizeVariableScope(value: unknown): VariableScope {
  if (typeof value !== 'string' || !value.trim()) {
    return 'LOCAL'
  }
  const normalized = value.trim().toUpperCase()
  if (isVariableScope(normalized)) {
    return normalized
  }
  return 'LOCAL'
}

export function normalizeWorkflowVariable(variable: WorkflowVariable): WorkflowVariable {
  return {
    ...variable,
    scope: normalizeVariableScope(variable.scope),
  }
}
