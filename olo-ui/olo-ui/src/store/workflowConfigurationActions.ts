/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { WorkflowConfigurationState } from './workflowConfigurationStore'
import { createWorkflowLoadActions } from './workflowConfigurationLoadActions'
import { createWorkflowMutateActions } from './workflowConfigurationMutateActions'

type SetState = (
  partial:
    | Partial<WorkflowConfigurationState>
    | ((state: WorkflowConfigurationState) => Partial<WorkflowConfigurationState>),
) => void
type GetState = () => WorkflowConfigurationState

export function createWorkflowConfigurationActions(set: SetState, get: GetState) {
  return {
    ...createWorkflowLoadActions(set, get),
    ...createWorkflowMutateActions(set, get),
  }
}
