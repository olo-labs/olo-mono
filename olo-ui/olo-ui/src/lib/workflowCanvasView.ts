/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { WorkflowDocument } from '../types/workflow'

export const WORKFLOW_CANVAS_VIEW_KEY = 'canvasView'

export const CANVAS_FIT_VIEW_PADDING = 0.25

export interface WorkflowCanvasViewport {
  x: number
  y: number
  zoom: number
}

export interface WorkflowCanvasSize {
  width: number
  height: number
}

export interface WorkflowCanvasView {
  viewport?: WorkflowCanvasViewport
  size?: WorkflowCanvasSize
}

const CANVAS_VIEWPORT_PRECISION = 2

function roundCanvasNumber(value: number): number {
  const factor = 10 ** CANVAS_VIEWPORT_PRECISION
  return Math.round(value * factor) / factor
}

/** Rounds viewport values so React Flow float drift does not mark the draft dirty. */
export function roundCanvasViewport(viewport: WorkflowCanvasViewport): WorkflowCanvasViewport {
  return {
    x: roundCanvasNumber(viewport.x),
    y: roundCanvasNumber(viewport.y),
    zoom: roundCanvasNumber(viewport.zoom),
  }
}

/** Canvas size follows the container layout and is not treated as a user edit. */
export function canvasViewForDirtyComparison(view: WorkflowCanvasView | null): WorkflowCanvasView | undefined {
  if (!view?.viewport) return undefined
  return { viewport: roundCanvasViewport(view.viewport) }
}

function readViewport(value: unknown): WorkflowCanvasViewport | undefined {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return undefined
  const record = value as Record<string, unknown>
  if (
    typeof record.x !== 'number'
    || typeof record.y !== 'number'
    || typeof record.zoom !== 'number'
    || !Number.isFinite(record.x)
    || !Number.isFinite(record.y)
    || !Number.isFinite(record.zoom)
    || record.zoom <= 0
  ) {
    return undefined
  }
  return { x: record.x, y: record.y, zoom: record.zoom }
}

function readSize(value: unknown): WorkflowCanvasSize | undefined {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return undefined
  const record = value as Record<string, unknown>
  if (
    typeof record.width !== 'number'
    || typeof record.height !== 'number'
    || !Number.isFinite(record.width)
    || !Number.isFinite(record.height)
    || record.width <= 0
    || record.height <= 0
  ) {
    return undefined
  }
  return { width: record.width, height: record.height }
}

export function readWorkflowCanvasView(doc: WorkflowDocument | null | undefined): WorkflowCanvasView | null {
  if (!doc?.metadata) return null
  const raw = doc.metadata[WORKFLOW_CANVAS_VIEW_KEY]
  if (!raw || typeof raw !== 'object' || Array.isArray(raw)) return null
  const record = raw as Record<string, unknown>
  const viewport = readViewport(record.viewport)
  const size = readSize(record.size)
  if (!viewport && !size) return null
  return {
    ...(viewport ? { viewport } : {}),
    ...(size ? { size } : {}),
  }
}

export function mergeWorkflowCanvasView(
  doc: WorkflowDocument,
  patch: WorkflowCanvasView,
): WorkflowDocument {
  const existing = readWorkflowCanvasView(doc) ?? {}
  return {
    ...doc,
    metadata: {
      ...doc.metadata,
      [WORKFLOW_CANVAS_VIEW_KEY]: {
        ...existing,
        ...patch,
      },
    },
  }
}
