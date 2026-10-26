/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Schema / Studio domain store: canvas, versions, schema editing, test run results.
 * One store per domain — do not split by component (e.g. no canvasStore, versionsStore).
 */
import { create } from 'zustand'

export interface SchemaState {
  // Placeholder: extend when implementing Canvas, Versions, Validate, Test Run, Schema, Simulation
  draftVersion: string | null
  setDraftVersion: (v: string | null) => void
}

export const useSchemaStore = create<SchemaState>((set) => ({
  draftVersion: null,
  setDraftVersion: (draftVersion) => set({ draftVersion }),
}))
