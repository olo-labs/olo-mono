/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import {
  cancelRuntimeRun,
  createRuntimeSession,
  getRuntimeRun,
  sendRuntimeMessage,
  streamRuntimeRunEvents,
  type RunEventDto,
} from '../api/oloRuntime'
import {
  fallbackResponseMessage,
  isDefinitiveWorkflowFinished,
  isWorkflowFinished,
  normalizeResponseText,
  pickResponseFromEvents,
} from './assistantResponse'
import { findPendingHumanEvent } from './builderHumanStep'
import { formatRunEventLogLine } from './runEventLog'
import { resolveWorkflowReturnText } from './builderRunResponse'

export interface BuilderRunExecutionRefs {
  abortRef: React.MutableRefObject<(() => void) | null>
  pollRef: React.MutableRefObject<number | null>
  eventsRef: React.MutableRefObject<RunEventDto[]>
  activeRunIdRef: React.MutableRefObject<string | null>
}

export interface BuilderRunExecutionCallbacks {
  appendLog: (line: string) => void
  setRunning: (running: boolean) => void
  setCancelling: (cancelling: boolean) => void
  setError: (error: string | null) => void
  setFinalResponse: (response: string) => void
  setLogLines: (lines: string[]) => void
  stopRun: () => void
  setRunEvents?: (events: RunEventDto[]) => void
  setActiveRunId?: (runId: string | null) => void
  setRunStatus?: (status: string | null) => void
}

/** Milliseconds to wait after a Temporal checkpoint before treating completed+temporal as terminal. */
const TEMPORAL_HUMAN_GRACE_MS = 8_000

function isTemporalCheckpointEvent(event: RunEventDto): boolean {
  if (event.nodeType?.toUpperCase() !== 'SYSTEM' || event.status?.toUpperCase() !== 'COMPLETED') {
    return false
  }
  const output = event.output as Record<string, unknown> | undefined
  return output?.source === 'temporal' && output?.status !== 'WORKFLOW_RESULT'
}

export function shouldFinishBuilderRun(
  runId: string,
  status: string | undefined,
  events: RunEventDto[],
  temporalCheckpointAt?: number | null,
): boolean {
  if (status === 'waiting_human' || status === 'running') return false
  if (findPendingHumanEvent(events, runId)) return false
  if (status === 'cancelled' || status === 'failed') return true
  if (status === 'completed') {
    if (isDefinitiveWorkflowFinished(events)) return true
    if (isWorkflowFinished(events)) {
      if (temporalCheckpointAt != null && Date.now() - temporalCheckpointAt < TEMPORAL_HUMAN_GRACE_MS) {
        return false
      }
      return true
    }
    return false
  }
  return false
}

