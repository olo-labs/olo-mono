/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { describe, expect, it } from 'vitest'
import {
  childWorkflowDisplayTitle,
  copyWorkflowPath,
  parseWorkflowJson,
  renameWorkflowPath,
  resolveDelegateWorkflowFileName,
  workflowFileName,
} from './workflowConfiguration'
import type { WorkflowSummary } from '../types/workflow'

describe('parseWorkflowJson', () => {
  it('parses a minimal workflow document', () => {
    const doc = parseWorkflowJson('{"id":"agent","enabled":true,"label":"Agent"}')
    expect(doc.id).toBe('agent')
    expect(doc.enabled).toBe(true)
    expect(doc.label).toBe('Agent')
  })

  it('rejects non-object JSON', () => {
    expect(() => parseWorkflowJson('[]')).toThrow(/object/)
  })

  it('requires string id', () => {
    expect(() => parseWorkflowJson('{"label":"x"}')).toThrow(/id/)
  })
})

describe('workflowFileName', () => {
  it('derives file name from workflow id', () => {
    expect(workflowFileName({ id: 'agent' })).toBe('agent.json')
  })
})

describe('copyWorkflowPath', () => {
  it('creates a sibling copy path in the same folder', () => {
    expect(copyWorkflowPath('default/agent.json', [])).toBe('default/agent-copy.json')
    expect(copyWorkflowPath('default/agent.json', ['default/agent-copy.json'])).toBe(
      'default/agent-copy-2.json',
    )
  })
})

describe('renameWorkflowPath', () => {
  it('keeps parent folder when renaming', () => {
    expect(renameWorkflowPath('default/agent.json', 'planner.json')).toBe('default/planner.json')
  })
})

const sampleWorkflows: WorkflowSummary[] = [
  { fileName: 'current-active/debug.json', id: 'debug', label: 'Debug Agent', queue: null, workflowType: null },
  { fileName: 'agent.json', id: 'agent', label: 'Agent', queue: null, workflowType: null },
]

describe('resolveDelegateWorkflowFileName', () => {
  it('resolves by workflow id', () => {
    expect(resolveDelegateWorkflowFileName('debug', sampleWorkflows)).toBe('current-active/debug.json')
  })

  it('falls back to id.json when workflows are loaded', () => {
    expect(resolveDelegateWorkflowFileName('planner', sampleWorkflows)).toBe('planner.json')
  })
})

describe('childWorkflowDisplayTitle', () => {
  it('prefers node label then workflow summary label', () => {
    expect(childWorkflowDisplayTitle('debug', 'My delegate', sampleWorkflows)).toBe('My delegate')
    expect(childWorkflowDisplayTitle('debug', undefined, sampleWorkflows)).toBe('Debug Agent')
    expect(childWorkflowDisplayTitle('missing', undefined, [])).toBe('missing')
  })
})
