/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Subscribes to SSE run events for one document upload row and updates status/chunks in documentUploadsStore.
 */

import { useEffect } from 'react'
import { streamRunEvents } from '../api/chatApi'
import { documentUploadsStore, type DocumentUploadRow } from '../store/documentUploadsStore'

export function DocumentUploadRunTracker({ row }: { row: DocumentUploadRow }) {
  useEffect(() => {
    if (!row.runId) return
    if (row.status !== 'processing') return

    const abort = streamRunEvents(
      row.runId,
      (ev) => {
        documentUploadsStore.getState().patchRowFromEvent(row.id, ev)
      },
      () => {
        /* SSE errors: keep processing; optional fallback handled in view */
      }
    )
    return abort
  }, [row.id, row.runId, row.status])

  return null
}
