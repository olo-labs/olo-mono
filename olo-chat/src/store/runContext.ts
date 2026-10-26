/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Run context: shared state for a run when shown in Runtime or Ledger (overview, tree, timeline, etc.).
 *
 * Narrow responsibility: run identity + shared run payload only.
 * Do not add: feature flags, generic UI state, or domain-specific state. Use URL or owning store.
 */
import { create } from 'zustand'

export interface RunContextState {
  runId: string | null
  runPayload: unknown | null
  setRun: (runId: string | null, runPayload?: unknown) => void
}

export const useRunContextStore = create<RunContextState>((set) => ({
  runId: null,
  runPayload: null,
  setRun: (runId, runPayload = null) => set({ runId, runPayload }),
}))
