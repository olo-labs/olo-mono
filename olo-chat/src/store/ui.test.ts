/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { describe, it, expect, beforeEach } from 'vitest'
import { useUIStore } from './ui'

describe('UI store', () => {
  beforeEach(() => {
    useUIStore.setState({
      sectionId: null,
      subId: '',
      propertiesPanelExpanded: false,
      runEventsBellUnread: false,
    })
  })

  describe('setSectionSub', () => {
    it('updates sectionId and subId', () => {
      useUIStore.getState().setSectionSub('chat', 'conversation')
      expect(useUIStore.getState().sectionId).toBe('chat')
      expect(useUIStore.getState().subId).toBe('conversation')
    })

    it('does not change panel state (panel state is URL-driven)', () => {
      useUIStore.setState({ propertiesPanelExpanded: true })
      useUIStore.getState().setSectionSub('chat', 'conversation')
      expect(useUIStore.getState().propertiesPanelExpanded).toBe(true)
    })
  })

  describe('setPanelStateFromUrl', () => {
    it('updates panel expanded state from URL sync', () => {
      useUIStore.getState().setPanelStateFromUrl(false, true, true)
      expect(useUIStore.getState().leftPanelExpanded).toBe(false)
      expect(useUIStore.getState().toolsPanelExpanded).toBe(true)
      expect(useUIStore.getState().propertiesPanelExpanded).toBe(true)
    })
  })

  describe('navigation state', () => {
    it('setRunId and setTenantId update store', () => {
      useUIStore.getState().setRunId('run-123')
      useUIStore.getState().setTenantId('tenant-abc')
      expect(useUIStore.getState().runId).toBe('run-123')
      expect(useUIStore.getState().tenantId).toBe('tenant-abc')
    })
  })
})
