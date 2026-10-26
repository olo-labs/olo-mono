/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { useEffect, useMemo, useRef, useState } from 'react'
import { DEFAULT_BUILDER_RUN_PROMPT } from '../lib/builderRunPrompts'
import {
  catalogQueues,
  resolveInitialRunSelection,
  workflowsForQueue,
} from '../lib/temporalCatalog'
import { catalogStore } from '../store/catalogStore'
import { workflowConfigurationStore } from '../store/workflowConfigurationStore'
import type { StudioCatalog } from '../types/catalog'
import type { WorkflowSummary } from '../types/workflow'

export function useBuilderRunDialogSelection(
  open: boolean,
  catalog: StudioCatalog | null,
  catalogLoading: boolean,
  workflows: WorkflowSummary[],
  workflowsLoading: boolean,
  initialTaskQueue: string,
  initialWorkflowId: string,
) {
  const [selectedQueue, setSelectedQueue] = useState('')
  const [selectedWorkflowId, setSelectedWorkflowId] = useState('')
  const [prompt, setPrompt] = useState('')
  const selectionInitializedRef = useRef(false)

  const queueWorkflows = useMemo(
    () => workflowsForQueue(workflows, selectedQueue),
    [workflows, selectedQueue],
  )
  const selectedWorkflow = useMemo(
    () => queueWorkflows.find((workflow) => workflow.id === selectedWorkflowId),
    [queueWorkflows, selectedWorkflowId],
  )

  useEffect(() => {
    if (!open) {
      selectionInitializedRef.current = false
      setSelectedQueue('')
      setSelectedWorkflowId('')
      setPrompt('')
      return
    }
    void catalogStore.getState().loadCatalog()
    void workflowConfigurationStore.getState().loadWorkflows()
  }, [open])

  useEffect(() => {
    if (!open || catalogLoading || workflowsLoading || !catalog) return
    const queues = catalogQueues(catalog)
    if (queues.length === 0) return

    const initial = resolveInitialRunSelection(catalog, workflows, initialTaskQueue, initialWorkflowId)
    if (initial) {
      setSelectedQueue(initial.queueName)
      setSelectedWorkflowId(initial.workflowId)
    } else if (!selectedQueue) {
      const queueName = queues[0].name
      setSelectedQueue(queueName)
      const firstForQueue = workflowsForQueue(workflows, queueName)[0] ?? workflows[0]
      if (firstForQueue?.id) setSelectedWorkflowId(firstForQueue.id)
    } else if (!selectedWorkflowId && workflows.length > 0) {
      const firstForQueue = workflowsForQueue(workflows, selectedQueue)[0] ?? workflows[0]
      if (firstForQueue?.id) setSelectedWorkflowId(firstForQueue.id)
    }

    if (!selectionInitializedRef.current) {
      setPrompt((current) => current || DEFAULT_BUILDER_RUN_PROMPT)
      selectionInitializedRef.current = true
    }
  }, [
    open, catalog, catalogLoading, workflowsLoading, workflows,
    initialTaskQueue, initialWorkflowId, selectedQueue, selectedWorkflowId,
  ])

  const handleQueueChange = (queueName: string) => {
    setSelectedQueue(queueName)
    const nextWorkflow = workflowsForQueue(workflows, queueName)[0]
    setSelectedWorkflowId(nextWorkflow?.id ?? '')
  }

  return {
    selectedQueue,
    selectedWorkflowId,
    setSelectedWorkflowId,
    selectedWorkflow,
    prompt,
    setPrompt,
    queueWorkflows,
    handleQueueChange,
  }
}
