/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { ModelProvider, ModelRouting, WorkflowDocument } from '../types/workflow'

export const DEFAULT_LOCAL_OLLAMA_URL = 'http://localhost:51435'
export const DEFAULT_LOCAL_OLLAMA_MODEL = 'llama3.2:latest'
export const DEFAULT_PROVIDER_ID = 'default-local'
export const DEFAULT_ROUTING_ID = 'default-routing'

export const MODEL_PROVIDER_KINDS = [
  {
    id: 'local',
    label: 'Local (Ollama)',
    description: 'Models served on your machine (Ollama, LM Studio, etc.)',
    defaultBaseUrl: DEFAULT_LOCAL_OLLAMA_URL,
    defaultModel: DEFAULT_LOCAL_OLLAMA_MODEL,
  },
  {
    id: 'url',
    label: 'OpenAI-compatible URL',
    description: 'Remote HTTP endpoint exposing /v1/chat/completions or /v1/models',
    defaultBaseUrl: 'http://localhost:8080/v1',
    defaultModel: '',
  },
  {
    id: 'openai',
    label: 'OpenAI',
    description: 'OpenAI API (api.openai.com)',
    defaultBaseUrl: 'https://api.openai.com/v1',
    defaultModel: 'gpt-4o',
  },
] as const

export type ModelProviderKind = (typeof MODEL_PROVIDER_KINDS)[number]['id']

export function workflowModelProviders(doc: WorkflowDocument): ModelProvider[] {
  return Array.isArray(doc.modelProviders) ? [...doc.modelProviders] : []
}

export function workflowModelRouting(doc: WorkflowDocument): ModelRouting[] {
  return Array.isArray(doc.modelRouting) ? [...doc.modelRouting] : []
}

export function primaryModelRouting(doc: WorkflowDocument): ModelRouting | null {
  const routing = workflowModelRouting(doc)
  return routing.find((r) => r.id === DEFAULT_ROUTING_ID) ?? routing[0] ?? null
}

export function defaultLocalProvider(): ModelProvider {
  return {
    id: DEFAULT_PROVIDER_ID,
    provider: 'local',
    model: DEFAULT_LOCAL_OLLAMA_MODEL,
    configuration: { baseUrl: DEFAULT_LOCAL_OLLAMA_URL },
  }
}

export function defaultModelRouting(defaultProviderId: string): ModelRouting {
  return {
    id: DEFAULT_ROUTING_ID,
    defaultProviderId,
    rules: [],
    metadata: {},
  }
}

export function ensureWorkflowModelInfrastructure(doc: WorkflowDocument): WorkflowDocument {
  let providers = workflowModelProviders(doc)
  if (providers.length === 0) {
    providers = [defaultLocalProvider()]
  }

  let routing = workflowModelRouting(doc)
  const primaryProviderId = providers[0]?.id ?? DEFAULT_PROVIDER_ID
  if (routing.length === 0) {
    routing = [defaultModelRouting(primaryProviderId)]
  } else {
    routing = routing.map((entry, index) => {
      if (index !== 0) return entry
      const defaultProviderId =
        entry.defaultProviderId && providers.some((p) => p.id === entry.defaultProviderId)
          ? entry.defaultProviderId
          : primaryProviderId
      return { ...entry, id: entry.id ?? DEFAULT_ROUTING_ID, defaultProviderId }
    })
  }

  return { ...doc, modelProviders: providers, modelRouting: routing }
}

export function uniqueModelProviderId(existing: ModelProvider[]): string {
  const taken = new Set(existing.map((p) => p.id))
  let base = 'model-provider'
  let n = 1
  while (taken.has(n === 1 ? base : `${base}-${n}`)) n += 1
  return n === 1 ? base : `${base}-${n}`
}

export function emptyModelProvider(existing: ModelProvider[]): ModelProvider {
  const kind: ModelProviderKind = 'local'
  const meta = MODEL_PROVIDER_KINDS.find((k) => k.id === kind)!
  return {
    id: uniqueModelProviderId(existing),
    provider: kind,
    model: meta.defaultModel ?? '',
    configuration: { baseUrl: meta.defaultBaseUrl },
  }
}

export function providerKind(provider: ModelProvider): ModelProviderKind {
  const value = provider.provider?.toLowerCase()
  if (value === 'ollama') return 'local'
  if (value === 'local' || value === 'url' || value === 'openai') return value
  return 'url'
}

export function providerBaseUrl(provider: ModelProvider): string {
  const kind = providerKind(provider)
  const fromConfig = provider.configuration?.baseUrl
  if (typeof fromConfig === 'string' && fromConfig.trim()) return fromConfig.trim()
  return MODEL_PROVIDER_KINDS.find((k) => k.id === kind)?.defaultBaseUrl ?? ''
}

export function providerApiKey(provider: ModelProvider): string {
  const raw = provider.configuration?.apiKey ?? provider.configuration?.apiKeyRef
  return typeof raw === 'string' ? raw : ''
}

export function isAgentNodeType(type: string): boolean {
  return type.toUpperCase() === 'AGENT'
}

export function isModelConsumerNodeType(type: string): boolean {
  const upper = type.toUpperCase()
  return upper === 'AGENT' || upper === 'MODEL'
}
