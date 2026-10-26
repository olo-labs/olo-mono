/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { useCallback, useEffect, useRef, useState } from 'react'
import {
  CHAT_PROGRESS_EXPANDED_KEY,
  CHAT_PROGRESS_HEIGHT_DEFAULT,
  CHAT_PROGRESS_HEIGHT_KEY,
  CHAT_PROGRESS_HEIGHT_MAX,
  CHAT_PROGRESS_HEIGHT_MIN,
  readStoredProgressExpanded,
  readStoredProgressHeight,
} from '../lib/chatProgressStorage'

export function useChatProgressPanel() {
  const [progressExpanded, setProgressExpanded] = useState(readStoredProgressExpanded)
  const [progressHeight, setProgressHeight] = useState(readStoredProgressHeight)
  const [progressResizing, setProgressResizing] = useState(false)
  const resizingRef = useRef(false)
  const resizeStartYRef = useRef(0)
  const resizeStartHeightRef = useRef(CHAT_PROGRESS_HEIGHT_DEFAULT)

  useEffect(() => {
    try {
      window.localStorage.setItem(CHAT_PROGRESS_EXPANDED_KEY, progressExpanded ? '1' : '0')
    } catch {
      /* quota */
    }
  }, [progressExpanded])

  useEffect(() => {
    try {
      window.localStorage.setItem(CHAT_PROGRESS_HEIGHT_KEY, String(progressHeight))
    } catch {
      /* quota */
    }
  }, [progressHeight])

  useEffect(() => {
    const onPointerMove = (e: PointerEvent) => {
      if (!resizingRef.current) return
      const delta = e.clientY - resizeStartYRef.current
      const next = Math.min(
        CHAT_PROGRESS_HEIGHT_MAX,
        Math.max(CHAT_PROGRESS_HEIGHT_MIN, resizeStartHeightRef.current - delta)
      )
      setProgressHeight(next)
    }
    const onPointerUp = () => {
      resizingRef.current = false
      setProgressResizing(false)
      document.body.style.cursor = ''
      document.body.style.userSelect = ''
    }
    const onMouseMove = (e: MouseEvent) => {
      if (!resizingRef.current) return
      const delta = e.clientY - resizeStartYRef.current
      const next = Math.min(
        CHAT_PROGRESS_HEIGHT_MAX,
        Math.max(CHAT_PROGRESS_HEIGHT_MIN, resizeStartHeightRef.current - delta)
      )
      setProgressHeight(next)
    }
    const onMouseUp = () => {
      resizingRef.current = false
      setProgressResizing(false)
      document.body.style.cursor = ''
      document.body.style.userSelect = ''
    }
    window.addEventListener('pointermove', onPointerMove)
    window.addEventListener('pointerup', onPointerUp)
    window.addEventListener('mousemove', onMouseMove)
    window.addEventListener('mouseup', onMouseUp)
    return () => {
      window.removeEventListener('pointermove', onPointerMove)
      window.removeEventListener('pointerup', onPointerUp)
      window.removeEventListener('mousemove', onMouseMove)
      window.removeEventListener('mouseup', onMouseUp)
    }
  }, [])

  const startResize = useCallback(
    (clientY: number) => {
      resizingRef.current = true
      setProgressResizing(true)
      resizeStartYRef.current = clientY
      resizeStartHeightRef.current = progressHeight
      document.body.style.cursor = 'ns-resize'
      document.body.style.userSelect = 'none'
    },
    [progressHeight]
  )

  return {
    progressExpanded,
    setProgressExpanded,
    progressHeight,
    progressResizing,
    startResize,
  }
}
