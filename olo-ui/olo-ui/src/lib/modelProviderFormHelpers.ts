/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { ModelProviderKind } from './workflowModelProviders'
import { MODEL_PROVIDER_KINDS } from './modelProviderDefaults'

export function configurationForKind(
  kind: ModelProviderKind,
  current: Record<string, unknown> | undefined,
): Record<string, unknown> {
  const meta = MODEL_PROVIDER_KINDS.find((k) => k.id === kind)!
  const next: Record<string, unknown> = { ...current }
  if (!next.baseUrl) next.baseUrl = meta.defaultBaseUrl
  if (kind !== 'openai' && kind !== 'url') {
    delete next.apiKey
    delete next.apiKeyRef
  }
  return next
}

export function modelForKind(kind: ModelProviderKind, currentModel: string | undefined): string {
  const meta = MODEL_PROVIDER_KINDS.find((k) => k.id === kind)!
  if (currentModel?.trim()) return currentModel
  return meta.defaultModel ?? ''
}
