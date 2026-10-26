/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { useCallback, useRef } from 'react'

export interface PanelResizeHandleProps {
  /** Which panel this handle resizes (left = left edge of left panel, etc.) */
  panel: 'left' | 'tools' | 'properties'
  onResize: (deltaPx: number) => void
  /** Optional: hide when panel is collapsed (no drag) */
  visible?: boolean
  className?: string
}

const HANDLE_WIDTH = 4

/**
 * Vertical resize handle between panels. Drag horizontally to resize the adjacent panel.
 */
export function PanelResizeHandle({
  panel,
  onResize,
  visible = true,
  className = '',
}: PanelResizeHandleProps) {
  const startX = useRef(0)
  const startWidth = useRef(0)

  const handleMouseDown = useCallback(
    (e: React.MouseEvent) => {
      e.preventDefault()
      startX.current = e.clientX
      startWidth.current = 0 // not used; we apply delta
      const onMouseMove = (move: MouseEvent) => {
        const delta = move.clientX - startX.current
        startX.current = move.clientX
        if (panel === 'properties') {
          onResize(-delta)
        } else {
          onResize(delta)
        }
      }
      const onMouseUp = () => {
        document.removeEventListener('mousemove', onMouseMove)
        document.removeEventListener('mouseup', onMouseUp)
        document.body.style.cursor = ''
        document.body.style.userSelect = ''
      }
      document.addEventListener('mousemove', onMouseMove)
      document.addEventListener('mouseup', onMouseUp)
      document.body.style.cursor = 'col-resize'
      document.body.style.userSelect = 'none'
    },
    [panel, onResize]
  )

  if (!visible) return null

  return (
    <div
      role="separator"
      aria-orientation="vertical"
      aria-label={`Resize ${panel === 'tools' ? 'Conversation' : panel === 'properties' ? 'Events' : panel} panel`}
      className={`panel-resize-handle panel-resize-handle-${panel} ${className}`.trim()}
      style={{ width: HANDLE_WIDTH, minWidth: HANDLE_WIDTH }}
      onMouseDown={handleMouseDown}
    />
  )
}
