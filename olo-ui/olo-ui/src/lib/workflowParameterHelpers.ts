/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { CatalogParameter } from '../types/catalog'
import type { WorkflowDocument, WorkflowParameter } from '../types/workflow'

export function workflowParameterToDescriptor(
  id: string,
  param: WorkflowParameter,
): CatalogParameter {
  return {
    id,
    type: param.type ?? 'string',
    label: param.label ?? id,
    description: param.description,
    required: param.required,
    validation: param.validation,
    ui: param.ui,
  }
}

export function updateWorkflowParameterValue(
  workflow: WorkflowDocument,
  paramId: string,
  value: unknown,
): WorkflowDocument {
  const existing = workflow.parameters?.[paramId] ?? { type: 'string' }
  return {
    ...workflow,
    parameters: {
      ...workflow.parameters,
      [paramId]: {
        ...existing,
        defaultValue: value,
      },
    },
  }
}
