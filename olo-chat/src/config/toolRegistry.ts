/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Tool registry: map of tool ID → metadata; optional component registry for host-owned tool UIs.
 * Contextual tools per sub-option are defined in layout (SubOption.toolIds).
 *
 * Rules:
 * - Tools array per sub-option (see types/layout.ts SubOption.toolIds).
 * - Tool components talk only to owning store (receive storeContext from host; no API, no other stores).
 * - Main always renders primary output; Tools panel shows contextual tools only.
 */

import type { ComponentType } from 'react'
import type { SectionId } from '../types/layout'
import { SECTIONS } from '../types/layout'

export interface ToolDef {
  id: string
  label: string
  description?: string
  /** Optional slot for extension point (e.g. 'filters' | 'actions') */
  slot?: string
}

/** Context passed to tool components. Host provides owning-store slice; tools must not import stores or API. */
export interface ToolContext {
  sectionId: SectionId | null
  subId: string
  runSelected: boolean
  /** Slice from the owning store for this section (e.g. tenantConfig for configuration, runtime for runtime). */
  storeContext: Record<string, unknown>
}

export type ToolComponent = ComponentType<{ context: ToolContext }>

/** Registry map: tool ID → metadata */
const registry = new Map<string, ToolDef>([
  ['quick-actions', { id: 'quick-actions', label: 'Quick actions', description: 'Run, pause, cancel', slot: 'actions' }],
  ['filters', { id: 'filters', label: 'Filters', description: 'Scope by status, time, tags', slot: 'filters' }],
  ['search', { id: 'search', label: 'Search', description: 'Find runs, nodes, logs', slot: 'search' }],
  ['shortcuts', { id: 'shortcuts', label: 'Shortcuts', description: 'Keyboard & command palette', slot: 'actions' }],
  ['node-inspector', { id: 'node-inspector', label: 'Node inspector', description: 'Inspect selected node', slot: 'preview' }],
  ['logs-preview', { id: 'logs-preview', label: 'Logs preview', description: 'Tail logs for selection', slot: 'preview' }],
])

/** Optional: tool ID → host-owned component. Components receive ToolContext and talk only to owning store. */
const componentRegistry = new Map<string, ToolComponent>()

/**
 * Register a tool (metadata). Used by extensions or app bootstrap.
 * Overwrites existing entry for the same id.
 */
export function registerTool(def: ToolDef): void {
  registry.set(def.id, def)
}

/**
 * Register a component for a tool. Host owns the component; it receives context with owning-store slice.
 * Tool components must not import API or other domain stores.
 */
export function registerToolComponent(id: string, component: ToolComponent): void {
  componentRegistry.set(id, component)
}

/**
 * Get metadata for a single tool by id.
 */
export function getTool(id: string): ToolDef | undefined {
  return registry.get(id)
}

/**
 * Get the registered component for a tool, if any.
 */
export function getToolComponent(id: string): ToolComponent | undefined {
  return componentRegistry.get(id)
}

/**
 * Get the list of tools for the current view (section + sub, or run-level sub).
 * Returns tool definitions in order of SubOption.toolIds; unknown ids are skipped.
 */
export function getToolsForView(
  sectionId: SectionId | null,
  subId: string,
  runSelected: boolean
): ToolDef[] {
  if (!sectionId) return []
  const section = SECTIONS.find((s) => s.id === sectionId)
  if (!section) return []
  const options = runSelected && section.runSelectedOptions?.length
    ? section.runSelectedOptions
    : section.subOptions
  const subOption = options.find((o) => o.id === subId)
  const toolIds = subOption?.toolIds ?? []
  const out: ToolDef[] = []
  for (const id of toolIds) {
    const def = registry.get(id)
    if (def) out.push(def)
  }
  return out
}

export { registry as toolRegistryMap, componentRegistry as toolComponentRegistry }
