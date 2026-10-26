/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Plugins domain store: executor registry, plugin metadata.
 * One store per domain — do not split by component.
 */
import { create } from 'zustand'

export interface PluginsState {
  // Placeholder: extend when implementing Plugins / Executor registry
  registry: unknown[]
  setRegistry: (r: unknown[]) => void
}

export const usePluginsStore = create<PluginsState>((set) => ({
  registry: [],
  setRegistry: (registry) => set({ registry }),
}))
