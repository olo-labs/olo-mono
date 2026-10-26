/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { describe, expect, it } from 'vitest'
import {
  mergeWorkflowCanvasView,
  readWorkflowCanvasView,
  WORKFLOW_CANVAS_VIEW_KEY,
} from './workflowCanvasView'
import type { WorkflowDocument } from '../types/workflow'

describe('workflowCanvasView', () => {
  it('reads and merges canvas view metadata', () => {
    const workflow: WorkflowDocument = {
      id: 'agent',
      metadata: {
        [WORKFLOW_CANVAS_VIEW_KEY]: {
          viewport: { x: -120, y: 40, zoom: 0.85 },
          size: { width: 1280, height: 720 },
        },
      },
    }

    expect(readWorkflowCanvasView(workflow)).toEqual({
      viewport: { x: -120, y: 40, zoom: 0.85 },
      size: { width: 1280, height: 720 },
    })

    const merged = mergeWorkflowCanvasView(workflow, {
      viewport: { x: -200, y: 10, zoom: 1 },
    })
    expect(readWorkflowCanvasView(merged)).toEqual({
      viewport: { x: -200, y: 10, zoom: 1 },
      size: { width: 1280, height: 720 },
    })
  })

  it('ignores invalid canvas view metadata', () => {
    const workflow: WorkflowDocument = {
      id: 'agent',
      metadata: {
        [WORKFLOW_CANVAS_VIEW_KEY]: {
          viewport: { x: 'bad', y: 0, zoom: 1 },
        },
      },
    }
    expect(readWorkflowCanvasView(workflow)).toBeNull()
  })
})
