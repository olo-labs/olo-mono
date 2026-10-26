/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { create } from 'zustand'
import type { SectionId } from '../types/layout'

const THEME_KEY = 'olo-theme'
const PANEL_WIDTHS_KEY = 'olo:panel-widths'
export type Theme = 'light' | 'dark'

const DEFAULT_LEFT = 260
const DEFAULT_TOOLS = 220
const DEFAULT_PROPERTIES = 260
const MIN_PANEL = 160
const MAX_LEFT = 480
const MAX_TOOLS = 400
const MAX_PROPERTIES = 480

function getInitialTheme(): Theme {
  if (typeof window === 'undefined') return 'dark'
  const stored = window.localStorage.getItem(THEME_KEY) as Theme | null
  return stored === 'light' || stored === 'dark' ? stored : 'dark'
}

function getStoredPanelWidths(): { left: number; tools: number; properties: number } {
  if (typeof window === 'undefined') return { left: DEFAULT_LEFT, tools: DEFAULT_TOOLS, properties: DEFAULT_PROPERTIES }
  try {
    const raw = window.localStorage.getItem(PANEL_WIDTHS_KEY)
    if (!raw) return { left: DEFAULT_LEFT, tools: DEFAULT_TOOLS, properties: DEFAULT_PROPERTIES }
    const parsed = JSON.parse(raw) as { left?: number; tools?: number; properties?: number }
    return {
      left: clamp(Number(parsed.left), MIN_PANEL, MAX_LEFT) || DEFAULT_LEFT,
      tools: clamp(Number(parsed.tools), MIN_PANEL, MAX_TOOLS) || DEFAULT_TOOLS,
      properties: clamp(Number(parsed.properties), MIN_PANEL, MAX_PROPERTIES) || DEFAULT_PROPERTIES,
    }
  } catch {
    return { left: DEFAULT_LEFT, tools: DEFAULT_TOOLS, properties: DEFAULT_PROPERTIES }
  }
}

function clamp(n: number, min: number, max: number): number {
  if (Number.isNaN(n)) return min
  return Math.max(min, Math.min(max, n))
}

function persistPanelWidths(widths: { left: number; tools: number; properties: number }) {
  if (typeof window === 'undefined') return
  try {
    window.localStorage.setItem(PANEL_WIDTHS_KEY, JSON.stringify(widths))
  } catch {
    // ignore
  }
}

function applyTheme(theme: Theme) {
  if (typeof document === 'undefined') return
  document.documentElement.setAttribute('data-theme', theme)
}

export interface UIState {
  // Panels
  leftPanelExpanded: boolean
  toolsPanelExpanded: boolean
  propertiesPanelExpanded: boolean
  toggleLeftPanel: () => void
  toggleToolsPanel: () => void
  togglePropertiesPanel: () => void
  setPropertiesPanelExpanded: (v: boolean) => void
  /** Called from App URL sync only; panel state is driven by URL (menu/tools/props query). */
  setPanelStateFromUrl: (menuExpanded: boolean, toolsExpanded: boolean, propsExpanded: boolean) => void

  /** Resizable panel widths (px); persisted to localStorage. */
  panelWidthLeft: number
  panelWidthTools: number
  panelWidthProperties: number
  setPanelWidthLeft: (w: number) => void
  setPanelWidthTools: (w: number) => void
  setPanelWidthProperties: (w: number) => void

  // Navigation
  sectionId: SectionId | null
  subId: string
  runId: string
  tenantId: string
  setRunId: (id: string) => void
  setTenantId: (id: string) => void
  setSectionSub: (sectionId: SectionId, subId: string) => void

  // Theme
  theme: Theme
  setTheme: (theme: Theme) => void
  toggleTheme: () => void

  /** True when a new workflow run event arrived while the Events (properties) panel was closed. Cleared when the panel opens. */
  runEventsBellUnread: boolean
  setRunEventsBellUnread: (v: boolean) => void
}

export const useUIStore = create<UIState>((set, get) => ({
  leftPanelExpanded: true,
  toolsPanelExpanded: false,
  propertiesPanelExpanded: false,
  panelWidthLeft: getStoredPanelWidths().left,
  panelWidthTools: getStoredPanelWidths().tools,
  panelWidthProperties: getStoredPanelWidths().properties,
  sectionId: null,
  subId: '',
  runId: '',
  tenantId: '',
  theme: getInitialTheme(),
  runEventsBellUnread: false,

  toggleLeftPanel: () => set((s) => ({ leftPanelExpanded: !s.leftPanelExpanded })),
  toggleToolsPanel: () => set((s) => ({ toolsPanelExpanded: !s.toolsPanelExpanded })),
  togglePropertiesPanel: () => set((s) => ({ propertiesPanelExpanded: !s.propertiesPanelExpanded })),
  setPropertiesPanelExpanded: (v) => set({ propertiesPanelExpanded: v }),
  setPanelStateFromUrl: (menuExpanded, toolsExpanded, propsExpanded) =>
    set({
      leftPanelExpanded: menuExpanded,
      toolsPanelExpanded: toolsExpanded,
      propertiesPanelExpanded: propsExpanded,
    }),

  setPanelWidthLeft: (w) => {
    const next = clamp(w, MIN_PANEL, MAX_LEFT)
    set({ panelWidthLeft: next })
    const g = get()
    persistPanelWidths({ left: next, tools: g.panelWidthTools, properties: g.panelWidthProperties })
  },
  setPanelWidthTools: (w) => {
    const next = clamp(w, MIN_PANEL, MAX_TOOLS)
    set({ panelWidthTools: next })
    const g = get()
    persistPanelWidths({ left: g.panelWidthLeft, tools: next, properties: g.panelWidthProperties })
  },
  setPanelWidthProperties: (w) => {
    const next = clamp(w, MIN_PANEL, MAX_PROPERTIES)
    set({ panelWidthProperties: next })
    const g = get()
    persistPanelWidths({ left: g.panelWidthLeft, tools: g.panelWidthTools, properties: next })
  },

  setRunId: (id) => set({ runId: id }),
  setTenantId: (id) => set({ tenantId: id }),

  setSectionSub: (sectionId, subId) => set({ sectionId, subId }),

  setTheme: (theme) => {
    if (typeof window !== 'undefined') window.localStorage.setItem(THEME_KEY, theme)
    applyTheme(theme)
    set({ theme })
  },
  toggleTheme: () => {
    const next = get().theme === 'dark' ? 'light' : 'dark'
    get().setTheme(next)
  },

  setRunEventsBellUnread: (v) => set({ runEventsBellUnread: v }),
}))

// Apply theme on load
if (typeof document !== 'undefined') {
  applyTheme(getInitialTheme())
}
