/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { useState } from 'react'
import { workflowConfigurationStore } from '../../store/workflowConfigurationStore'
import {
  DEFAULT_ROUTING_ID,
  emptyModelProvider,
  primaryModelRouting,
  providerKind,
  removeModelProvider,
  setPrimaryRoutingDefaultProvider,
  upsertModelProvider,
  workflowModelProviders,
} from '../../lib/workflowModelProviders'
import type { ModelProvider } from '../../types/workflow'
import { ModelProviderEditForm } from './ModelProviderEditForm'

export function ModelProvidersSection() {
  const draft = workflowConfigurationStore((s) => s.draft)
  const updateDraft = workflowConfigurationStore((s) => s.updateDraft)
  const providers = draft ? workflowModelProviders(draft) : []
  const defaultRouting = draft ? primaryModelRouting(draft) : null
  const [editingId, setEditingId] = useState<string | null>(null)
  const [form, setForm] = useState<ModelProvider | null>(null)

  if (!draft) {
    return <p className="builder-empty">Open a workflow to edit model providers.</p>
  }

  const startEdit = (provider: ModelProvider) => {
    setEditingId(provider.id)
    setForm({ ...provider, configuration: { ...provider.configuration } })
  }

  const startAdd = () => {
    setEditingId('__new__')
    setForm(emptyModelProvider(providers))
  }

  const cancelEdit = () => {
    setEditingId(null)
    setForm(null)
  }

  const saveForm = () => {
    if (!form || !form.id.trim() || !form.provider.trim()) return
    const previous = editingId === '__new__' ? undefined : editingId ?? undefined
    updateDraft(
      upsertModelProvider(
        draft,
        {
          ...form,
          id: form.id.trim(),
          provider: form.provider.trim(),
          model: form.model?.trim() ?? '',
        },
        previous,
      ),
    )
    cancelEdit()
  }

  const handleDelete = (id: string) => {
    updateDraft(removeModelProvider(draft, id))
    if (editingId === id) cancelEdit()
  }

  return (
    <div className="builder-variables builder-model-providers">
      {defaultRouting && providers.length > 0 ? (
        <div className="builder-routing-default">
          <label className="builder-field">
            <span>Default routing ({defaultRouting.id ?? DEFAULT_ROUTING_ID})</span>
            <select
              className="builder-input"
              value={defaultRouting.defaultProviderId ?? ''}
              onChange={(e) => updateDraft(setPrimaryRoutingDefaultProvider(draft, e.target.value))}
            >
              {providers.map((provider) => (
                <option key={provider.id} value={provider.id}>
                  {provider.id} ({providerKind(provider)} · {provider.model || 'default'})
                </option>
              ))}
            </select>
          </label>
          <p className="builder-field-hint">
            Agents using the default routing profile resolve to this provider unless overridden.
          </p>
        </div>
      ) : null}
      <button type="button" className="builder-btn small" onClick={startAdd}>
        + Add model provider
      </button>
      {providers.length === 0 && editingId !== '__new__' ? (
        <p className="builder-empty">No model providers defined.</p>
      ) : (
        <ul className="builder-check-list">
          {providers.map((provider) => (
            <li key={provider.id} className="builder-variable-item">
              <button type="button" className="builder-variable-summary" onClick={() => startEdit(provider)}>
                <code>{provider.id}</code>
                <span className="builder-variable-type">
                  {providerKind(provider)} · {provider.model || 'no model'}
                </span>
              </button>
              <button
                type="button"
                className="builder-icon-btn danger"
                onClick={() => handleDelete(provider.id)}
                title="Remove model provider"
              >
                ×
              </button>
            </li>
          ))}
        </ul>
      )}
      {form ? (
        <ModelProviderEditForm form={form} setForm={setForm} onCancel={cancelEdit} onSave={saveForm} />
      ) : null}
    </div>
  )
}
