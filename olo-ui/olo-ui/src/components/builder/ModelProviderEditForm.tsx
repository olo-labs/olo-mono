/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { useState } from 'react'
import { testModelProvider } from '../../api/rest'
import {
  MODEL_PROVIDER_KINDS,
  providerApiKey,
  providerBaseUrl,
  providerKind,
  type ModelProviderKind,
} from '../../lib/workflowModelProviders'
import { configurationForKind, modelForKind } from '../../lib/modelProviderFormHelpers'
import type { ModelProvider } from '../../types/workflow'

type TestState =
  | { status: 'idle' }
  | { status: 'running' }
  | { status: 'ok'; message: string; latencyMs?: number }
  | { status: 'error'; message: string }

export function ModelProviderEditForm({
  form,
  setForm,
  onCancel,
  onSave,
}: {
  form: ModelProvider
  setForm: (form: ModelProvider) => void
  onCancel: () => void
  onSave: () => void
}) {
  const [testState, setTestState] = useState<TestState>({ status: 'idle' })
  const kind = providerKind(form)

  const runTest = async () => {
    setTestState({ status: 'running' })
    try {
      const result = await testModelProvider({
        provider: form.provider,
        model: form.model,
        configuration: form.configuration,
      })
      if (result.ok) {
        setTestState({ status: 'ok', message: result.message, latencyMs: result.latencyMs })
      } else {
        setTestState({ status: 'error', message: result.message })
      }
    } catch (e) {
      setTestState({
        status: 'error',
        message: e instanceof Error ? e.message : 'Test request failed',
      })
    }
  }

  return (
    <div className="builder-variable-form">
      <label className="builder-field">
        <span>Id</span>
        <input className="builder-input" value={form.id} onChange={(e) => setForm({ ...form, id: e.target.value })} />
      </label>
      <label className="builder-field">
        <span>Type</span>
        <select
          className="builder-input"
          value={kind}
          onChange={(e) => {
            const nextKind = e.target.value as ModelProviderKind
            setForm({
              ...form,
              provider: nextKind,
              model: modelForKind(nextKind, form.model),
              configuration: configurationForKind(nextKind, form.configuration),
            })
            setTestState({ status: 'idle' })
          }}
        >
          {MODEL_PROVIDER_KINDS.map((item) => (
            <option key={item.id} value={item.id}>
              {item.label}
            </option>
          ))}
        </select>
      </label>
      <p className="builder-field-hint">{MODEL_PROVIDER_KINDS.find((k) => k.id === kind)?.description}</p>
      <label className="builder-field">
        <span>Model</span>
        <input
          className="builder-input"
          placeholder="e.g. gpt-4o, llama3.2"
          value={form.model ?? ''}
          onChange={(e) => setForm({ ...form, model: e.target.value })}
        />
      </label>
      <label className="builder-field">
        <span>Base URL</span>
        <input
          className="builder-input"
          value={providerBaseUrl(form)}
          onChange={(e) =>
            setForm({ ...form, configuration: { ...form.configuration, baseUrl: e.target.value } })
          }
        />
      </label>
      {kind === 'openai' || kind === 'url' ? (
        <label className="builder-field">
          <span>API key (optional)</span>
          <input
            className="builder-input"
            type="password"
            autoComplete="off"
            placeholder={kind === 'openai' ? 'sk-… or ${env:OPENAI_API_KEY}' : 'Bearer token'}
            value={providerApiKey(form)}
            onChange={(e) =>
              setForm({
                ...form,
                configuration: { ...form.configuration, apiKey: e.target.value || undefined },
              })
            }
          />
        </label>
      ) : null}
      <div className="builder-form-actions">
        <button type="button" className="builder-btn small" onClick={runTest} disabled={testState.status === 'running'}>
          {testState.status === 'running' ? 'Testing…' : 'Test connection'}
        </button>
      </div>
      {testState.status === 'ok' ? (
        <p className="model-provider-test-ok">
          {testState.message}
          {testState.latencyMs != null ? ` (${testState.latencyMs}ms)` : ''}
        </p>
      ) : null}
      {testState.status === 'error' ? <p className="model-provider-test-error">{testState.message}</p> : null}
      <div className="builder-form-actions">
        <button type="button" className="builder-btn small" onClick={onCancel}>
          Cancel
        </button>
        <button type="button" className="builder-btn small primary" onClick={onSave}>
          Save
        </button>
      </div>
    </div>
  )
}
