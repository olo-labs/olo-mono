/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { useCallback, useEffect, useRef } from 'react'
import { useReactFlow, useUpdateNodeInternals } from '@xyflow/react'
import {
  mergeWorkflowCanvasView,
  roundCanvasViewport,
  type WorkflowCanvasSize,
} from '../lib/workflowCanvasView'
import { workflowDraftSnapshot } from '../lib/workflowDraftSnapshot'
import type { WorkflowDocument } from '../types/workflow'
import { workflowConfigurationStore } from '../store/workflowConfigurationStore'

export function useWorkflowCanvasViewPersistence(
  draft: WorkflowDocument | null,
  readOnly: boolean,
  syncingRef: React.MutableRefObject<boolean>,
  workflowCanvasKey: string,
  canvasHydratingRef: React.MutableRefObject<boolean>,
  canvasContainerRef: React.RefObject<HTMLDivElement>,
  canvasSizeRef: React.MutableRefObject<WorkflowCanvasSize | null>,
) {
  const { getViewport, getNodes } = useReactFlow()
  const updateNodeInternals = useUpdateNodeInternals()
  const updateDraft = workflowConfigurationStore((s) => s.updateDraft)
  const canvasViewPersistTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  const persistCanvasView = useCallback(() => {
    if (!draft || readOnly || syncingRef.current || canvasHydratingRef.current) return
    const viewport = roundCanvasViewport(getViewport())
    const size = canvasSizeRef.current
    const merged = mergeWorkflowCanvasView(draft, {
      viewport,
      ...(size ? { size } : {}),
    })
    if (workflowConfigurationStore.getState().savedSnapshot === workflowDraftSnapshot(merged)) {
      return
    }
    updateDraft(merged)
  }, [canvasHydratingRef, canvasSizeRef, draft, getViewport, readOnly, syncingRef, updateDraft])

  const schedulePersistCanvasView = useCallback(() => {
    if (canvasViewPersistTimerRef.current) clearTimeout(canvasViewPersistTimerRef.current)
    canvasViewPersistTimerRef.current = setTimeout(() => {
      persistCanvasView()
    }, 250)
  }, [persistCanvasView])

  useEffect(() => {
    const element = canvasContainerRef.current
    if (!element || readOnly) return undefined

    const updateSize = () => {
      const width = Math.round(element.clientWidth)
      const height = Math.round(element.clientHeight)
      if (width <= 0 || height <= 0) return
      const previous = canvasSizeRef.current
      if (previous?.width === width && previous?.height === height) return
      canvasSizeRef.current = { width, height }
      window.requestAnimationFrame(() => {
        for (const node of getNodes()) {
          updateNodeInternals(node.id)
        }
      })
      schedulePersistCanvasView()
    }

    updateSize()
    const observer = new ResizeObserver(updateSize)
    observer.observe(element)
    return () => {
      observer.disconnect()
      if (canvasViewPersistTimerRef.current) clearTimeout(canvasViewPersistTimerRef.current)
    }
  }, [canvasContainerRef, canvasSizeRef, getNodes, readOnly, schedulePersistCanvasView, updateNodeInternals, workflowCanvasKey])

  return { schedulePersistCanvasView }
}
