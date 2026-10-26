/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import type { SessionSummaryDto } from '../api/chatApi'
import { sessionDisplayStore, truncateLabel } from '../store/sessionDisplay'

const LABEL_MAX = 48
const PREVIEW_MAX = 40

function formatSessionLabel(createdAt: number): string {
  const d = new Date(createdAt)
  return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

export function getSessionDisplay(session: SessionSummaryDto): { primary: string; subtitle?: string } {
  const entry = sessionDisplayStore.getState().entries[session.sessionId]
  const customTitle = entry?.customTitle?.trim()
  const preview = entry?.firstMessagePreview?.trim()
  const fallback = formatSessionLabel(session.createdAt)
  const primary = customTitle || (preview ? truncateLabel(preview, LABEL_MAX) : fallback)
  const subtitle = customTitle && preview ? truncateLabel(preview, PREVIEW_MAX) : undefined
  return { primary, subtitle }
}
