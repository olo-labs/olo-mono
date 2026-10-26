/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { describe, expect, it } from 'vitest'
import type { RunEventDto } from '../api/oloRuntime'
import { isDefinitiveWorkflowFinished, isWorkflowFinished } from './assistantResponse'

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

describe('workflow finished detection', () => {
  it('treats temporal checkpoint as finished for preview paths', () => {
    expect(isWorkflowFinished([temporalCheckpoint('run-1')])).toBe(true)
  })

  it('does not treat temporal checkpoint alone as definitive completion', () => {
    expect(isDefinitiveWorkflowFinished([temporalCheckpoint('run-1')])).toBe(false)
  })

  it('treats WORKFLOW_RESULT as definitive completion', () => {
    expect(isDefinitiveWorkflowFinished([workflowResult('run-1')])).toBe(true)
  })
})
