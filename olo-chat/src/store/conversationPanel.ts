/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { create } from 'zustand'

/**
 * Selected chat profile and derived queue/pipeline (from GET /api/ui/context `chatProfiles`).
 */
export interface ConversationPanelState {
  selectedProfileId: string
  setSelectedProfileId: (id: string) => void
  selectedQueueId: string
  setSelectedQueueId: (id: string) => void
  selectedPipelineId: string
  setSelectedPipelineId: (id: string) => void
}

export const conversationPanelStore = create<ConversationPanelState>((set) => ({
  selectedProfileId: '',
  setSelectedProfileId: (selectedProfileId) => set({ selectedProfileId }),
  selectedQueueId: '',
  setSelectedQueueId: (selectedQueueId) => set({ selectedQueueId }),
  selectedPipelineId: '',
  setSelectedPipelineId: (selectedPipelineId) => set({ selectedPipelineId }),
}))
