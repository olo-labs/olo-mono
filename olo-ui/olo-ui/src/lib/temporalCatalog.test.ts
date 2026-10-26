/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { describe, expect, it } from 'vitest'
import type { StudioCatalog } from '../types/catalog'
import type { WorkflowSummary } from '../types/workflow'
import { resolveInitialRunSelection, workflowsForQueue } from './temporalCatalog'

const sampleCatalog: StudioCatalog = {
  schemaVersion: '1.0',
  workflowTypes: [{ id: 'olo', label: 'OLO Kernel' }],
  queues: [
    { name: 'oloQueue1', label: 'Queue 1', workflowType: 'olo' },
    { name: 'oloQueue2', label: 'Queue 2', workflowType: 'olo' },
  ],
}

const sampleWorkflows: WorkflowSummary[] = [
  { fileName: 'architect.json', id: 'architect', label: 'Architect', queue: 'oloQueue1' },
  { fileName: 'agent.json', id: 'agent', label: 'Agent', queue: 'oloQueue2' },
]

describe('temporalCatalog', () => {
  it('filters workflows by queue', () => {
    expect(workflowsForQueue(sampleWorkflows, 'oloQueue2')).toEqual([
      { fileName: 'agent.json', id: 'agent', label: 'Agent', queue: 'oloQueue2' },
    ])
  })

  it('resolves initial run selection from catalog and workflow list', () => {
    expect(resolveInitialRunSelection(sampleCatalog, sampleWorkflows, 'oloQueue2', 'agent')).toEqual({
      queueName: 'oloQueue2',
      workflowId: 'agent',
      label: 'Agent',
      workflowType: 'olo',
    })
  })
})
