/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Documents upload API — **backend-only** handling (olo-chat only sends multipart to the BE).
 *
 * **Endpoint:** `POST /api/resource/upload` (multipart: `capabilitySource`, `files`, optional `taskQueue` / `pipelineId`).
 */

import { getApiPathPrefix } from '../lib/apiBase'
import { getApiAuthHeaders } from '../lib/wsUrl'
import { parseUploadJson, uploadErrorMessage } from './documentsUploadParse'
import type {
  CapabilitySourceUploadRequest,
  DocumentUploadResult,
  ResourceUploadMultipartOptions,
} from './documentsUploadTypes'

export type {
  CapabilitySourceUploadRequest,
  DocumentUploadResult,
  ResourceUploadMultipartOptions,
} from './documentsUploadTypes'

const API = getApiPathPrefix()

const UPLOAD_QUEUE =
  (import.meta.env.VITE_RESOURCE_UPLOAD_QUEUE as string)?.trim() ||
  (import.meta.env.VITE_RAG_QUEUE as string)?.trim() ||
  ''
const UPLOAD_PIPELINE =
  (import.meta.env.VITE_RESOURCE_UPLOAD_PIPELINE as string)?.trim() ||
  (import.meta.env.VITE_RAG_PIPELINE as string)?.trim() ||
  ''

export interface ResourceUploadedDocumentDto {
  fileName: string
  capabilitySource: string
  sizeBytes?: number
  lastModified?: number
}

export interface ResourceUploadSourceDto {
  capabilitySource: string
  fileCount: number
  files: ResourceUploadedDocumentDto[]
}

function parseJson<T>(text: string): T | null {
  if (!text?.trim()) return null
  try {
    return JSON.parse(text) as T
  } catch {
    return null
  }
}

function asString(value: unknown): string | undefined {
  return typeof value === 'string' && value.trim() ? value.trim() : undefined
}

function asNumber(value: unknown): number | undefined {
  return typeof value === 'number' && Number.isFinite(value) ? value : undefined
}

function normalizeUploadSource(raw: unknown): ResourceUploadSourceDto | null {
  if (!raw || typeof raw !== 'object') return null
  const item = raw as Record<string, unknown>
  const capabilitySource = asString(item.capabilitySource) ?? asString(item.name)
  if (!capabilitySource) return null
  const files = Array.isArray(item.files)
    ? item.files
        .map((file) => {
          if (!file || typeof file !== 'object') return null
          const f = file as Record<string, unknown>
          const fileName = asString(f.fileName) ?? asString(f.name)
          if (!fileName) return null
          const row: ResourceUploadedDocumentDto = {
            fileName,
            capabilitySource: asString(f.capabilitySource) ?? capabilitySource,
          }
          const sizeBytes = asNumber(f.sizeBytes)
          const lastModified = asNumber(f.lastModified)
          if (sizeBytes != null) row.sizeBytes = sizeBytes
          if (lastModified != null) row.lastModified = lastModified
          return row
        })
        .filter((file): file is ResourceUploadedDocumentDto => Boolean(file))
    : []
  return {
    capabilitySource,
    fileCount: asNumber(item.fileCount) ?? files.length,
    files,
  }
}

export async function uploadMultipartToSharedFolder(
  options: ResourceUploadMultipartOptions
): Promise<DocumentUploadResult> {
  const capabilitySource = options.capabilitySource?.trim() ?? ''
  const { files } = options
  if (!capabilitySource || files.length === 0) {
    return { success: false, message: 'Capability source and at least one file are required.' }
  }

  const form = new FormData()
  form.append('capabilitySource', capabilitySource)
  const q = options.taskQueue ?? UPLOAD_QUEUE
  const p = options.pipelineId ?? UPLOAD_PIPELINE
  if (q) form.append('taskQueue', q)
  if (p) form.append('pipelineId', p)
  for (const file of files) {
    form.append('files', file, file.name)
  }

  try {
    const res = await fetch(`${API}/resource/upload`, {
      method: 'POST',
      headers: getApiAuthHeaders(),
      body: form,
    })

    const text = await res.text()
    if (!res.ok) {
      return { success: false, message: uploadErrorMessage(res.status, text) }
    }
    try {
      const data = text?.trim() ? (JSON.parse(text) as Record<string, unknown>) : {}
      if (data && typeof data === 'object' && data.success === false) {
        const msg =
          typeof data.message === 'string'
            ? data.message
            : typeof data.error === 'string'
              ? data.error
              : 'Upload failed'
        return { success: false, message: msg }
      }
    } catch {
      /* empty or non-JSON success body is OK */
    }
    const parsed = parseUploadJson(text)
    return { success: true, ...parsed }
  } catch (err) {
    const msg = err instanceof Error ? err.message : 'Network error during upload'
    return {
      success: false,
      message: /Failed to fetch|NetworkError|aborted/i.test(msg)
        ? 'Upload failed (network error or connection closed).'
        : msg,
    }
  }
}

