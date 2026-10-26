/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { create } from 'zustand'
import * as api from '../api/rest'
import { getTenants as getTenantsFromOlo } from '../api/chatApi'
import type { Tenant } from '../types/tenant'

const DEFAULT_TENANT: Tenant = { id: 'default', name: 'Default' }

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

  /** Load tenants from GET /api/tenants for the Chat UI top dropdown. */
  loadTenants: async () => {
    set({ tenantsLoading: true })
    try {
      const list = await getTenantsFromOlo()
      const tenants: Tenant[] = list.length > 0
        ? list.map((t) => ({ id: t.id, name: t.name }))
        : [DEFAULT_TENANT]
      set({ tenants })
    } catch {
      set({ tenants: [DEFAULT_TENANT] })
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
