/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { describe, it, expect } from 'vitest'
import {
  parsePath,
  buildPath,
  buildPathWithQuery,
  parseQuery,
  buildQuery,
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

    it('parses section-only path and uses default subId', () => {
      const r = parsePath('/chat')
      expect(r).toEqual({ sectionId: 'chat', subId: 'conversation', runId: null })
    })

    it('parses section + subId', () => {
      expect(parsePath('/chat/conversation')).toEqual({
        sectionId: 'chat',
        subId: 'conversation',
        runId: null,
      })
      expect(parsePath('/knowledge/sources')).toEqual({
        sectionId: 'knowledge',
        subId: 'sources',
        runId: null,
      })
      expect(parsePath('/documents/upload')).toEqual({
        sectionId: 'documents',
        subId: 'upload',
        runId: null,
      })
    })

    it('returns null for invalid section', () => {
      expect(parsePath('/invalid/canvas')).toBeNull()
      expect(parsePath('/studio')).toBeNull()
    })

    it('handles leading/trailing slashes', () => {
      expect(parsePath('chat/conversation')).toEqual({
        sectionId: 'chat',
        subId: 'conversation',
        runId: null,
      })
      expect(parsePath('/knowledge/sources/')).toEqual({
        sectionId: 'knowledge',
        subId: 'sources',
        runId: null,
      })
    })

    it('falls back to default sub-option when sub is invalid', () => {
      const r = parsePath('/knowledge/unknown-sub')
      expect(r).not.toBeNull()
      expect(r!.sectionId).toBe('knowledge')
      expect(r!.subId).toBe('sources')
      expect(r!.runId).toBeNull()
    })

    it('chat with invalid sub (e.g. old queue path) falls back to conversation', () => {
      const r = parsePath('/chat/some-queue-name')
      expect(r).not.toBeNull()
      expect(r!.sectionId).toBe('chat')
      expect(r!.subId).toBe('conversation')
      expect(r!.runId).toBeNull()
    })
  })

  describe('buildPath', () => {
    it('builds section + sub path', () => {
      expect(buildPath('chat', 'conversation')).toBe('/chat/conversation')
      expect(buildPath('knowledge', 'sources')).toBe('/knowledge/sources')
      expect(buildPath('documents', 'upload')).toBe('/documents/upload')
    })

    it('ignores runId (no run-level routes)', () => {
      expect(buildPath('chat', 'conversation', null)).toBe('/chat/conversation')
      expect(buildPath('chat', 'conversation', 'any-id')).toBe('/chat/conversation')
    })
  })

  describe('parseQuery', () => {
    it('returns defaults when search is empty', () => {
      expect(parseQuery('')).toEqual({
        menuExpanded: true,
        toolsExpanded: false,
        propsExpanded: false,
      })
    })

    it('parses menu=0 as collapsed', () => {
      expect(parseQuery('?menu=0').menuExpanded).toBe(false)
    })

    it('parses tools=1 and props=1 as expanded', () => {
      const q = parseQuery('?tools=1&props=1')
      expect(q.toolsExpanded).toBe(true)
      expect(q.propsExpanded).toBe(true)
    })

  })

  describe('buildQuery and buildPathWithQuery', () => {
    it('buildQuery outputs menu, tools, props', () => {
      const q = buildQuery({ menu: 1, tools: 0, props: 0 })
      expect(q).toContain('menu=1')
      expect(q).toContain('tools=0')
      expect(q).toContain('props=0')
    })

    it('buildPathWithQuery appends query to pathname', () => {
      const url = buildPathWithQuery('/chat/conversation', {
        menu: 0,
        tools: 1,
        props: 0,
      })
      expect(url).toContain('/chat/conversation?')
      expect(url).toContain('menu=0')
      expect(url).toContain('tools=1')
    })
  })

  describe('parsedToPanelParams', () => {
    it('converts ParsedQuery to buildQuery params', () => {
      const q = parseQuery('?menu=0&tools=1&props=0')
      const params = parsedToPanelParams(q)
      expect(params.menu).toBe(0)
      expect(params.tools).toBe(1)
      expect(params.props).toBe(0)
    })
  })

  describe('getDefaultSubId', () => {
    it('returns first subOption for section', () => {
      expect(getDefaultSubId('chat')).toBe('conversation')
      expect(getDefaultSubId('knowledge')).toBe('sources')
      expect(getDefaultSubId('documents')).toBe('upload')
    })
  })

  describe('getRunLevelDefaultSubId', () => {
    it('returns overview for sections with no runSelectedOptions', () => {
      expect(getRunLevelDefaultSubId('chat')).toBe('overview')
      expect(getRunLevelDefaultSubId('knowledge')).toBe('overview')
      expect(getRunLevelDefaultSubId('documents')).toBe('overview')
    })
  })

  describe('constants', () => {
    it('DEFAULT_PATH is chat/conversation', () => {
      expect(DEFAULT_PATH).toBe('/chat/conversation')
    })

    it('VALID_SECTION_IDS includes chat, knowledge, documents', () => {
      expect(VALID_SECTION_IDS).toContain('chat')
      expect(VALID_SECTION_IDS).toContain('knowledge')
      expect(VALID_SECTION_IDS).toContain('documents')
      expect(VALID_SECTION_IDS).toHaveLength(3)
    })
  })
})
