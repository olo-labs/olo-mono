/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { describe, expect, it } from 'vitest'
import type { RunEventDto } from '../api/oloRuntime'
import { findPendingHumanEvent, syntheticHumanWaitingEvent } from './builderHumanStep'

function humanWaiting(runId: string, seq = 10_001): RunEventDto {
  return {
    runId,
    nodeId: 'human-approve',
    nodeType: 'HUMAN',
    status: 'WAITING',
    sequenceNumber: seq,
    timestamp: seq,
    output: { status: 'HUMAN_WAITING', approvalStatus: 'waiting' },
    metadata: { phase: 'human-wait' },
  }
}

function temporalCompleted(runId: string): RunEventDto {
  return {
    runId,
    nodeId: 'root',
    nodeType: 'SYSTEM',
    status: 'COMPLETED',
    sequenceNumber: 99,
    timestamp: 99,
    output: { source: 'temporal', response: 'partial' },
  }
}

describe('findPendingHumanEvent', () => {
  it('returns human wait even when a temporal SYSTEM COMPLETED is present', () => {
    const runId = 'run-1'
    const events = [temporalCompleted(runId), humanWaiting(runId)]
    expect(findPendingHumanEvent(events, runId)?.nodeId).toBe('human-approve')
  })

  it('resolves run id from events when activeRunId is null (builder only)', () => {
    const events = [humanWaiting('run-2')]
    expect(findPendingHumanEvent(events, null, { allowRunIdFromEvents: true })?.runId).toBe('run-2')
  })

  it('returns null after human completed for the same node', () => {
    const runId = 'run-3'
    const events: RunEventDto[] = [
      humanWaiting(runId),
      {
        runId,
        nodeId: 'human-approve',
        nodeType: 'HUMAN',
        status: 'COMPLETED',
        sequenceNumber: 10_002,
        timestamp: 10_002,
        output: { approvalStatus: 'approved' },
      },
    ]
    expect(findPendingHumanEvent(events, runId)).toBeNull()
  })

  it('detects human wait from output markers without strict node type', () => {
    const runId = 'run-4'
    const events: RunEventDto[] = [
      {
        runId,
        nodeId: 'admin-approve',
        nodeType: 'ADMIN',
        status: 'WAITING',
        sequenceNumber: 5,
        timestamp: 5,
        output: { status: 'HUMAN_WAITING', approvalStatus: 'waiting' },
      },
    ]
    expect(findPendingHumanEvent(events, runId)?.nodeId).toBe('admin-approve')
  })
})

describe('syntheticHumanWaitingEvent', () => {
  it('creates a placeholder event for waiting_human API status', () => {
    const ev = syntheticHumanWaitingEvent('run-5')
    expect(ev.runId).toBe('run-5')
    expect(ev.nodeType).toBe('HUMAN')
    expect(ev.status).toBe('WAITING')
  })
})
