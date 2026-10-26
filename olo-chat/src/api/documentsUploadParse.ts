/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import type { DocumentUploadResult } from './documentsUploadTypes'

/** Prefer readable text for UI; avoid dumping HTML error pages. Uses JSON `message` when the BE returns it. */
export function uploadErrorMessage(status: number, text: string): string {
  const trimmed = text?.trim() ?? ''
  try {
    const j = JSON.parse(trimmed) as { message?: string; error?: string }
    const m = j.message ?? j.error
    if (typeof m === 'string' && m.trim()) {
      return m.trim()
    }
  } catch {
    /* not JSON */
  }
  if (status === 413 || /MaxUploadSizeExceeded|maximum upload size/i.test(trimmed)) {
    return 'File too large (exceeds server upload limit).'
  }
  if (trimmed.startsWith('<') || trimmed.includes('<!DOCTYPE')) {
    if (status === 413) return 'File too large (exceeds server upload limit).'
    if (status >= 500) return `Upload failed (${status} server error).`
    return `Upload failed (${status}).`
  }
  if (trimmed) return trimmed.length > 500 ? `${trimmed.slice(0, 500)}…` : trimmed
  if (status === 413) return 'File too large (exceeds server upload limit).'
  return `Upload failed (${status}).`
}

export function parseUploadJson(text: string): Pick<DocumentUploadResult, 'runId' | 'runIds' | 'files'> {
  if (!text?.trim()) return {}
  try {
    const data = JSON.parse(text) as Record<string, unknown>
    const runId = typeof data.runId === 'string' ? data.runId : undefined
    const runIds = Array.isArray(data.runIds)
      ? data.runIds.filter((x): x is string => typeof x === 'string')
      : undefined
    let files: { fileName: string; runId?: string }[] | undefined
    if (Array.isArray(data.files)) {
      const out: { fileName: string; runId?: string }[] = []
      for (const item of data.files) {
        if (!item || typeof item !== 'object') continue
        const o = item as Record<string, unknown>
        const fileName = typeof o.fileName === 'string' ? o.fileName : typeof o.name === 'string' ? o.name : ''
        if (!fileName) continue
        const rid = typeof o.runId === 'string' ? o.runId : undefined
        if (rid !== undefined) out.push({ fileName, runId: rid })
        else out.push({ fileName })
      }
      files = out.length > 0 ? out : undefined
    }
    return { runId, runIds, files }
  } catch {
    return {}
  }
}
