/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Ledger domain store: historical runs, cost analysis, snapshots, replay, run-level view state.
 * One store per domain — do not split by component.
 */
import { create } from 'zustand'

export interface LedgerState {
  // Placeholder: extend when implementing Runs, Cost Analysis, Snapshots, Replay
  selectedRunId: string | null
  setSelectedRunId: (id: string | null) => void
}

export const useLedgerStore = create<LedgerState>((set) => ({
  selectedRunId: null,
  setSelectedRunId: (id) => set({ selectedRunId: id }),
}))
