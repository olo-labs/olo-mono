/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { RunEventDto } from '../api/oloRuntime'
import { shouldFinishBuilderRun } from './builderRunExecution'

function temporalCheckpoint(runId: string): RunEventDto {
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

function humanWaiting(runId: string): RunEventDto {
  return {
    runId,
    nodeId: 'human-approve',
    nodeType: 'HUMAN',
    status: 'WAITING',
    sequenceNumber: 10_001,
    timestamp: 10_001,
    output: { status: 'HUMAN_WAITING', approvalStatus: 'waiting' },
    metadata: { phase: 'human-wait' },
  }
}

function workflowResult(runId: string): RunEventDto {
  return {
    runId,
    nodeId: 'root',
    nodeType: 'SYSTEM',
    status: 'COMPLETED',
    sequenceNumber: 200,
    timestamp: 200,
    output: { status: 'WORKFLOW_RESULT', response: 'done' },
  }
}

describe('shouldFinishBuilderRun', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-01-01T00:00:00.000Z'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('does not finish while API status is running or waiting_human', () => {
    const runId = 'run-1'
    const events = [temporalCheckpoint(runId)]
    expect(shouldFinishBuilderRun(runId, 'running', events)).toBe(false)
    expect(shouldFinishBuilderRun(runId, 'waiting_human', events)).toBe(false)
  })

  it('does not finish on completed+temporal before human event arrives', () => {
    const runId = 'run-2'
    const events = [temporalCheckpoint(runId)]
    const checkpointAt = Date.now()
    expect(shouldFinishBuilderRun(runId, 'completed', events, checkpointAt)).toBe(false)
  })

  it('does not finish when human wait is present', () => {
    const runId = 'run-3'
    const events = [temporalCheckpoint(runId), humanWaiting(runId)]
    expect(shouldFinishBuilderRun(runId, 'completed', events, Date.now())).toBe(false)
  })

  it('finishes on definitive workflow result', () => {
    const runId = 'run-4'
    const events = [temporalCheckpoint(runId), workflowResult(runId)]
    expect(shouldFinishBuilderRun(runId, 'completed', events, Date.now())).toBe(true)
  })

  it('finishes after grace period when only temporal checkpoint remains', () => {
    const runId = 'run-5'
    const events = [temporalCheckpoint(runId)]
    const checkpointAt = Date.now()
    vi.advanceTimersByTime(8_000)
    expect(shouldFinishBuilderRun(runId, 'completed', events, checkpointAt)).toBe(true)
  })
})
