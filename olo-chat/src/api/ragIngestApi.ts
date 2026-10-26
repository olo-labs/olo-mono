/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * RAG ingest API - start indexing runs and list knowledge sources / uploaded documents.
 *
 * Endpoints:
 * - POST /api/rag/ingest - start documents-index Temporal workflow
 * - GET /api/knowledge/sources - executed knowledge sources with status
 * - GET /api/knowledge/source-collections - source collections with file counts
 * - GET /api/documents?capabilitySource= - files for a source folder
 */

import { getApiPathPrefix } from '../lib/apiBase'
import { getApiAuthHeaders } from '../lib/wsUrl'

const API = getApiPathPrefix()

const INGEST_QUEUE =
  (import.meta.env.VITE_RESOURCE_UPLOAD_QUEUE as string)?.trim() ||
  (import.meta.env.VITE_RAG_QUEUE as string)?.trim() ||
  ''
const INGEST_PIPELINE =
  (import.meta.env.VITE_RESOURCE_UPLOAD_PIPELINE as string)?.trim() ||
  (import.meta.env.VITE_RAG_PIPELINE as string)?.trim() ||
  'documents-index'
const DELETE_QUEUE = (import.meta.env.VITE_RAG_DELETE_QUEUE as string)?.trim() || ''
const DELETE_PIPELINE = (import.meta.env.VITE_RAG_DELETE_PIPELINE as string)?.trim() || 'documents-delete'

export const FILES_KNOWLEDGE_SOURCE_TYPE = 'files'

export interface KnowledgeSourceDto {
  capabilitySource: string
  fileCount: number
  files?: UploadedDocumentDto[]
  sourceType: string
  displayName: string
  description?: string
  status?: 'in_progress' | 'success' | 'failed' | 'unknown'
  message?: string
  runId?: string
  sourceCollection?: string
  createdAt?: number
  updatedAt?: number
  pipeline?: string
  taskQueue?: string
}

export interface UploadedDocumentDto {
  fileName: string
  capabilitySource: string
  sizeBytes?: number
  lastModified?: number
}

export interface RagIngestRequest {
  tenantId?: string
  sourceType?: string
  capabilitySource: string
  knowledgeName?: string
  fileNames: string[]
  taskQueue?: string
  pipelineId?: string
}

export interface RagIngestResult {
  success: boolean
  message?: string
  runId?: string
  capabilitySource?: string
  sourceType?: string
  knowledgeName?: string
  pipeline?: string
  taskQueue?: string
  files?: string[]
}

export interface RagDeleteRequest {
  tenantId?: string
  sourceType?: string
  knowledgeName: string
  sourceCollection?: string
  taskQueue?: string
  pipelineId?: string
}

export interface RagDeleteResult {
  success: boolean
  message?: string
  runId?: string
  knowledgeName?: string
  sourceType?: string
  sourceCollection?: string
  pipeline?: string
  taskQueue?: string
}

export interface KnowledgeSourceTypeOption {
  id: string
  label: string
  description: string
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

export function normalizeKnowledgeSource(raw: unknown): KnowledgeSourceDto | null {
  if (!raw || typeof raw !== 'object') return null
  const item = raw as Record<string, unknown>
  const capabilitySource =
    asString(item.capabilitySource) ??
    asString(item.id) ??
    asString(item.sourceId) ??
    asString(item.name)
  if (!capabilitySource) return null

  const sourceType =
    asString(item.sourceType) ?? asString(item.type) ?? asString(item.category) ?? FILES_KNOWLEDGE_SOURCE_TYPE
  const displayName = asString(item.displayName) ?? asString(item.label) ?? capabilitySource
  const fileCount = asNumber(item.fileCount) ?? asNumber(item.count) ?? 0
  const status = asString(item.status)

  return {
    capabilitySource,
    sourceType,
    displayName,
    description: asString(item.description),
    fileCount,
    files: Array.isArray(item.files) ? (item.files as UploadedDocumentDto[]) : undefined,
    message: asString(item.message),
    runId: asString(item.runId),
    sourceCollection: asString(item.sourceCollection),
    createdAt: asNumber(item.createdAt),
    updatedAt: asNumber(item.updatedAt),
    pipeline: asString(item.pipeline),
    taskQueue: asString(item.taskQueue),
    status:
      status === 'in_progress' || status === 'success' || status === 'failed' || status === 'unknown'
        ? status
        : undefined,
  }
}

export function knowledgeSourceTypeLabel(type: string): string {
  const normalized = type.trim()
  if (!normalized) return 'Other'
  if (normalized === FILES_KNOWLEDGE_SOURCE_TYPE) return 'File collection'
  if (normalized === 'github') return 'GitHub repository'
  if (normalized === 'code') return 'Code repository'
  return normalized
    .split(/[-_\s]+/)
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ')
}

export function getKnowledgeSourceTypeOptions(sources: KnowledgeSourceDto[]): KnowledgeSourceTypeOption[] {
  const typeIds = new Set<string>([FILES_KNOWLEDGE_SOURCE_TYPE])
  for (const source of sources) {
    typeIds.add(source.sourceType || FILES_KNOWLEDGE_SOURCE_TYPE)
  }
  return [...typeIds].sort((a, b) => a.localeCompare(b)).map((id) => ({
    id,
    label: knowledgeSourceTypeLabel(id),
    description:
      id === FILES_KNOWLEDGE_SOURCE_TYPE
        ? 'Uploaded files from Documents / Upload.'
        : `${knowledgeSourceTypeLabel(id)} knowledge collections.`,
  }))
}

export async function startRagIngest(req: RagIngestRequest): Promise<RagIngestResult> {
  const capabilitySource = req.capabilitySource?.trim() ?? ''
  if (!capabilitySource) {
    return { success: false, message: 'Knowledge source is required.' }
  }
  const fileNames = (req.fileNames ?? []).map((f) => f.trim()).filter(Boolean)
  const sourceType = req.sourceType?.trim() || FILES_KNOWLEDGE_SOURCE_TYPE
  const knowledgeName = req.knowledgeName?.trim() || capabilitySource

  const body: Record<string, unknown> = {
    tenantId: req.tenantId?.trim() || 'default',
    sourceType,
    capabilitySource,
    knowledgeName,
    fileNames,
  }
  const queue = req.taskQueue ?? INGEST_QUEUE
  const pipeline = req.pipelineId ?? INGEST_PIPELINE
  if (queue) body.taskQueue = queue
  if (pipeline) body.pipelineId = pipeline

  try {
    const res = await fetch(`${API}/rag/ingest`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...getApiAuthHeaders() },
      body: JSON.stringify(body),
    })
    const text = await res.text()
    const data = parseJson<RagIngestResult & { message?: string }>(text)
    if (!res.ok) {
      return {
        success: false,
        message: data?.message ?? `RAG ingest failed (${res.status})`,
      }
    }
    if (data?.success === false) {
      return { success: false, message: data.message ?? 'RAG ingest failed' }
    }
    return {
      success: true,
      runId: typeof data?.runId === 'string' ? data.runId : undefined,
      capabilitySource: data?.capabilitySource ?? capabilitySource,
      sourceType: data?.sourceType ?? sourceType,
      knowledgeName: data?.knowledgeName ?? knowledgeName,
      pipeline: data?.pipeline ?? pipeline,
      taskQueue: data?.taskQueue ?? queue,
      files: data?.files ?? fileNames,
    }
  } catch (err) {
    const msg = err instanceof Error ? err.message : 'Network error'
    return { success: false, message: msg }
  }
}

