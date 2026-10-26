/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { create } from 'zustand'
import type { SessionSummaryDto } from '../api/chatApi'

export interface ChatSessionsState {
  sessions: SessionSummaryDto[]
  selectedSessionId: string | null
  setSessions: (sessions: SessionSummaryDto[]) => void
  setSelectedSessionId: (id: string | null) => void
}

export const chatSessionsStore = create<ChatSessionsState>((set) => ({
  sessions: [],
  selectedSessionId: null,

  setSessions: (sessions) => set({ sessions }),

  setSelectedSessionId: (selectedSessionId) => set({ selectedSessionId }),
}))
