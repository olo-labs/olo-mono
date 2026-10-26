/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { WorkflowDocument, WorkflowNode } from '../types/workflow'

export function readNodeConfigValue(node: WorkflowNode, key: string, fallback = ''): string {
  const raw = node.configuration?.[key]
  if (raw === undefined || raw === null) return fallback
  return String(raw)
}

export function readNodeConfigNumber(node: WorkflowNode, key: string, fallback: number): number {
  const raw = node.configuration?.[key]
  if (typeof raw === 'number' && Number.isFinite(raw)) return raw
  if (raw !== undefined && raw !== null) {
    const parsed = Number(String(raw))
    if (Number.isFinite(parsed)) return parsed
  }
  return fallback
}

export function applyNodeConfigValue(
  workflow: WorkflowDocument,
  nodeId: string,
  key: string,
  value: string | number,
): WorkflowDocument {
  return {
    ...workflow,
    nodes: (workflow.nodes ?? []).map((node) =>
      node.id === nodeId
        ? {
            ...node,
            configuration: {
              ...(node.configuration ?? {}),
              [key]: value,
            },
          }
        : node,
    ),
  }
}

export function readToolSubtypeKey(node: WorkflowNode): string {
  const toolId = readNodeConfigValue(node, 'toolId')
  if (!toolId) return ''
  const idx = toolId.lastIndexOf(':')
  return idx >= 0 ? toolId.slice(idx + 1) : toolId
}
