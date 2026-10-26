/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

export interface CreateSessionResponse {
  sessionId: string
}

/** Session summary for list (GET /api/tenants/:tenantId/sessions). Most recently active first. */
export interface SessionSummaryDto {
  sessionId: string
  tenantId: string
  createdAt: number
  lastActivityAt?: number
}

export interface SendMessageResponse {
  messageId: string
  runId: string
}

export interface ChatMessageDto {
  messageId: string
  sessionId: string
  role: string
  content: string
  runId: string
  createdAt: number
}

export interface RunEventDto {
  eventVersion?: number
  runId: string
  nodeId: string
  parentNodeId: string | null
  nodeType: string
  status: string
  eventType?: string
  timestamp: number
  sequenceNumber?: number
  correlationId?: string
  input?: Record<string, unknown>
  output?: Record<string, unknown>
  metadata?: Record<string, unknown>
}

/** Tenant list from GET /api/tenants (olo backend). Used by configuration and related flows. */
export interface TenantDto {
  id: string
  name: string
}

/** One chat profile from regional/worker config (replaces separate Queue + Pipeline pickers when non-empty). */
export interface ChatProfileDto {
  id: string
  displayName: string
  /** Short description for tooltips / secondary UI (from pipeline JSON {@code display_summary}). */
  displaySummary?: string
  /** Optional emoji from config ({@code emoji} in pipeline JSON); otherwise inferred in the UI. */
  emoji?: string
  queue: string
  pipeline: string
  /** From pipeline JSON {@code run_again}; when true, profile is offered in per-message run-again UI. */
  runAgain?: boolean
}

/** Tenant id, footer labels, and Olo version from GET /api/ui/context. Send Bearer token so tenantId comes from JWT {@code tenantId} claim. */
export interface UiContextDto {
  tenantId: string
  tenant: string
  user: string
  /** Backend release label (olo.version / OLO_VERSION), e.g. v1.0.0-Dev */
  oloVersion: string
  /** Presets for chat (queue/pipeline per profile); required for the chat UI. */
  chatProfiles?: ChatProfileDto[]
}

/** Options for POST /api/sessions. Backend uses tenantId, taskQueue, queueName, pipelineId, and optional overrides (future). */
export interface CreateSessionBody {
  tenantId: string
  taskQueue?: string
  /** Queue name stored on the session (same as taskQueue when creating). */
  queueName?: string
  pipelineId?: string
  overrides?: Record<string, unknown>
}

/** Run status from GET /api/runs/{runId}. Used to un-gray when workflow completes but SSE did not deliver MODEL event. */
export interface RunStatusDto {
  runId: string
  status: string
}

/** Run response from GET /api/runs/{runId}/response. Use when receiving events or while run in progress to show current assistant reply. */
export interface RunResponseDto {
  runId: string
  response: string
}

export interface HumanInputRequestDto {
  approved: boolean
  message?: string
  /** Chat history line(s); when omitted, backend uses {@link message}. */
  historyText?: string
}
