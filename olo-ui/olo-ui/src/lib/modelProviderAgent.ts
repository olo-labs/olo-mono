/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { WorkflowDocument } from '../types/workflow'
import {
  DEFAULT_ROUTING_ID,
  ensureWorkflowModelInfrastructure,
  primaryModelRouting,
  providerKind,
  workflowModelProviders,
  workflowModelRouting,
} from './modelProviderDefaults'
import type { ModelProvider } from '../types/workflow'

export function upsertModelProvider(
  doc: WorkflowDocument,
  provider: ModelProvider,
  previousId?: string,
): WorkflowDocument {
  const list = workflowModelProviders(doc).filter((p) => p.id !== (previousId ?? provider.id))
  return ensureWorkflowModelInfrastructure({ ...doc, modelProviders: [...list, provider] })
}

export function removeModelProvider(doc: WorkflowDocument, id: string): WorkflowDocument {
  const providers = workflowModelProviders(doc).filter((p) => p.id !== id)
  let next: WorkflowDocument = { ...doc, modelProviders: providers }
  const routing = primaryModelRouting(next)
  if (routing?.defaultProviderId === id && providers[0]) {
    next = syncRoutingDefaultProvider(next, providers[0].id)
  }
  return ensureWorkflowModelInfrastructure(next)
}

export function syncRoutingDefaultProvider(
  doc: WorkflowDocument,
  defaultProviderId: string,
): WorkflowDocument {
  const routing = workflowModelRouting(doc)
  if (routing.length === 0) {
    return { ...doc, modelRouting: [{ id: DEFAULT_ROUTING_ID, defaultProviderId, rules: [], metadata: {} }] }
  }
  return {
    ...doc,
    modelRouting: routing.map((entry, index) =>
      index === 0 ? { ...entry, id: entry.id ?? DEFAULT_ROUTING_ID, defaultProviderId } : entry,
    ),
  }
}

export function setPrimaryRoutingDefaultProvider(
  doc: WorkflowDocument,
  defaultProviderId: string,
): WorkflowDocument {
  return syncRoutingDefaultProvider(doc, defaultProviderId)
}

export interface AgentModelOption {
  value: string
  label: string
  group: 'routing' | 'provider'
  description?: string
}

export function agentModelOptions(doc: WorkflowDocument): AgentModelOption[] {
  const options: AgentModelOption[] = []
  for (const route of workflowModelRouting(doc)) {
    if (!route.id) continue
    const provider = workflowModelProviders(doc).find((p) => p.id === route.defaultProviderId)
    const providerLabel = provider
      ? `${provider.id} (${providerKind(provider)} · ${provider.model || 'default'})`
      : route.defaultProviderId ?? 'unset'
    options.push({
      value: `routing:${route.id}`,
      label: route.id,
      group: 'routing',
      description: `Default → ${providerLabel}`,
    })
  }
  for (const provider of workflowModelProviders(doc)) {
    options.push({
      value: `provider:${provider.id}`,
      label: provider.id,
      group: 'provider',
      description: `${providerKind(provider)} · ${provider.model || 'default'}`,
    })
  }
  return options
}

export function readAgentModelSelection(
  node: { configuration?: Record<string, unknown> },
  doc: WorkflowDocument,
): string {
  const routingRef = node.configuration?.routingRef
  if (typeof routingRef === 'string' && routingRef.trim()) {
    return `routing:${routingRef}`
  }
  const providerRef = readProviderRef(node)
  if (providerRef) return `provider:${providerRef}`
  const defaultRouting = primaryModelRouting(doc)
  if (defaultRouting?.id) return `routing:${defaultRouting.id}`
  const first = workflowModelProviders(doc)[0]
  return first ? `provider:${first.id}` : ''
}

export function applyAgentModelSelection(
  doc: WorkflowDocument,
  nodeId: string,
  selection: string,
): WorkflowDocument {
  const nodes = (doc.nodes ?? []).map((node) => {
    if (node.id !== nodeId) return node
    const configuration = { ...node.configuration }
    if (!selection) {
      delete configuration.routingRef
      delete configuration.providerRef
      return { ...node, configuration }
    }
    if (selection.startsWith('routing:')) {
      const routingRef = selection.slice('routing:'.length)
      configuration.routingRef = routingRef
      delete configuration.providerRef
      return { ...node, configuration }
    }
    if (selection.startsWith('provider:')) {
      const providerRef = selection.slice('provider:'.length)
      configuration.providerRef = providerRef
      delete configuration.routingRef
      return { ...node, configuration }
    }
    configuration.providerRef = selection
    delete configuration.routingRef
    return { ...node, configuration }
  })
  return { ...doc, nodes }
}

export function applyProviderRef(
  doc: WorkflowDocument,
  nodeId: string,
  providerRef: string,
): WorkflowDocument {
  const nodes = (doc.nodes ?? []).map((node) => {
    if (node.id !== nodeId) return node
    return {
      ...node,
      configuration: {
        ...node.configuration,
        providerRef: providerRef || undefined,
      },
    }
  })
  return { ...doc, nodes }
}

export function readProviderRef(node: { configuration?: Record<string, unknown> }): string {
  const value = node.configuration?.providerRef
  return typeof value === 'string' ? value : ''
}
