/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Tools panel identity: each item has a clear purpose for infra UI.
 * Prevents "TOOLS" from feeling like an undefined placeholder.
 */
export interface ToolItem {
  id: string
  label: string
  description?: string
}

export const TOOLS: ToolItem[] = [
  { id: 'quick-actions', label: 'Quick actions', description: 'Run, pause, cancel' },
  { id: 'filters', label: 'Filters', description: 'Scope by status, time, tags' },
  { id: 'search', label: 'Search', description: 'Find runs, nodes, logs' },
  { id: 'shortcuts', label: 'Shortcuts', description: 'Keyboard & command palette' },
  { id: 'node-inspector', label: 'Node inspector', description: 'Inspect selected node' },
  { id: 'logs-preview', label: 'Logs preview', description: 'Tail logs for selection' },
]
