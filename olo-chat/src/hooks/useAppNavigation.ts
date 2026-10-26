/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { useEffect } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import {
  parsePath,
  buildPath,
  buildPathWithQuery,
  buildQuery,
  parseQuery,
  parsedToPanelParams,
  DEFAULT_PATH,
  getRunLevelDefaultSubId,
} from '../routes'
import { getLastSelectedPath, setLastSelectedPath } from '../lib/lastSelectedPath'
import type { SectionId } from '../types/layout'
import { isFeatureEnabled } from '../config/features'
import type { FeatureId } from '../config/features'
import { useUIStore } from '../store/ui'

export function useAppNavigation() {
  const location = useLocation()
  const navigate = useNavigate()
  const { setRunId, setSectionSub } = useUIStore()

  useEffect(() => {
    const pathname = location.pathname || '/'
    if (pathname === '/' || pathname === '') {
      const last = getLastSelectedPath()
      const initialPath = last != null && last !== '' && parsePath(last) ? last : DEFAULT_PATH
      navigate(initialPath, { replace: true })
      return
    }
    const parsed = parsePath(pathname)
    if (!parsed) {
      const last = getLastSelectedPath()
      const fallback = last != null && last !== '' && parsePath(last) ? last : DEFAULT_PATH
      navigate(fallback, { replace: true })
      return
    }
    setLastSelectedPath(pathname)
    if (!isFeatureEnabled(parsed.sectionId as FeatureId)) {
      navigate(DEFAULT_PATH, { replace: true })
      return
    }
    setSectionSub(parsed.sectionId, parsed.subId)
    setRunId(parsed.runId ?? '')
    const q = parseQuery(location.search)
    useUIStore.getState().setPanelStateFromUrl(q.menuExpanded, q.toolsExpanded, q.propsExpanded)
  }, [location.pathname, location.search, location.key, navigate, setSectionSub, setRunId])

  const q = parseQuery(location.search)

  const updatePanelQuery = (updates: { menu?: 0 | 1; tools?: 0 | 1; props?: 0 | 1 }) => {
    const params = parsedToPanelParams(q)
    const next = { ...params, ...updates }
    navigate(location.pathname + '?' + buildQuery(next), { replace: true })
  }

  const handleSectionSubSelect = (sid: SectionId, sub: string) => {
    const params = parsedToPanelParams(q)
    navigate(buildPathWithQuery(buildPath(sid, sub), { ...params, props: 0 }))
  }

  const handleRunIdChange = (id: string) => {
    const sectionId = useUIStore.getState().sectionId
    const subId = useUIStore.getState().subId
    if (!sectionId) return
    const params = parsedToPanelParams(q)
    const sub = id ? getRunLevelDefaultSubId(sectionId) : subId
    navigate(buildPathWithQuery(buildPath(sectionId, sub), params))
  }

  const handleToggleLeftPanel = () => updatePanelQuery({ menu: q.menuExpanded ? 0 : 1 })
  const handleToggleToolsPanel = () => updatePanelQuery({ tools: q.toolsExpanded ? 0 : 1 })
  const handleTogglePropertiesPanel = () => updatePanelQuery({ props: q.propsExpanded ? 0 : 1 })
  const openPropertiesPanel = () => updatePanelQuery({ props: 1 })

  return {
    handleSectionSubSelect,
    handleRunIdChange,
    handleToggleLeftPanel,
    handleToggleToolsPanel,
    handleTogglePropertiesPanel,
    openPropertiesPanel,
  }
}
