/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { CatalogTemporalQueue, CatalogWorkflowType, StudioCatalog } from '../types/catalog'
import type { WorkflowSummary } from '../types/workflow'

export function catalogQueues(catalog: StudioCatalog | null | undefined): CatalogTemporalQueue[] {
  return catalog?.queues ?? []
}

export function catalogWorkflowTypes(
  catalog: StudioCatalog | null | undefined,
): CatalogWorkflowType[] {
  return catalog?.workflowTypes ?? []
}

export function findCatalogQueue(
  catalog: StudioCatalog | null | undefined,
  queueName: string,
): CatalogTemporalQueue | undefined {
  return catalogQueues(catalog).find((queue) => queue.name === queueName)
}

export function findCatalogWorkflowType(
  catalog: StudioCatalog | null | undefined,
  workflowTypeId: string,
): CatalogWorkflowType | undefined {
  return catalogWorkflowTypes(catalog).find((type) => type.id === workflowTypeId)
}

export function workflowsForQueue(
  workflows: WorkflowSummary[],
  queueName: string,
): WorkflowSummary[] {
  const queue = queueName.trim()
  if (!queue) return []
  return workflows.filter((workflow) => workflow.queue?.trim() === queue)
}

export function resolveInitialRunSelection(
  catalog: StudioCatalog | null | undefined,
  workflows: WorkflowSummary[],
  initialQueue: string,
  initialWorkflowId: string,
): {
  queueName: string
  workflowId: string
  label: string
  workflowType: string
} | null {
  const queues = catalogQueues(catalog)
  if (queues.length === 0) return null

  const queueFromDraft = initialQueue.trim()
  const workflowFromDraft = initialWorkflowId.trim()

  if (queueFromDraft) {
    const queue = findCatalogQueue(catalog, queueFromDraft) ?? queues[0]
    const queueWorkflows = workflowsForQueue(workflows, queue.name)
    const workflow =
      (workflowFromDraft
        ? queueWorkflows.find((entry) => entry.id === workflowFromDraft)
        : undefined) ??
      queueWorkflows[0] ??
      (workflowFromDraft
        ? workflows.find((entry) => entry.id === workflowFromDraft)
        : undefined)
    if (!workflow?.id) return null
    return {
      queueName: queue.name,
      workflowId: workflow.id,
      label: workflow.label ?? workflow.id,
      workflowType: queue.workflowType,
    }
  }

  const firstQueue = queues[0]
  const firstWorkflow = workflowsForQueue(workflows, firstQueue.name)[0] ?? workflows[0]
  if (!firstWorkflow?.id) return null
  return {
    queueName: firstQueue.name,
    workflowId: firstWorkflow.id,
    label: firstWorkflow.label ?? firstWorkflow.id,
    workflowType: firstQueue.workflowType,
  }
}
