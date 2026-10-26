/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { useEffect, useState } from 'react'
import { getChatBackendHealth } from '../api/chatApi'

const DEFAULT_POLL_MS = 12_000

/**
 * True when GET /api/health succeeds. Polls so the UI recovers when the backend starts without a full reload.
 */
export function useBackendReachable(pollMs: number = DEFAULT_POLL_MS) {
  const [reachable, setReachable] = useState(false)

  useEffect(() => {
    let cancelled = false
    const run = () => {
      getChatBackendHealth().then((ok) => {
        if (!cancelled) setReachable(ok)
      })
    }
    run()
    const id = window.setInterval(run, pollMs)
    return () => {
      cancelled = true
      window.clearInterval(id)
    }
  }, [pollMs])

  return reachable
}
