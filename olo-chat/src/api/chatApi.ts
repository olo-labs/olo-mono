/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Chat API for olo backend (sessions, messages, runs, SSE).
 * Uses VITE_API_BASE (e.g. http://localhost:7080) so requests go to BE port 7080; if unset, uses /api (Vite proxy to 7080).
 */

export { tenantIdForApiPath } from './chatApiClient'
export type {
  ChatMessageDto,
  ChatProfileDto,
  CreateSessionBody,
  CreateSessionResponse,
  HumanInputRequestDto,
  RunEventDto,
  RunResponseDto,
  RunStatusDto,
  SendMessageResponse,
  SessionSummaryDto,
  TenantDto,
  UiContextDto,
} from './chatTypes'
export { getChatBackendHealth, getTenants, getUiContext } from './chatContextApi'
export {
  createSession,
  deleteAllSessions,
  deleteSession,
  listMessages,
  listSessions,
  sendMessage,
} from './chatSessionsApi'
export { getRun, getRunResponse, submitHumanInput, cancelRun } from './chatRunsApi'
export { streamRunEvents } from './chatSseApi'
