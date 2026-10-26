/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { create } from 'zustand'
import * as api from '../api/rest'
import type { StudioCatalog } from '../types/catalog'

export interface CatalogState {
  catalog: StudioCatalog | null
  loading: boolean
  error: string | null
  loadCatalog: () => Promise<void>
}

export const catalogStore = create<CatalogState>((set) => ({
  catalog: null,
  loading: false,
  error: null,

  loadCatalog: async () => {
    set({ loading: true, error: null })
    try {
      const catalog = await api.getCatalog()
      set({ catalog, loading: false })
    } catch (e) {
      set({
        catalog: null,
        loading: false,
        error: e instanceof Error ? e.message : 'Failed to load catalog',
      })
    }
  },
}))
