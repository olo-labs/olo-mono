/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { describe, expect, it, beforeEach } from 'vitest'
import { getLastWorkflowFileName, setLastWorkflowFileName } from './lastWorkflow'

describe('lastWorkflow', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('stores and reads the last workflow file name', () => {
    expect(getLastWorkflowFileName()).toBe('')
    setLastWorkflowFileName('current-active/agent.json')
    expect(getLastWorkflowFileName()).toBe('current-active/agent.json')
    setLastWorkflowFileName('')
    expect(getLastWorkflowFileName()).toBe('')
  })
})
