/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { describe, expect, it } from 'vitest'
import {
  EMPTY_RESPONSE_MESSAGE,
  fallbackResponseMessage,
  isRunTerminalFromApi,
  isWorkflowFinished,
  normalizeResponseText,
  pickResponseFromEvents,
  resolvePersistedAssistantContent,
} from './assistantResponse'
import type { RunEventDto } from '../api/chatApi'

function runEvent(event: Omit<RunEventDto, 'parentNodeId'> & { parentNodeId?: string | null }): RunEventDto {
  return { parentNodeId: null, ...event }
}

describe('pickResponseFromEvents', () => {
  it('prefers WORKFLOW_RESULT over later empty temporal SYSTEM event', () => {
    const events: RunEventDto[] = [
      runEvent({
        runId: 'r1',
        nodeId: 'kernel',
        nodeType: 'SYSTEM',
        status: 'COMPLETED',
        timestamp: 1,
        output: { status: 'WORKFLOW_RESULT', response: 'workflow answer' },
        metadata: { phase: 'kernel-result' },
      }),
      runEvent({
        runId: 'r1',
        nodeId: 'root',
        nodeType: 'SYSTEM',
        status: 'COMPLETED',
        timestamp: 2,
        output: { source: 'temporal' },
      }),
    ]
    expect(pickResponseFromEvents(events)).toBe('workflow answer')
  })
})

describe('normalizeResponseText', () => {
  it('returns null for metadata-only json', () => {
    expect(normalizeResponseText('{"source":"temporal"}')).toBeNull()
    expect(normalizeResponseText('hello')).toBe('hello')
  })

  it('formats Java map conversation-store summaries', () => {
    expect(
      normalizeResponseText(
        '{storedTurns=2, sessionId=5ad431c5-b7cb-410c-a6ba-740a660a6882, storagePath=/app/shared/olo-core/tools/demo-data/conversations/5ad431c5-b7cb-410c-a6ba-740a660a6882.json, summary=user: who stores json assistant: JSON data is typically stored in files or databases.}',
      ),
    ).toBe('User: who stores json\n\nAssistant: JSON data is typically stored in files or databases.')
  })
})

describe('isWorkflowFinished', () => {
  it('ignores CONTEXT_READY-only completion', () => {
    const events: RunEventDto[] = [
      runEvent({
        runId: 'r1',
        nodeId: 'kernel',
        nodeType: 'SYSTEM',
        status: 'COMPLETED',
        timestamp: 1,
        output: { status: 'CONTEXT_READY', queue: 'agent' },
      }),
    ]
    expect(isWorkflowFinished(events)).toBe(false)
  })

  it('detects WORKFLOW_RESULT completion', () => {
    const events: RunEventDto[] = [
      runEvent({
        runId: 'r1',
        nodeId: 'kernel',
        nodeType: 'SYSTEM',
        status: 'COMPLETED',
        timestamp: 1,
        output: { status: 'WORKFLOW_RESULT', response: 'done' },
      }),
    ]
    expect(isWorkflowFinished(events)).toBe(true)
  })
})

describe('isRunTerminalFromApi', () => {
  it('ignores completed status after CONTEXT_READY only', () => {
    const events: RunEventDto[] = [
      runEvent({
        runId: 'r1',
        nodeId: 'kernel',
        nodeType: 'SYSTEM',
        status: 'COMPLETED',
        timestamp: 1,
        output: { status: 'CONTEXT_READY' },
      }),
    ]
    expect(isRunTerminalFromApi('completed', events)).toBe(false)
    expect(isRunTerminalFromApi('completed', [
      ...events,
      runEvent({
        runId: 'r1',
        nodeId: 'kernel',
        nodeType: 'SYSTEM',
        status: 'COMPLETED',
        timestamp: 2,
        output: { status: 'WORKFLOW_RESULT', response: 'ok' },
      }),
    ])).toBe(true)
  })
})

describe('resolvePersistedAssistantContent', () => {
  it('returns null for empty assistant while run is in progress', () => {
    expect(
      resolvePersistedAssistantContent('', {
        runId: 'r1',
        activeRunId: 'r1',
        sending: true,
        events: [],
      }),
    ).toBeNull()
  })
})

describe('fallbackResponseMessage', () => {
  it('uses chat empty copy only when terminal', () => {
    expect(fallbackResponseMessage('completed')).toBe(EMPTY_RESPONSE_MESSAGE)
  })
})
