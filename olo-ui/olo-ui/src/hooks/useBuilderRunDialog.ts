/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { useCallback, useEffect, useRef, useState } from 'react'
import type { RunEventDto } from '../api/oloRuntime'
import {
  catalogWorkflowTypes,
  findCatalogQueue,
} from '../lib/temporalCatalog'
import { catalogStore } from '../store/catalogStore'
import { workflowConfigurationStore } from '../store/workflowConfigurationStore'
import { cancelBuilderRun, executeBuilderRun } from '../lib/builderRunExecution'
import { useBuilderRunDialogSelection } from './useBuilderRunDialogSelection'
import { useBuilderHumanInput } from './useBuilderHumanInput'

export function useBuilderRunDialog(
  open: boolean,
  initialWorkflowId: string,
  initialTaskQueue: string,
  tenantId: string,
) {
  const catalog = catalogStore((s) => s.catalog)
  const catalogLoading = catalogStore((s) => s.loading)
  const workflows = workflowConfigurationStore((s) => s.workflows)
  const workflowsLoading = workflowConfigurationStore((s) => s.loading)

  const [running, setRunning] = useState(false)
  const [cancelling, setCancelling] = useState(false)
  const [logLines, setLogLines] = useState<string[]>([])
  const [finalResponse, setFinalResponse] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [runEvents, setRunEvents] = useState<RunEventDto[]>([])
  const [activeRunId, setActiveRunId] = useState<string | null>(null)
  const [runStatus, setRunStatus] = useState<string | null>(null)
  const logRef = useRef<HTMLDivElement>(null)
  const abortRef = useRef<(() => void) | null>(null)
  const pollRef = useRef<number | null>(null)
  const eventsRef = useRef<RunEventDto[]>([])
  const activeRunIdRef = useRef<string | null>(null)

  const selection = useBuilderRunDialogSelection(
    open,
    catalog,
    catalogLoading,
    workflows,
    workflowsLoading,
    initialTaskQueue,
    initialWorkflowId,
  )

  const queueDefinition = findCatalogQueue(catalog, selection.selectedQueue)
  const workflowType = queueDefinition?.workflowType ?? catalogWorkflowTypes(catalog)[0]?.id ?? 'olo'

  const stopRun = useCallback(() => {
    abortRef.current?.()
    abortRef.current = null
    if (pollRef.current != null) {
      window.clearInterval(pollRef.current)
      pollRef.current = null
    }
  }, [])

  const appendLog = useCallback((line: string) => {
    setLogLines((prev) => [...prev, line])
  }, [])

  const humanInput = useBuilderHumanInput(
    runEvents,
    activeRunId,
    open,
    running,
    runStatus,
    setError,
    appendLog,
  )

  useEffect(() => {
    if (logRef.current) {
      logRef.current.scrollTop = logRef.current.scrollHeight
    }
  }, [logLines])

  useEffect(() => {
    if (!open) {
      stopRun()
      setRunning(false)
      setCancelling(false)
      setLogLines([])
      setFinalResponse('')
      setError(null)
      setRunEvents([])
      setActiveRunId(null)
      setRunStatus(null)
      eventsRef.current = []
      activeRunIdRef.current = null
      return
    }
    return () => stopRun()
  }, [open, stopRun])

  const executionCallbacks = {
    appendLog,
    setRunning,
    setCancelling,
    setError,
    setFinalResponse,
    setLogLines,
    stopRun,
    setRunEvents,
    setActiveRunId,
    setRunStatus,
  }

  const handleRun = async (workflowLabel: string, messageOverride?: string) => {
    const content = (messageOverride ?? selection.prompt).trim()
    const taskQueue = selection.selectedQueue.trim()
    const workflowId = selection.selectedWorkflowId.trim()
    if (!content || running || !taskQueue || !workflowId) return

    await executeBuilderRun(
      {
        content,
        taskQueue,
        workflowId,
        workflowLabel,
        workflowType,
        tenantId,
      },
      { abortRef, pollRef, eventsRef, activeRunIdRef },
      executionCallbacks,
    )
  }

  const handleCancel = async () => {
    const runId = activeRunIdRef.current
    if (!runId || !running || cancelling) return
    await cancelBuilderRun(runId, { abortRef, pollRef, eventsRef, activeRunIdRef }, executionCallbacks)
  }

  return {
    catalog,
    catalogLoading,
    workflows,
    workflowsLoading,
    selectedQueue: selection.selectedQueue,
    selectedWorkflowId: selection.selectedWorkflowId,
    setSelectedWorkflowId: selection.setSelectedWorkflowId,
    selectedWorkflow: selection.selectedWorkflow,
    prompt: selection.prompt,
    setPrompt: selection.setPrompt,
    running,
    cancelling,
    runStatus,
    logLines,
    finalResponse,
    error,
    logRef,
    workflowType,
    handleQueueChange: selection.handleQueueChange,
    handleRun,
    handleCancel,
    queueWorkflows: selection.queueWorkflows,
    humanInput,
  }
}
