/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { normalizeWorkflowBoundaries } from './boundaryNodes'
import { canvasViewForDirtyComparison, readWorkflowCanvasView } from './workflowCanvasView'
import { ensureWorkflowModelInfrastructure } from './workflowModelProviders'
import { normalizeWorkflowDocumentEmoji } from './workflowEmoji'
import type { WorkflowDocument, WorkflowNode } from '../types/workflow'

const NODE_POSITION_PRECISION = 2

function roundNodeNumber(value: number): number {
  const factor = 10 ** NODE_POSITION_PRECISION
  return Math.round(value * factor) / factor
}

function readDesignerPosition(node: WorkflowNode): { x: number; y: number } | null {
  const designer = node.configuration?.designer as
    | { position?: { x?: number; y?: number }; x?: number; y?: number }
    | undefined
  if (!designer) return null
  if (designer.position && typeof designer.position.x === 'number' && typeof designer.position.y === 'number') {
    return { x: designer.position.x, y: designer.position.y }
  }
  if (typeof designer.x === 'number' && typeof designer.y === 'number') {
    return { x: designer.x, y: designer.y }
  }
  return null
}

/** Applies the same normalization used before persisting a workflow to disk. */
export function normalizeWorkflowDraft(document: WorkflowDocument): WorkflowDocument {
  return normalizeWorkflowDocumentEmoji(
    ensureWorkflowModelInfrastructure(normalizeWorkflowBoundaries(document)),
  )
}

function workflowDraftForDirtyComparison(document: WorkflowDocument): WorkflowDocument {
  const normalized = normalizeWorkflowDraft(document)
  const canvasView = canvasViewForDirtyComparison(readWorkflowCanvasView(normalized))
  const { canvasView: _ignoredCanvasView, ...metadataWithoutCanvasView } = normalized.metadata ?? {}

  return {
    ...normalized,
    nodes: (normalized.nodes ?? []).map((node) => {
      const position = readDesignerPosition(node)
      if (!position) return node
      return {
        ...node,
        configuration: {
          ...node.configuration,
          designer: {
            ...((node.configuration?.designer as object) ?? {}),
            position: {
              x: roundNodeNumber(position.x),
              y: roundNodeNumber(position.y),
            },
          },
        },
      }
    }),
    metadata: {
      ...metadataWithoutCanvasView,
      ...(canvasView ? { canvasView } : {}),
    },
  }
}

/** Stable JSON snapshot for dirty-state comparison. */
export function workflowDraftSnapshot(document: WorkflowDocument): string {
  return JSON.stringify(workflowDraftForDirtyComparison(document))
}

export function isWorkflowDraftDirty(
  document: WorkflowDocument,
  savedSnapshot: string | null,
): boolean {
  if (!savedSnapshot) return false

  const current = workflowDraftForDirtyComparison(document)
  const saved = JSON.parse(savedSnapshot) as WorkflowDocument
  const savedCanvasView = saved.metadata?.canvasView as { viewport?: unknown } | undefined
  const savedHadCanvasViewport = Boolean(savedCanvasView?.viewport)

  if (!savedHadCanvasViewport) {
    const { canvasView: _currentCanvasView, ...currentMetadata } = current.metadata ?? {}
    const { canvasView: _savedCanvasView, ...savedMetadata } = saved.metadata ?? {}
    return JSON.stringify({ ...current, metadata: currentMetadata })
      !== JSON.stringify({ ...saved, metadata: savedMetadata })
  }

  return JSON.stringify(current) !== savedSnapshot
}
