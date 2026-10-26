/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { describe, it, expect } from 'vitest'
import {
  parsePath,
  buildPath,
  buildPathWithTenant,
  buildPathWithQuery,
  parseQuery,
  parsedToPanelParams,
  getDefaultSubId,
  getRunLevelDefaultSubId,
  DEFAULT_PATH,
  VALID_SECTION_IDS,
} from './routes'

describe('routes', () => {
  describe('parsePath', () => {
    it('returns null for empty path', () => {
      expect(parsePath('')).toBeNull()
      expect(parsePath('/')).toBeNull()
    })

    it('parses section-only path and uses default subId for workflows', () => {
      const r = parsePath('/workflows')
      expect(r).toEqual({ sectionId: 'workflows', subId: 'builder', runId: null })
    })

    it('parses section + subId', () => {
      expect(parsePath('/workflows/builder')).toEqual({
        sectionId: 'workflows',
        subId: 'builder',
        runId: null,
      })
      expect(parsePath('/workflows/agents')).toEqual({
        sectionId: 'workflows',
        subId: 'agents',
        runId: null,
      })
      expect(parsePath('/workflows/import-export')).toEqual({
        sectionId: 'workflows',
        subId: 'agents',
        runId: null,
      })
      expect(parsePath('/administration/tenants')).toEqual({
        sectionId: 'administration',
        subId: 'tenants',
        runId: null,
      })
    })

    it('parses section without sub-options', () => {
      expect(parsePath('/overview')).toEqual({
        sectionId: 'overview',
        subId: '',
        runId: null,
      })
      expect(parsePath('/executions')).toEqual({
        sectionId: 'executions',
        subId: '',
        runId: null,
      })
    })

    it('returns null for invalid section', () => {
      expect(parsePath('/invalid/builder')).toBeNull()
      expect(parsePath('/studio/canvas')).toBeNull()
    })

    it('falls back to default sub-option when sub is invalid', () => {
      const r = parsePath('/workflows/unknown-sub')
      expect(r).not.toBeNull()
      expect(r!.sectionId).toBe('workflows')
      expect(r!.subId).toBe('builder')
      expect(r!.runId).toBeNull()
    })

    it('handles leading/trailing slashes', () => {
      expect(parsePath('workflows/builder')).toEqual({
        sectionId: 'workflows',
        subId: 'builder',
        runId: null,
      })
    })
  })

  describe('buildPath', () => {
    it('builds section + sub path', () => {
      expect(buildPath('workflows', 'builder')).toBe('/workflows/builder')
      expect(buildPath('administration', 'tenants')).toBe('/administration/tenants')
    })

    it('builds section-only path when no sub-options', () => {
      expect(buildPath('overview', '')).toBe('/overview')
      expect(buildPath('executions', '')).toBe('/executions')
    })
  })

  describe('buildPathWithTenant', () => {
    it('returns pathname when tenantId is empty', () => {
      expect(buildPathWithTenant('/workflows/builder', '')).toBe('/workflows/builder')
    })

    it('appends tenant query when tenantId is set', () => {
      expect(buildPathWithTenant('/workflows/builder', 'tenant-1')).toBe(
        '/workflows/builder?tenant=tenant-1',
      )
    })
  })

  describe('parseQuery', () => {
    it('returns defaults when search is empty', () => {
      expect(parseQuery('')).toEqual({
        tenantId: '',
        menuExpanded: true,
        toolsExpanded: false,
        propsExpanded: false,
      })
    })

    it('parses tools=1 and props=1 as expanded', () => {
      const q = parseQuery('?tools=1&props=1')
      expect(q.toolsExpanded).toBe(true)
      expect(q.propsExpanded).toBe(true)
    })
  })

  describe('buildQuery and buildPathWithQuery', () => {
    it('buildPathWithQuery appends query to pathname', () => {
      const url = buildPathWithQuery('/workflows/builder', {
        tenantId: 't1',
        menu: 0,
        tools: 1,
        props: 0,
      })
      expect(url).toContain('/workflows/builder?')
      expect(url).toContain('tenant=t1')
      expect(url).toContain('tools=1')
    })
  })

  describe('parsedToPanelParams', () => {
    it('converts ParsedQuery to buildQuery params', () => {
      const q = parseQuery('?tenant=abc&menu=0&tools=1&props=0')
      const params = parsedToPanelParams(q)
      expect(params.tenantId).toBe('abc')
      expect(params.tools).toBe(1)
    })
  })

  describe('getDefaultSubId', () => {
    it('returns first subOption for section with options', () => {
      expect(getDefaultSubId('workflows')).toBe('builder')
      expect(getDefaultSubId('administration')).toBe('tenants')
    })

    it('returns empty string for section with no subOptions', () => {
      expect(getDefaultSubId('overview')).toBe('')
    })
  })

  describe('getRunLevelDefaultSubId', () => {
    it('returns overview when no runSelectedOptions', () => {
      expect(getRunLevelDefaultSubId('workflows')).toBe('overview')
    })
  })

  describe('constants', () => {
    it('DEFAULT_PATH is workflows/builder', () => {
      expect(DEFAULT_PATH).toBe('/workflows/builder')
    })

    it('VALID_SECTION_IDS includes all v1 sections', () => {
      expect(VALID_SECTION_IDS).toContain('overview')
      expect(VALID_SECTION_IDS).toContain('workflows')
      expect(VALID_SECTION_IDS).toContain('executions')
      expect(VALID_SECTION_IDS).toContain('observability')
      expect(VALID_SECTION_IDS).toContain('extensions')
      expect(VALID_SECTION_IDS).toContain('administration')
    })
  })
})
