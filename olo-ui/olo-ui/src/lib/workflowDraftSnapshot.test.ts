/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { describe, expect, it } from 'vitest'
import fs from 'fs'
import path from 'path'
import {
  isWorkflowDraftDirty,
  normalizeWorkflowDraft,
  workflowDraftSnapshot,
} from './workflowDraftSnapshot'
import { mergeWorkflowCanvasView, readWorkflowCanvasView } from './workflowCanvasView'
import type { WorkflowDocument } from '../types/workflow'

const baseWorkflow: WorkflowDocument = {
  id: 'agent',
  version: '1.0.0',
  nodes: [{ id: 'start', type: 'START', ports: [] }],
  metadata: {
    canvasView: {
      viewport: { x: 125, y: 176, zoom: 1 },
      size: { width: 1194, height: 813 },
    },
  },
}

describe('workflowDraftSnapshot', () => {
  it('treats equivalent canvas view persistence as clean', () => {
    const normalized = normalizeWorkflowDraft(baseWorkflow)
    const snapshot = workflowDraftSnapshot(normalized)
    const persisted = mergeWorkflowCanvasView(normalized, {
      viewport: { x: 125, y: 176, zoom: 1 },
      size: { width: 1194, height: 813 },
    })

    expect(isWorkflowDraftDirty(persisted, snapshot)).toBe(false)
  })

  it('ignores container size changes for dirty state', () => {
    const normalized = normalizeWorkflowDraft(baseWorkflow)
    const snapshot = workflowDraftSnapshot(normalized)
    const resized = mergeWorkflowCanvasView(normalized, {
      size: { width: 1400, height: 900 },
    })

    expect(isWorkflowDraftDirty(resized, snapshot)).toBe(false)
  })

  it('ignores minor viewport float drift for dirty state', () => {
    const normalized = normalizeWorkflowDraft(baseWorkflow)
    const snapshot = workflowDraftSnapshot(normalized)
    const drifted = mergeWorkflowCanvasView(normalized, {
      viewport: { x: 125.004, y: 176.001, zoom: 1.0004 },
      size: { width: 1194, height: 813 },
    })

    expect(isWorkflowDraftDirty(drifted, snapshot)).toBe(false)
  })

  it('marks meaningful viewport changes as dirty', () => {
    const normalized = normalizeWorkflowDraft(baseWorkflow)
    const snapshot = workflowDraftSnapshot(normalized)
    const panned = mergeWorkflowCanvasView(normalized, {
      viewport: { x: 200, y: 176, zoom: 1 },
    })

    expect(isWorkflowDraftDirty(panned, snapshot)).toBe(true)
  })

  it('marks real edits as dirty', () => {
    const normalized = normalizeWorkflowDraft(baseWorkflow)
    const snapshot = workflowDraftSnapshot(normalized)
    const edited = {
      ...normalized,
      label: 'Changed label',
    }

    expect(isWorkflowDraftDirty(edited, snapshot)).toBe(true)
  })

  it('keeps current-active agent.json clean after canvas mount adjustments', () => {
    const agentPath = path.resolve(
      __dirname,
      '../../../../olo-mono/olo-definition/olo-configuration/default/agent.json',
    )
    const agent = JSON.parse(fs.readFileSync(agentPath, 'utf8')) as WorkflowDocument
    const normalized = normalizeWorkflowDraft(agent)
    const snapshot = workflowDraftSnapshot(normalized)
    const existingView = readWorkflowCanvasView(normalized)
    const afterMount = mergeWorkflowCanvasView(normalized, {
      viewport: {
        x: (existingView?.viewport?.x ?? 0) + 0.003,
        y: (existingView?.viewport?.y ?? 0) + 0.001,
        zoom: existingView?.viewport?.zoom ?? 1,
      },
      size: { width: 1280, height: 800 },
    })

    expect(isWorkflowDraftDirty(afterMount, snapshot)).toBe(false)
  })
})
