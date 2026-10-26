/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { getApiPathPrefix } from '../lib/apiBase'
import { getApiAuthHeaders } from '../lib/wsUrl'

export const CHAT_API = getApiPathPrefix()

export function withAuth(headers: Record<string, string> = {}): Record<string, string> {
  return { ...getApiAuthHeaders(), ...headers }
}

/** Trims tenant id for URL paths and bodies (from GET /api/ui/context tenantId). */
export function tenantIdForApiPath(tenantId: string): string {
  return tenantId?.trim() ?? ''
}
