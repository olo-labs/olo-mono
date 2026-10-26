/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { useCallback, useEffect, useMemo, useState } from 'react'
import { listMessages, submitHumanInput } from '../api/chatApi'
import type { RunEventDto } from '../api/chatApi'
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
  type HumanStepParameter,
} from '../lib/chatHumanStep'
import { isActiveRunForSession } from '../store/runEvents'
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

function buildPluginSubmitMessage(parameters: HumanStepParameter[], fieldValues: Record<string, string>): string {
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

export function useChatHumanInput(
  runEvents: RunEventDto[],
  activeRunId: string | null,
  sessionId: string | null,
  sessionActive: boolean,
  setMessages: React.Dispatch<React.SetStateAction<import('../api/chatApi').ChatMessageDto[]>>,
  setError: React.Dispatch<React.SetStateAction<string | null>>
) {
  const [humanFieldValues, setHumanFieldValues] = useState<Record<string, string>>({})
  const [submittingHumanInput, setSubmittingHumanInput] = useState(false)
  const [answeredHumanStepKey, setAnsweredHumanStepKey] = useState<string | null>(null)

  useEffect(() => {
    setAnsweredHumanStepKey(null)
  }, [activeRunId, sessionId])

  const runBelongsToActiveSession =
    sessionActive && isActiveRunForSession(sessionId, activeRunId) && !!activeRunId?.trim()

  const rawPendingHumanEvent = runBelongsToActiveSession
    ? findPendingHumanEvent(runEvents, activeRunId)
    : null
  const rawPendingKey = humanStepEventKey(rawPendingHumanEvent)
  const pendingHumanEvent =
    rawPendingHumanEvent && rawPendingKey !== answeredHumanStepKey ? rawPendingHumanEvent : null

  const humanPromptMessage = humanStepPromptFromEvent(pendingHumanEvent ?? undefined)
  const humanStepOptions = humanStepOptionsFromEvent(pendingHumanEvent ?? undefined)
  const humanStepParameters = useMemo(
    () => humanStepParametersFromEvent(pendingHumanEvent ?? undefined),
    [pendingHumanEvent]
  )
  const humanPluginName = humanStepPluginNameFromEvent(pendingHumanEvent ?? undefined)
  const usesPluginForm = humanStepUsesPluginForm(pendingHumanEvent ?? undefined)
  const humanTaskId =
    stringValue(pendingHumanEvent?.metadata?.taskId) ??
    stringValue(pendingHumanEvent?.output?.taskId) ??
    pendingHumanEvent?.nodeId ??
    'human-input'
  const humanInputType = humanStepParameters.length > 0 ? 'plugin' : 'options'

  const humanStepFooterActions = useMemo(
    () => resolveHumanStepFooterActions(humanStepParameters, humanStepOptions),
    [humanStepParameters, humanStepOptions]
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
      isParameterValueValid(param, humanFieldValues[param.id] ?? '')
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
        await submitHumanInput(pendingHumanEvent.runId, {
          approved,
          message: resolvedMessage,
          historyText,
        })
        if (sessionId) {
          listMessages(sessionId).then(setMessages).catch(() => {})
        }
        setHumanFieldValues(initialFieldValues(humanStepParameters))
      } catch (e: unknown) {
        setAnsweredHumanStepKey(null)
        const msg = e instanceof Error ? e.message : String(e)
        setError(msg)
      } finally {
        setSubmittingHumanInput(false)
      }
    },
    [pendingHumanEvent, sessionId, setMessages, setError, humanStepParameters, humanFieldValues]
  )

  return {
    pendingHumanEvent,
    humanPromptMessage,
    humanStepOptions,
    humanStepFooterActions,
    humanStepParameters,
    humanPluginName,
    humanTaskId,
    humanInputType,
    usesPluginForm,
    humanFieldValues,
    setHumanFieldValue,
    pluginFormValid,
    submittingHumanInput,
    handleSubmitHumanInput,
  }
}
