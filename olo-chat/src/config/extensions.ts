/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Stable public UI extension surface. Declarative only: extensions register metadata,
 * not behavior-heavy logic. The host owns injection points and resolves what to render.
 *
 * Rule: Extension API = declarative metadata + controlled injection points.
 * Avoid: registerSidebarItem({ render: () => ... }) or registerTool({ render: () => complex logic }).
 */

export type SectionId = string

/** Declarative only: id, section, label, path. No render functions. */
export interface SidebarItemRegistration {
  id: string
  section: SectionId
  label: string
  path: string
  /** Optional order hint (lower = earlier) */
  order?: number
}

/**
 * Declarative only: id, label, slot. The host decides what to render for each slot.
 * Do not add render(): use slot + host-owned component lookup so extensions stay metadata-only.
 */
export interface ToolRegistration {
  id: string
  label: string
  /** Injection point (e.g. 'filters' | 'actions' | 'preview'). Host resolves component. */
  slot?: string
}

const sidebarItems: SidebarItemRegistration[] = []
const tools: ToolRegistration[] = []

/**
 * Register a sidebar item (metadata only). Navigation uses path; no render logic.
 * Example: registerSidebarItem({ id: 'custom', section: 'runtime', label: 'Custom View', path: '/runtime/custom' })
 */
export function registerSidebarItem(item: SidebarItemRegistration): void {
  sidebarItems.push(item)
}

/**
 * Register a tool (metadata only). Host renders at slot; extensions do not pass render().
 * Example: registerTool({ id: 'custom-filter', label: 'Custom filter', slot: 'filters' })
 */
export function registerTool(tool: ToolRegistration): void {
  tools.push(tool)
}

export function getRegisteredSidebarItems(): readonly SidebarItemRegistration[] {
  return sidebarItems
}

export function getRegisteredTools(): readonly ToolRegistration[] {
  return tools
}