export async function startRagDelete(req: RagDeleteRequest): Promise<RagDeleteResult> {
  const knowledgeName = req.knowledgeName?.trim() ?? ''
  if (!knowledgeName) {
    return { success: false, message: 'Knowledge source name is required.' }
  }
  const body: Record<string, unknown> = {
    tenantId: req.tenantId?.trim() || 'default',
    sourceType: req.sourceType?.trim() || FILES_KNOWLEDGE_SOURCE_TYPE,
    knowledgeName,
  }
  const sourceCollection = req.sourceCollection?.trim()
  if (sourceCollection) body.sourceCollection = sourceCollection
  const queue = req.taskQueue ?? DELETE_QUEUE
  const pipeline = req.pipelineId ?? DELETE_PIPELINE
  if (queue) body.taskQueue = queue
  if (pipeline) body.pipelineId = pipeline

  try {
    const res = await fetch(`${API}/rag/delete`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...getApiAuthHeaders() },
      body: JSON.stringify(body),
    })
    const text = await res.text()
    const data = parseJson<RagDeleteResult & { message?: string }>(text)
    if (!res.ok || data?.success === false) {
      return {
        success: false,
        message: data?.message ?? `RAG delete failed (${res.status})`,
      }
    }
    return {
      success: true,
      runId: typeof data?.runId === 'string' ? data.runId : undefined,
      knowledgeName: data?.knowledgeName ?? knowledgeName,
      sourceType: data?.sourceType ?? String(body.sourceType),
      sourceCollection: data?.sourceCollection ?? sourceCollection,
      pipeline: data?.pipeline ?? pipeline,
      taskQueue: data?.taskQueue ?? queue,
    }
  } catch (err) {
    return {
      success: false,
      message: err instanceof Error ? err.message : 'Network error',
    }
  }
}

export async function listKnowledgeSources(): Promise<KnowledgeSourceDto[]> {
  try {
    const res = await fetch(`${API}/knowledge/sources`, {
      headers: getApiAuthHeaders(),
    })
    if (!res.ok) return []
    const data = parseJson<unknown>(await res.text())
    const list = Array.isArray(data)
      ? data
      : data && typeof data === 'object' && Array.isArray((data as Record<string, unknown>).sources)
        ? ((data as Record<string, unknown>).sources as unknown[])
        : []
    return list.map(normalizeKnowledgeSource).filter((s): s is KnowledgeSourceDto => Boolean(s))
  } catch {
    return []
  }
}

export async function listKnowledgeSourceCollections(): Promise<KnowledgeSourceDto[]> {
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
    return list.map(normalizeKnowledgeSource).filter((s): s is KnowledgeSourceDto => Boolean(s))
  } catch {
    return []
  }
}

export async function listUploadedDocuments(capabilitySource: string): Promise<UploadedDocumentDto[]> {
  const src = capabilitySource?.trim()
  if (!src) return []
  try {
    const url = `${API}/documents?${new URLSearchParams({ capabilitySource: src })}`
    const res = await fetch(url, { headers: getApiAuthHeaders() })
    if (!res.ok) return []
    const data = parseJson<UploadedDocumentDto[]>(await res.text())
    return Array.isArray(data) ? data : []
  } catch {
    return []
  }
}
