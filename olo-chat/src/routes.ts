/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import type { SectionId } from './types/layout'
import { SECTIONS } from './types/layout'

export const VALID_SECTION_IDS: SectionId[] = ['chat', 'knowledge', 'documents']

function isSectionId(s: string): s is SectionId {
  return VALID_SECTION_IDS.includes(s as SectionId)
}

/** Default subId when only section is in path (e.g. /studio -> canvas) */
export function getDefaultSubId(sectionId: SectionId): string {
  const section = SECTIONS.find((s) => s.id === sectionId)
  const options = section?.subOptions ?? []
  return options[0]?.id ?? ''
}

function getListSubId(sectionId: SectionId): string {
  return getDefaultSubId(sectionId)
}

function isValidSubId(sectionId: SectionId, subId: string, forRunLevel: boolean): boolean {
  const section = SECTIONS.find((s) => s.id === sectionId)
  if (!section) return false
  const options = forRunLevel ? (section.runSelectedOptions ?? []) : (section.subOptions ?? [])
  return options.some((o) => o.id === subId)
}

export interface ParsedPath {
  sectionId: SectionId
  subId: string
  runId: string | null
}

/**
 * Parse pathname to navigation state.
 * - Invalid sub-option falls back to default (or run-level default).
 * - Run path with missing/empty runId falls back to list view (e.g. /runtime/live-runs).
 */
export function parsePath(pathname: string): ParsedPath | null {
  const segments = pathname.replace(/^\/+|\/+$/g, '').split('/').filter(Boolean)
  if (segments.length === 0) return null

  const sectionId = segments[0]
  if (!isSectionId(sectionId)) return null

  // /:sectionId/run/:runId/:subId — run-level route
  if (segments[1] === 'run') {
    const runIdRaw = segments[2]
    let subIdRaw = segments[3]
    try {
      if (subIdRaw != null) subIdRaw = decodeURIComponent(subIdRaw)
    } catch {
      // leave as-is
    }
    // Missing runId: /runtime/run//overview or /runtime/run/overview (only 3 segments) -> list view
    if (segments.length < 4 || !runIdRaw || runIdRaw.trim() === '') {
      return {
        sectionId,
        subId: getListSubId(sectionId),
        runId: null,
      }
    }
    const runLevelDefault = getRunLevelDefaultSubId(sectionId)
    const subId = isValidSubId(sectionId, subIdRaw ?? '', true) ? (subIdRaw ?? runLevelDefault) : runLevelDefault
    return { sectionId, subId, runId: runIdRaw }
  }

  // /:sectionId/:subId? — decode so subId is stored without %3A (e.g. queue name with :1.0)
  let subIdRaw = segments[1] ?? getDefaultSubId(sectionId)
  try {
    subIdRaw = decodeURIComponent(subIdRaw)
  } catch {
    // leave as-is if not valid encoding
  }
  const defaultSub = getDefaultSubId(sectionId)
  const subId = isValidSubId(sectionId, subIdRaw, false) ? subIdRaw : defaultSub
  return { sectionId, subId, runId: null }
}

/**
 * Build path for navigation. Use for deep links and browser history.
 * For run-level views (runtime/ledger with runId), use runId; otherwise pass null.
 */
export function buildPath(
  sectionId: SectionId,
  subId: string,
  _runId: string | null = null
): string {
  return `/${sectionId}/${encodeURIComponent(subId)}`
}

/** Default run-level subId when opening a run (e.g. Overview for runtime, Summary for ledger). */
export function getRunLevelDefaultSubId(sectionId: SectionId): string {
  const section = SECTIONS.find((s) => s.id === sectionId)
  const options = section?.runSelectedOptions ?? []
  return options[0]?.id ?? 'overview'
}

/** Default path when no previous selection (Chat). */
export const DEFAULT_PATH = '/chat/conversation'

/** Panel state in query: menu=0 collapsed, menu=1 expanded; same for tools, props. */
export type PanelQuery = { menu?: 0 | 1; tools?: 0 | 1; props?: 0 | 1 }

export interface ParsedQuery {
  menuExpanded: boolean
  toolsExpanded: boolean
  propsExpanded: boolean
}

/**
 * Parse search string to panel state.
 * Defaults: menu expanded (true), tools and props collapsed (false).
 */
export function parseQuery(search: string): ParsedQuery {
  const params = new URLSearchParams(search)
  return {
    menuExpanded: params.get('menu') !== '0',
    toolsExpanded: params.get('tools') === '1',
    propsExpanded: params.get('props') === '1',
  }
}

/** Build query string for panel state (menu, tools, props). */
export function buildQuery(params: {
  menu: 0 | 1
  tools: 0 | 1
  props: 0 | 1
}): string {
  const p = new URLSearchParams()
  p.set('menu', String(params.menu))
  p.set('tools', String(params.tools))
  p.set('props', String(params.props))
  return p.toString()
}

/** Current panel state from ParsedQuery for buildQuery. */
export function parsedToPanelParams(q: ParsedQuery): { menu: 0 | 1; tools: 0 | 1; props: 0 | 1 } {
  return {
    menu: q.menuExpanded ? 1 : 0,
    tools: q.toolsExpanded ? 1 : 0,
    props: q.propsExpanded ? 1 : 0,
  }
}

/** Build pathname + query for navigation. Preserves panel state in URL. */
export function buildPathWithQuery(
  pathname: string,
  params: { menu: 0 | 1; tools: 0 | 1; props: 0 | 1 }
): string {
  const q = buildQuery(params)
  return pathname + (q ? '?' + q : '')
}
