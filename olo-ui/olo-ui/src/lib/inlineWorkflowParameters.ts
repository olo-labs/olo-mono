/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { presetParametersForWorkflow } from './catalogLookup'
import { workflowParameterToDescriptor } from './workflowParameterHelpers'
import type { StudioCatalog } from '../types/catalog'
import type { WorkflowDocument } from '../types/workflow'

export function sortedWorkflowParameters(workflow: WorkflowDocument, catalog: StudioCatalog | null) {
  const fromWorkflow = Object.entries(workflow.parameters ?? {}).map(([id, param]) =>
    workflowParameterToDescriptor(id, param),
  )
  if (fromWorkflow.length > 0) {
    return fromWorkflow.sort((a, b) => (a.ui?.order ?? 0) - (b.ui?.order ?? 0))
  }
  return [...presetParametersForWorkflow(catalog, workflow)].sort(
    (a, b) => (a.ui?.order ?? 0) - (b.ui?.order ?? 0),
  )
}
