/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { useEffect, useRef } from 'react'
import { useReactFlow, useStore } from '@xyflow/react'
import {
  CANVAS_FIT_VIEW_PADDING,
  type WorkflowCanvasView,
} from '../../lib/workflowCanvasView'

export interface CanvasViewportControllerProps {
  workflowKey: string
  savedView: WorkflowCanvasView | null
  nodeCount: number
  onViewApplied?: () => void
}

export function CanvasViewportController({
  workflowKey,
  savedView,
  nodeCount,
  onViewApplied,
}: CanvasViewportControllerProps) {
  const { setViewport, fitView } = useReactFlow()
  const { width, height } = useStore((state) => ({
    width: state.width,
    height: state.height,
  }))
  const appliedKeyRef = useRef<string | null>(null)

  useEffect(() => {
    if (appliedKeyRef.current === workflowKey) return
    if (width <= 0 || height <= 0) return
    appliedKeyRef.current = workflowKey

    if (savedView?.viewport) {
      setViewport(savedView.viewport, { duration: 0 })
      onViewApplied?.()
      return
    }

    if (nodeCount > 0) {
      void fitView({ padding: CANVAS_FIT_VIEW_PADDING, duration: 0 }).then(() => {
        onViewApplied?.()
      })
      return
    }

    onViewApplied?.()
  }, [fitView, height, nodeCount, onViewApplied, savedView, setViewport, width, workflowKey])

  return null
}
