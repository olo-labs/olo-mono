/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { useState, useEffect } from 'react'
import type { Tenant } from '../types/tenant'

export interface TenantConfigFormProps {
  tenant: Tenant | null
  isAddingNew: boolean
  onSave: (tenant: Tenant) => Promise<void>
}

export function TenantConfigForm({ tenant, isAddingNew, onSave }: TenantConfigFormProps) {
  const [id, setId] = useState('')
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [configVersion, setConfigVersion] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (tenant) {
      setId(tenant.id)
      setName(tenant.name ?? '')
      setDescription(tenant.description ?? '')
      setConfigVersion(tenant.configVersion ?? '')
    } else {
      setId('')
      setName('')
      setDescription('')
      setConfigVersion('')
    }
  }, [tenant])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    const tid = id.trim()
    if (!tid) {
      setError('ID is required')
      return
    }
    setError(null)
    setSaving(true)
    try {
      await onSave({
        id: tid,
        name: name.trim(),
        description: description.trim(),
        configVersion: configVersion.trim(),
      })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save')
    } finally {
      setSaving(false)
    }
  }

  if (!tenant && !isAddingNew) {
    return (
      <div className="tenant-config-form-empty">
        Select a tenant from the list or click + to add a new one.
      </div>
    )
  }

  return (
    <form className="tenant-config-form-inner" onSubmit={handleSubmit}>
      <div className="side-panel-title">Tenant details</div>
      {error && (
        <div className="tenant-config-error" role="alert">
          {error}
        </div>
      )}
      <div className="tenant-config-form-row">
        <label className="tenant-config-label">ID</label>
        <input
          className="tenant-config-input"
          value={id}
          onChange={(e) => setId(e.target.value)}
          placeholder="e.g. UUID or tenant key"
          required
          readOnly={!!tenant}
        />
      </div>
      <div className="tenant-config-form-row">
        <label className="tenant-config-label">Name</label>
        <input
          className="tenant-config-input"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Display name"
        />
      </div>
      <div className="tenant-config-form-row">
        <label className="tenant-config-label">Description</label>
        <input
          className="tenant-config-input"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="Optional description"
        />
      </div>
      <div className="tenant-config-form-row">
        <label className="tenant-config-label">Config version</label>
        <input
          className="tenant-config-input"
          value={configVersion}
          onChange={(e) => setConfigVersion(e.target.value)}
          placeholder="e.g. 1.0"
        />
      </div>
      <div className="tenant-config-form-actions tenant-config-form-actions-bottom">
        <button type="submit" className="tenant-config-btn primary" disabled={saving}>
          {saving ? 'Saving…' : (tenant ? 'Update' : 'Add')}
        </button>
      </div>
    </form>
  )
}