export async function uploadCapabilitySourceFiles(
  req: CapabilitySourceUploadRequest
): Promise<DocumentUploadResult> {
  return uploadMultipartToSharedFolder({
    capabilitySource: req.capabilitySource.trim(),
    files: req.files,
  })
}

export async function listResourceUploadSources(): Promise<ResourceUploadSourceDto[]> {
  try {
    const res = await fetch(`${API}/knowledge/source-collections`, {
      headers: getApiAuthHeaders(),
    })
    if (!res.ok) return []
    const data = parseJson<unknown>(await res.text())
    const list = Array.isArray(data)
      ? data
      : data && typeof data === 'object' && Array.isArray((data as Record<string, unknown>).sources)
        ? ((data as Record<string, unknown>).sources as unknown[])
        : []
    return list.map(normalizeUploadSource).filter((s): s is ResourceUploadSourceDto => Boolean(s))
  } catch {
    return []
  }
}

export async function deleteCapabilitySource(
  capabilitySource: string
): Promise<{ success: boolean; message?: string; deletedFiles?: number }> {
  const source = capabilitySource.trim()
  if (!source) return { success: false, message: 'Capability source is required.' }
  try {
    const res = await fetch(`${API}/resource/source/${encodeURIComponent(source)}`, {
      method: 'DELETE',
      headers: getApiAuthHeaders(),
    })
    const text = await res.text()
    const data = parseJson<Record<string, unknown>>(text)
    if (!res.ok || data?.success === false) {
      return {
        success: false,
        message:
          typeof data?.message === 'string'
            ? data.message
            : `Delete source failed (${res.status})`,
      }
    }
    return {
      success: true,
      deletedFiles: typeof data?.deletedFiles === 'number' ? data.deletedFiles : undefined,
    }
  } catch (err) {
    return {
      success: false,
      message: err instanceof Error ? err.message : 'Network error during delete',
    }
  }
}

export async function fetchCapabilitySourceFile(body: {
  capabilitySource: string
  fileName: string
}): Promise<{ success: boolean; blob?: Blob; contentType?: string; message?: string }> {
  const source = body.capabilitySource.trim()
  const fileName = body.fileName.trim()
  if (!source || !fileName) return { success: false, message: 'Source and file name are required.' }
  try {
    const res = await fetch(
      `${API}/resource/source/${encodeURIComponent(source)}/file/${encodeURIComponent(fileName)}`,
      { headers: getApiAuthHeaders() }
    )
    if (!res.ok) return { success: false, message: `File preview failed (${res.status})` }
    return {
      success: true,
      blob: await res.blob(),
      contentType: res.headers.get('content-type') ?? undefined,
    }
  } catch (err) {
    return {
      success: false,
      message: err instanceof Error ? err.message : 'Network error during file preview',
    }
  }
}

export async function reprocessUploadedDocument(body: {
  capabilitySource: string
  fileName: string
}): Promise<{ success: boolean; runId?: string }> {
  try {
    const res = await fetch(`${API}/resource/reprocess`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...getApiAuthHeaders() },
      body: JSON.stringify(body),
    })
    if (res.status === 404) return { success: false }
    const text = await res.text()
    if (!res.ok) return { success: false }
    try {
      const data = text ? (JSON.parse(text) as { runId?: string }) : {}
      return { success: true, runId: typeof data.runId === 'string' ? data.runId : undefined }
    } catch {
      return { success: true }
    }
  } catch {
    return { success: false }
  }
}

export async function reprocessCapabilitySourceDocument(body: {
  capabilitySource: string
  fileName: string
}): Promise<{ success: boolean; runId?: string }> {
  return reprocessUploadedDocument(body)
}

export function getConfiguredCapabilitySources(): string[] {
  const raw =
    (import.meta.env.VITE_CAPABILITY_SOURCE_OPTIONS as string)?.trim() ||
    (import.meta.env.VITE_RAG_OPTIONS as string)?.trim() ||
    ''
  if (!raw) return []
  return raw.split(',').map((s) => s.trim()).filter(Boolean)
}
