/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { create } from 'zustand'
import * as api from '../api/rest'
import type { Tenant } from '../types/tenant'

export interface TenantConfigState {
  tenants: Tenant[]
  tenantsLoading: boolean
  configSelectedTenant: Tenant | null
  configIsAddingNew: boolean

  loadTenants: () => Promise<void>
  selectTenant: (tenant: Tenant | null) => void
  startAddNew: () => void
  clearSelection: () => void
  saveTenant: (tenant: Tenant) => Promise<void>
  deleteTenant: (id: string) => Promise<void>
}

export const tenantConfigStore = create<TenantConfigState>((set, get) => ({
  tenants: [],
  tenantsLoading: false,
  configSelectedTenant: null,
  configIsAddingNew: false,

  loadTenants: async () => {
    set({ tenantsLoading: true })
    try {
      const list = await api.getTenants()
      set({ tenants: list })
    } catch {
      set({ tenants: [] })
    } finally {
      set({ tenantsLoading: false })
    }
  },

  selectTenant: (tenant) => set({ configSelectedTenant: tenant, configIsAddingNew: false }),

  startAddNew: () => set({ configIsAddingNew: true, configSelectedTenant: null }),

  clearSelection: () => set({ configSelectedTenant: null, configIsAddingNew: false }),

  saveTenant: async (tenant) => {
    const { configIsAddingNew } = get()
    if (configIsAddingNew) {
      await api.saveTenant(tenant)
    } else {
      await api.updateTenant(tenant.id, tenant)
    }
    await get().loadTenants()
    set({ configSelectedTenant: null, configIsAddingNew: false })
  },

  deleteTenant: async (id) => {
    await api.deleteTenant(id)
    await get().loadTenants()
    set({ configSelectedTenant: null, configIsAddingNew: false })
  },
}))
