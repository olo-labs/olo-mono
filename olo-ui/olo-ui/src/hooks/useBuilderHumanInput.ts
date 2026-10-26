/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { useCallback, useEffect, useMemo, useState } from 'react'
import { submitRuntimeHumanInput, type RunEventDto } from '../api/oloRuntime'
import {
  buildHumanStepHistoryText,
  findPendingHumanEvent,
  humanStepEventKey,
  humanStepOptionsFromEvent,
  humanStepParametersFromEvent,
  humanStepPluginNameFromEvent,
  humanStepPromptFromEvent,
  humanStepUsesPluginForm,
  resolveHumanStepFooterActions,
  syntheticHumanWaitingEvent,
  type HumanStepParameter,
} from '../lib/builderHumanStep'
import {
  approvalTogglesAllowApprove,
  defaultFieldValue,
  isParameterValueValid,
  resolveHumanInputWidget,
} from '../lib/humanInputWidget'

function stringValue(v: unknown): string | null {
  return typeof v === 'string' && v.trim().length > 0 ? v.trim() : null
}

function initialFieldValues(parameters: HumanStepParameter[]): Record<string, string> {
  const values: Record<string, string> = {}
  for (const param of parameters) {
    values[param.id] = defaultFieldValue(param)
  }
  return values
}

function buildPluginSubmitMessage(
  parameters: HumanStepParameter[],
  fieldValues: Record<string, string>,
): string {
  const payload: Record<string, string | boolean> = {}
  for (const param of parameters) {
    const raw = fieldValues[param.id] ?? ''
    const trimmed = raw.trim()
    const widget = resolveHumanInputWidget(param)
    if (widget === 'BOOLEAN' || widget === 'APPROVAL_TOGGLE') {
      if (trimmed === 'true' || trimmed === 'false') {
        payload[param.id] = trimmed === 'true'
      } else if (param.required) {
        payload[param.id] = false
      }
      continue
    }
    if (trimmed) {
      payload[param.id] = trimmed
    } else if (param.required) {
      payload[param.id] = ''
    }
  }
  return JSON.stringify(payload)
}

export function useBuilderHumanInput(
  runEvents: RunEventDto[],
  activeRunId: string | null,
  dialogActive: boolean,
  running: boolean,
  runStatus: string | null,
  setError: React.Dispatch<React.SetStateAction<string | null>>,
  appendLog: (line: string) => void,
) {
  const [humanFieldValues, setHumanFieldValues] = useState<Record<string, string>>({})
  const [submittingHumanInput, setSubmittingHumanInput] = useState(false)
  const [answeredHumanStepKey, setAnsweredHumanStepKey] = useState<string | null>(null)

  useEffect(() => {
    setAnsweredHumanStepKey(null)
  }, [activeRunId, dialogActive])

  const runContextActive = dialogActive && !!activeRunId?.trim() && (running || runStatus === 'waiting_human')

  const rawPendingHumanEvent = runContextActive
    ? findPendingHumanEvent(runEvents, activeRunId) ??
      (runStatus === 'waiting_human' && activeRunId
        ? syntheticHumanWaitingEvent(activeRunId)
        : null)
    : null
  const rawPendingKey = humanStepEventKey(rawPendingHumanEvent)
  const pendingHumanEvent =
    rawPendingHumanEvent && rawPendingKey !== answeredHumanStepKey ? rawPendingHumanEvent : null

  const humanPromptMessage = humanStepPromptFromEvent(pendingHumanEvent ?? undefined)
  const humanStepOptions = humanStepOptionsFromEvent(pendingHumanEvent ?? undefined)
  const humanStepParameters = useMemo(
    () => humanStepParametersFromEvent(pendingHumanEvent ?? undefined),
    [pendingHumanEvent],
  )
  const humanPluginName = humanStepPluginNameFromEvent(pendingHumanEvent ?? undefined)
  const usesPluginForm = humanStepUsesPluginForm(pendingHumanEvent ?? undefined)
  const humanTaskId =
    stringValue(pendingHumanEvent?.metadata?.taskId) ??
    stringValue(pendingHumanEvent?.output?.taskId) ??
    pendingHumanEvent?.nodeId ??
    'human-input'

  const humanStepFooterActions = useMemo(
    () => resolveHumanStepFooterActions(humanStepParameters, humanStepOptions),
    [humanStepParameters, humanStepOptions],
  )

  useEffect(() => {
    if (!pendingHumanEvent) {
      setHumanFieldValues({})
      return
    }
    setHumanFieldValues(initialFieldValues(humanStepParameters))
  }, [pendingHumanEvent?.nodeId, pendingHumanEvent?.sequenceNumber, pendingHumanEvent, humanStepParameters])

  const setHumanFieldValue = useCallback((id: string, value: string) => {
    setHumanFieldValues((prev) => ({ ...prev, [id]: value }))
  }, [])

  const pluginFormValid = useMemo(() => {
    if (humanStepParameters.length === 0) return true
    const fieldsValid = humanStepParameters.every((param) =>
      isParameterValueValid(param, humanFieldValues[param.id] ?? ''),
    )
    return fieldsValid && approvalTogglesAllowApprove(humanStepParameters, humanFieldValues)
  }, [humanStepParameters, humanFieldValues])

  const handleSubmitHumanInput = useCallback(
    async (approved: boolean, message: string) => {
      if (!pendingHumanEvent?.runId) return
      const dismissKey = humanStepEventKey(pendingHumanEvent)
      if (dismissKey) setAnsweredHumanStepKey(dismissKey)
      setSubmittingHumanInput(true)
      setError(null)
      try {
        const resolvedMessage =
          humanStepParameters.length > 0
            ? buildPluginSubmitMessage(humanStepParameters, humanFieldValues)
            : message
        const historyText =
          humanStepParameters.length > 0
            ? buildHumanStepHistoryText(humanStepParameters, humanFieldValues, message)
            : message
        await submitRuntimeHumanInput(pendingHumanEvent.runId, {
          approved,
          message: resolvedMessage,
          historyText,
        })
        appendLog(
          approved
            ? `Human input submitted (${humanTaskId})`
            : `Human input cancelled (${humanTaskId})`,
        )
        setHumanFieldValues(initialFieldValues(humanStepParameters))
      } catch (e: unknown) {
        setAnsweredHumanStepKey(null)
        const msg = e instanceof Error ? e.message : String(e)
        setError(msg)
        appendLog(`ERROR: ${msg}`)
      } finally {
        setSubmittingHumanInput(false)
      }
    },
    [
      pendingHumanEvent,
      setError,
      appendLog,
      humanStepParameters,
      humanFieldValues,
      humanTaskId,
    ],
  )

  return {
    pendingHumanEvent,
    humanPromptMessage,
    humanStepFooterActions,
    humanStepParameters,
    humanPluginName,
    humanTaskId,
    usesPluginForm,
    humanFieldValues,
    setHumanFieldValue,
    pluginFormValid,
    submittingHumanInput,
    handleSubmitHumanInput,
  }
}
