/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Runtime domain store: live runs, queues, metrics, run-level view state.
 * One store per domain — do not split by component (e.g. no liveRunsStore, metricsStore).
 */
import { create } from 'zustand'

export interface RuntimeState {
  // Placeholder: extend when implementing Live Runs, Queues, Metrics, Node Inspector
  selectedRunId: string | null
  setSelectedRunId: (id: string | null) => void
}

export const useRuntimeStore = create<RuntimeState>((set) => ({
  selectedRunId: null,
  setSelectedRunId: (id) => set({ selectedRunId: id }),
}))