export async function executeBuilderRun(
  {
    content,
    taskQueue,
    workflowId,
    workflowLabel,
    workflowType,
    tenantId,
  }: {
    content: string
    taskQueue: string
    workflowId: string
    workflowLabel: string
    workflowType: string
    tenantId: string
  },
  refs: BuilderRunExecutionRefs,
  callbacks: BuilderRunExecutionCallbacks,
): Promise<void> {
  const { abortRef, pollRef, eventsRef, activeRunIdRef } = refs
  const {
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
  } = callbacks

  stopRun()
  activeRunIdRef.current = null
  setActiveRunId?.(null)
  setRunStatus?.(null)
  setCancelling(false)
  setRunning(true)
  setError(null)
  setLogLines([])
  setFinalResponse('')
  eventsRef.current = []
  setRunEvents?.([])
  appendLog(`Starting workflow "${workflowLabel}" (queue: ${taskQueue}, type: ${workflowType})…`)

  try {
    const tenant = tenantId.trim() || 'default'
    const { sessionId } = await createRuntimeSession({
      tenantId: tenant,
      queueName: taskQueue,
      pipelineId: workflowId,
    })
    appendLog(`Session ${sessionId.slice(0, 8)}… created`)

    const { runId } = await sendRuntimeMessage(sessionId, content, { taskQueue })
    activeRunIdRef.current = runId
    setActiveRunId?.(runId)
    appendLog(`Run ${runId} started`)

    let temporalCheckpointAt: number | null = null

    abortRef.current = streamRuntimeRunEvents(
      runId,
      (event) => {
        const normalized = event.runId?.trim() ? event : { ...event, runId }
        if (isTemporalCheckpointEvent(normalized)) {
          temporalCheckpointAt = Date.now()
        }
        eventsRef.current = [...eventsRef.current, normalized]
        setRunEvents?.(eventsRef.current)
        appendLog(formatRunEventLogLine(normalized))
        const preview = normalizeResponseText(pickResponseFromEvents(eventsRef.current))
        if (preview) setFinalResponse(preview)
      },
      (err) => {
        const message = err instanceof Error ? err.message : 'Event stream error'
        setError(message)
        appendLog(`ERROR: ${message}`)
      },
    )

    pollRef.current = window.setInterval(() => {
      void (async () => {
        const run = await getRuntimeRun(runId)
        if (run?.status) setRunStatus?.(run.status)
        if (!run || !shouldFinishBuilderRun(runId, run.status, eventsRef.current, temporalCheckpointAt)) return
        if (findPendingHumanEvent(eventsRef.current, runId)) return
        if (pollRef.current != null) {
          window.clearInterval(pollRef.current)
          pollRef.current = null
        }
        await new Promise((resolve) => window.setTimeout(resolve, 800))
        stopRun()
        activeRunIdRef.current = null
        setActiveRunId?.(null)
        setRunStatus?.(null)
        setCancelling(false)
        if (run.status === 'cancelled') {
          setFinalResponse('Run cancelled.')
          appendLog('Run cancelled')
          setRunning(false)
          return
        }
        const workflowText = await resolveWorkflowReturnText(runId, sessionId, eventsRef.current)
        setFinalResponse(workflowText ?? fallbackResponseMessage(run.status))
        appendLog(`Run ${run.status}`)
        setRunning(false)
      })()
    }, 1500)
  } catch (e) {
    const message = e instanceof Error ? e.message : 'Run failed'
    setError(message)
    appendLog(`ERROR: ${message}`)
    activeRunIdRef.current = null
    setActiveRunId?.(null)
    setRunStatus?.(null)
    setCancelling(false)
    setRunning(false)
  }
}

export async function cancelBuilderRun(
  runId: string,
  refs: BuilderRunExecutionRefs,
  callbacks: Pick<
    BuilderRunExecutionCallbacks,
    'appendLog' | 'setCancelling' | 'setError' | 'setFinalResponse' | 'setRunning' | 'stopRun' | 'setActiveRunId' | 'setRunStatus'
  >,
): Promise<void> {
  const { appendLog, setCancelling, setError } = callbacks
  if (!runId.trim()) return

  setCancelling(true)
  appendLog('Cancelling run…')
  try {
    await cancelRuntimeRun(runId)
    appendLog('Cancel requested')
  } catch (e) {
    const message = e instanceof Error ? e.message : 'Cancel failed'
    if (message.includes('no longer in progress')) {
      appendLog('Run already finished')
      finishBuilderRun(refs, callbacks, { response: 'Run cancelled.', logLine: 'Run cancelled' })
      return
    }
    setError(message)
    appendLog(`ERROR: ${message}`)
    setCancelling(false)
    return
  }

  finishBuilderRun(refs, callbacks, { response: 'Run cancelled.', logLine: 'Run cancelled' })
}

function finishBuilderRun(
  refs: BuilderRunExecutionRefs,
  callbacks: Pick<
    BuilderRunExecutionCallbacks,
    'appendLog' | 'setCancelling' | 'setFinalResponse' | 'setRunning' | 'stopRun' | 'setActiveRunId' | 'setRunStatus'
  >,
  options?: { response?: string; logLine?: string },
): void {
  const { activeRunIdRef } = refs
  const { appendLog, setCancelling, setFinalResponse, setRunning, stopRun, setActiveRunId, setRunStatus } =
    callbacks
  stopRun()
  activeRunIdRef.current = null
  setActiveRunId?.(null)
  setRunStatus?.(null)
  setCancelling(false)
  setRunning(false)
  if (options?.response) setFinalResponse(options.response)
  if (options?.logLine) appendLog(options.logLine)
}
