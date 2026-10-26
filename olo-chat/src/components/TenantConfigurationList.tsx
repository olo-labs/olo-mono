/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { useState } from 'react'
import { tenantDisplayName } from '../lib/tenantDisplay'
import type { Tenant } from '../types/tenant'

export interface TenantConfigurationListProps {
  tenants: Tenant[]
  loading: boolean
  selectedTenantId: string | null
  onSelectTenant: (tenant: Tenant) => void
  onAddNew: () => void
  onDeleteTenant: (id: string) => void
}

export function TenantConfigurationList({
  tenants,
  loading,
  selectedTenantId,
  onSelectTenant,
  onAddNew,
  onDeleteTenant,
}: TenantConfigurationListProps) {
  const [contextMenu, setContextMenu] = useState<{ x: number; y: number; tenant: Tenant } | null>(null)

  const handleContextMenu = (e: React.MouseEvent, tenant: Tenant) => {
    e.preventDefault()
    setContextMenu({ x: e.clientX, y: e.clientY, tenant })
  }

  const handleDelete = () => {
    if (contextMenu) {
      onDeleteTenant(contextMenu.tenant.id)
      setContextMenu(null)
    }
  }

  const closeContextMenu = () => setContextMenu(null)

  return (
    <div className="tenant-config-list">
      <div className="tenant-config-list-header">
        <span className="tenant-config-list-title">Tenants</span>
        <button
          type="button"
          className="tenant-config-add-btn"
          onClick={onAddNew}
          title="Add new tenant"
          aria-label="Add new tenant"
        >
          +
        </button>
      </div>
      {loading ? (
        <p className="tenant-config-message">Loading…</p>
      ) : tenants.length === 0 ? (
        <p className="tenant-config-message">No tenants. Click + to add.</p>
      ) : (
        <ul className="tenant-config-list-ul">
          {tenants.map((t) => (
            <li
              key={t.id}
              className={`tenant-config-list-item ${selectedTenantId === t.id ? 'selected' : ''}`}
              onClick={() => onSelectTenant(t)}
              onContextMenu={(e) => handleContextMenu(e, t)}
            >
              {tenantDisplayName(t)}
            </li>
          ))}
        </ul>
      )}
      {contextMenu && (
        <>
          <div className="tenant-config-context-backdrop" onClick={closeContextMenu} onContextMenu={closeContextMenu} />
          <div
            className="tenant-config-context-menu"
            style={{ left: contextMenu.x, top: contextMenu.y }}
          >
            <button type="button" className="tenant-config-context-item danger" onClick={handleDelete}>
              Delete tenant
            </button>
          </div>
        </>
      )}
    </div>
  )
}
