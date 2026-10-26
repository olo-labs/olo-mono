/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { SectionId } from './types/layout'
import { SECTIONS, getSectionDefaultSubId, resolveSubId } from './types/layout'

export const VALID_SECTION_IDS: SectionId[] = [
  'overview',
  'workflows',
  'executions',
  'observability',
  'extensions',
  'administration',
]

function isSectionId(s: string): s is SectionId {
  return VALID_SECTION_IDS.includes(s as SectionId)
}

function getSection(sectionId: SectionId) {
  return SECTIONS.find((s) => s.id === sectionId)
}

/** Default subId when only section is in path (e.g. /workflows → builder). */
export function getDefaultSubId(sectionId: SectionId): string {
  return getSectionDefaultSubId(sectionId)
}

function supportsRunLevelRoutes(sectionId: SectionId): boolean {
  const section = getSection(sectionId)
  return Boolean(section?.runSelectedOptions?.length)
}

/** Return list-view subId when run path is missing or invalid. */
function getListSubId(sectionId: SectionId): string {
  return getDefaultSubId(sectionId)
}

function isValidSubId(sectionId: SectionId, subId: string, forRunLevel: boolean): boolean {
  const section = getSection(sectionId)
  if (!section) return false
  const options = forRunLevel ? (section.runSelectedOptions ?? []) : (section.subOptions ?? [])
  if (options.length === 0) return subId === ''
  return options.some((o) => o.id === subId)
}

export interface ParsedPath {
  sectionId: SectionId
  subId: string
  runId: string | null
}

/**
 * Parse pathname to navigation state.
 * - Sections without sub-options use empty subId (e.g. /overview).
 * - Invalid sub-option falls back to default.
 * - Run paths supported when section defines runSelectedOptions (future Executions).
 */
export function parsePath(pathname: string): ParsedPath | null {
  const segments = pathname.replace(/^\/+|\/+$/g, '').split('/').filter(Boolean)
  if (segments.length === 0) return null

  const sectionId = segments[0]
  if (!isSectionId(sectionId)) return null

  if (segments[1] === 'run' && supportsRunLevelRoutes(sectionId)) {
    const runIdRaw = segments[2]
    const subIdRaw = segments[3]
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

  const subIdRaw = segments[1] ?? getDefaultSubId(sectionId)
  const defaultSub = getDefaultSubId(sectionId)
  const normalizedSubId = resolveSubId(sectionId, subIdRaw)
  const subId = isValidSubId(sectionId, normalizedSubId, false) ? normalizedSubId : defaultSub
  return { sectionId, subId, runId: null }
}

/**
 * Build path for navigation. Use for deep links and browser history.
 */
export function buildPath(
  sectionId: SectionId,
  subId: string,
  runId: string | null = null,
): string {
  if (runId && supportsRunLevelRoutes(sectionId)) {
    return `/${sectionId}/run/${encodeURIComponent(runId)}/${encodeURIComponent(subId)}`
  }
  const section = getSection(sectionId)
  const hasSubs = (section?.subOptions?.length ?? 0) > 0
  if (!hasSubs || !subId) {
    return `/${sectionId}`
  }
  return `/${sectionId}/${encodeURIComponent(subId)}`
}

/** Default run-level subId when opening a run (future Executions / Observability). */
export function getRunLevelDefaultSubId(sectionId: SectionId): string {
  const section = getSection(sectionId)
  const options = section?.runSelectedOptions ?? []
  return options[0]?.id ?? 'overview'
}

/** Root path for app; redirect here when path is invalid or "/" */
export const DEFAULT_PATH = '/workflows/builder'

/** Panel state in query: menu=0 collapsed, menu=1 expanded; same for tools, props. */
export type PanelQuery = { menu?: 0 | 1; tools?: 0 | 1; props?: 0 | 1 }

export interface ParsedQuery {
  tenantId: string
  menuExpanded: boolean
  toolsExpanded: boolean
  propsExpanded: boolean
}

export function parseQuery(search: string): ParsedQuery {
  const params = new URLSearchParams(search)
  return {
    tenantId: params.get('tenant') ?? '',
    menuExpanded: params.get('menu') !== '0',
    toolsExpanded: params.get('tools') === '1',
    propsExpanded: params.get('props') === '1',
  }
}

export function buildQuery(params: {
  tenantId?: string
  menu: 0 | 1
  tools: 0 | 1
  props: 0 | 1
}): string {
  const p = new URLSearchParams()
  if (params.tenantId) p.set('tenant', params.tenantId)
  p.set('menu', String(params.menu))
  p.set('tools', String(params.tools))
  p.set('props', String(params.props))
  return p.toString()
}

export function parsedToPanelParams(q: ParsedQuery): { tenantId: string; menu: 0 | 1; tools: 0 | 1; props: 0 | 1 } {
  return {
    tenantId: q.tenantId,
    menu: q.menuExpanded ? 1 : 0,
    tools: q.toolsExpanded ? 1 : 0,
    props: q.propsExpanded ? 1 : 0,
  }
}

export function buildPathWithQuery(
  pathname: string,
  params: { tenantId?: string; menu: 0 | 1; tools: 0 | 1; props: 0 | 1 },
): string {
  const q = buildQuery(params)
  return pathname + (q ? '?' + q : '')
}

export function buildPathWithTenant(pathname: string, tenantId: string): string {
  const search = tenantId ? `?tenant=${encodeURIComponent(tenantId)}` : ''
  return pathname + search
}
