/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

/** Parsed from POST /api/resource/upload JSON body when the backend returns structured data. */
export interface DocumentUploadResult {
  success: boolean
  message?: string
  /** Single workflow run (multi-file upload). */
  runId?: string
  /** Multiple runs (optional). */
  runIds?: string[]
  /** Per-file run ids when the API returns them. */
  files?: { fileName: string; runId?: string }[]
}

/** Multipart upload: capability source id + files (BE persists to shared storage / object store). */
export interface ResourceUploadMultipartOptions {
  capabilitySource: string
  files: File[]
  taskQueue?: string
  pipelineId?: string
}

/** Upload request; BE persists bytes (local dir and/or object store per server config). */
export interface CapabilitySourceUploadRequest {
  capabilitySource: string
  files: File[]
}
