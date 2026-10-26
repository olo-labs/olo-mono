/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { describe, expect, it } from 'vitest'
import type { RunEventDto } from '../api/chatApi'
import {
  buildHumanStepHistoryText,
  formatHumanStepReplyForDisplay,
  normalizeHumanStepHistoryContent,
  resolveHumanStepAssistantDisplay,
  DEFAULT_HUMAN_APPROVE_OPTION,
  DEFAULT_HUMAN_CANCEL_OPTION,
  DEFAULT_HUMAN_OPTION_BUTTONS,
  DEFAULT_HUMAN_SUBMIT_OPTION,
  findPendingHumanEvent,
  humanStepEventKey,
  resolveHumanStepFooterActions,
} from './chatHumanStep'

function humanWaiting(runId: string, seq = 1): RunEventDto {
  return {
    runId,
    nodeId: 'human-input',
    parentNodeId: null,
    nodeType: 'HUMAN',
    status: 'WAITING',
    sequenceNumber: seq,
    timestamp: seq,
    output: { inputType: 'options' },
  }
}

describe('normalizeHumanStepHistoryContent', () => {
  it('keeps only the prompt line when no operator reply is known yet', () => {
    const raw = 'User Input Step: Approve container restart\nApprove\nCancel'
    expect(normalizeHumanStepHistoryContent(raw)).toBe('User Input Step: Approve container restart')
  })

  it('shows prompt plus chosen operator reply', () => {
    const raw = 'User Input Step: Approve container restart\nApprove\nCancel'
    expect(normalizeHumanStepHistoryContent(raw, { chosenReply: 'Approve' })).toBe(
      'User Input Step: Approve container restart\nApprove'
    )
  })

  it('strips legacy <Options> marker lines', () => {
    const raw = 'User Input Step: Book ticket\n<Options>\nApprove\nCancel'
    expect(normalizeHumanStepHistoryContent(raw)).toBe('User Input Step: Book ticket')
  })
})

describe('resolveHumanStepAssistantDisplay', () => {
  it('merges the following user reply into the assistant human-step bubble', () => {
    const messages = [
      {
        messageId: 'a1',
        sessionId: 'session-1',
        role: 'assistant',
        content: 'User Input Step: Approve container restart\nApprove\nCancel',
        runId: 'run-1',
        createdAt: 1,
      },
      { messageId: 'u1', sessionId: 'session-1', role: 'user', content: 'Approve', runId: 'run-1', createdAt: 2 },
    ] as const
    expect(resolveHumanStepAssistantDisplay([...messages], 0, messages[0].content)).toBe(
      'User Input Step: Approve container restart\nApprove'
    )
  })
})

describe('buildHumanStepHistoryText', () => {
  it('uses operator field values for plugin forms', () => {
    expect(
      buildHumanStepHistoryText(
        [
          { id: 'approveRestart', type: 'boolean', ui: { widget: 'APPROVAL_TOGGLE' } },
          { id: 'containerId', type: 'string', ui: { widget: 'STRING' } },
        ],
        { approveRestart: 'true', containerId: 'c1' },
        'Submit'
      )
    ).toBe('Yes\nc1')
  })

  it('falls back to the clicked action label for options-only steps', () => {
    expect(buildHumanStepHistoryText([], {}, 'Approve container restart')).toBe('Approve container restart')
  })
})

describe('formatHumanStepReplyForDisplay', () => {
  it('returns persisted operator input as-is', () => {
    expect(formatHumanStepReplyForDisplay('Approve container restart')).toBe('Approve container restart')
    expect(formatHumanStepReplyForDisplay('my-container-id')).toBe('my-container-id')
    expect(formatHumanStepReplyForDisplay('Yes\nmy-container-id')).toBe('Yes\nmy-container-id')
  })
})

describe('humanStepEventKey', () => {
  it('builds a stable key per waiting event', () => {
    expect(humanStepEventKey(humanWaiting('run-1', 3))).toBe('run-1:human-input:3')
  })
})

describe('findPendingHumanEvent', () => {
  it('returns null without an active run id (no run-id fallback in chat)', () => {
    expect(findPendingHumanEvent([humanWaiting('run-1')], null)).toBeNull()
  })

  it('scopes to the active run id', () => {
    expect(findPendingHumanEvent([humanWaiting('run-1'), humanWaiting('run-2')], 'run-2')?.runId).toBe(
      'run-2'
    )
  })

  it('hides after workflow cancellation', () => {
    const events: RunEventDto[] = [
      humanWaiting('run-1'),
      {
        runId: 'run-1',
        nodeId: 'system',
        parentNodeId: null,
        nodeType: 'SYSTEM',
        status: 'FAILED',
        sequenceNumber: 2,
        timestamp: 2,
        output: { status: 'CANCELLED' },
      },
    ]
    expect(findPendingHumanEvent(events, 'run-1')).toBeNull()
  })

  it('still shows human wait when temporal completion event arrived early', () => {
    const events: RunEventDto[] = [
      {
        runId: 'run-1',
        nodeId: 'root',
        parentNodeId: null,
        nodeType: 'SYSTEM',
        status: 'COMPLETED',
        sequenceNumber: 2,
        timestamp: 2,
        output: { source: 'temporal' },
      },
      humanWaiting('run-1', 3),
    ]
    expect(findPendingHumanEvent(events, 'run-1')?.nodeId).toBe('human-input')
  })
})

describe('resolveHumanStepFooterActions', () => {
  it('defaults to approve and cancel buttons when no plugin options', () => {
    expect(resolveHumanStepFooterActions([], [])).toEqual(DEFAULT_HUMAN_OPTION_BUTTONS)
  })

  it('adds submit and cancel when plugin has text fields', () => {
    expect(
      resolveHumanStepFooterActions(
        [{ id: 'containerId', type: 'string', ui: { widget: 'STRING' } }],
        []
      )
    ).toEqual([DEFAULT_HUMAN_SUBMIT_OPTION, DEFAULT_HUMAN_CANCEL_OPTION])
  })

  it('keeps plugin options when only approval toggles are present', () => {
    const pluginOptions = [
      { label: 'Approve', approved: true },
      { label: 'Cancel', approved: false },
    ]
    expect(
      resolveHumanStepFooterActions(
        [{ id: 'approveRestart', type: 'boolean', ui: { widget: 'APPROVAL_TOGGLE' } }],
        pluginOptions
      )
    ).toEqual(pluginOptions)
  })

  it('uses approve/cancel labels in default option buttons', () => {
    expect(DEFAULT_HUMAN_OPTION_BUTTONS[0]).toEqual(DEFAULT_HUMAN_APPROVE_OPTION)
    expect(DEFAULT_HUMAN_OPTION_BUTTONS[1]).toEqual(DEFAULT_HUMAN_CANCEL_OPTION)
  })
})
