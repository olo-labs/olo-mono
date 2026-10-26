/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { getHealth } from '../api/rest'
import { useUIStore } from '../store/ui'
import { tenantConfigStore } from '../store/tenantConfig'
import { workflowConfigurationStore } from '../store/workflowConfigurationStore'
import { configurationFolderStore } from '../store/configurationFolderStore'
import { graphLogStore } from '../store/graphLogStore'
import { catalogStore } from '../store/catalogStore'
import { parsePath, parseQuery, DEFAULT_PATH, buildQuery, parsedToPanelParams } from '../routes'
import { isFeatureEnabled } from '../config/features'
import type { FeatureId } from '../config/features'
import { logEvent } from '../lib/observability'
import { getLastTenantId } from '../lib/lastTenant'
import type { SectionId } from '../types/layout'

const BACKEND_POLL_INTERVAL_MS = 2000

export function useAppBackendReady() {
  const [backendReady, setBackendReady] = useState(false)
  useEffect(() => {
    let cancelled = false
    const check = () => {
      getHealth()
        .then(() => { if (!cancelled) setBackendReady(true) })
        .catch(() => {})
    }
    check()
    const id = setInterval(check, BACKEND_POLL_INTERVAL_MS)
    return () => { cancelled = true; clearInterval(id) }
  }, [])
  return backendReady
}

export function useAppRouteSync() {
  const location = useLocation()
  const navigate = useNavigate()
  const { setRunId, setTenantId, setSectionSub } = useUIStore()

  useEffect(() => {
    const pathname = location.pathname || '/'
    if (pathname === '/' || pathname === '') {
      navigate(DEFAULT_PATH, { replace: true })
      return
    }
    const parsed = parsePath(pathname)
    if (!parsed) {
      navigate(DEFAULT_PATH, { replace: true })
      return
    }
    if (!isFeatureEnabled(parsed.sectionId as FeatureId)) {
      navigate(DEFAULT_PATH, { replace: true })
      return
    }
    setSectionSub(parsed.sectionId, parsed.subId)
    setRunId(parsed.runId ?? '')
    const q = parseQuery(location.search)
    setTenantId(q.tenantId)
    useUIStore.getState().setPanelStateFromUrl(q.menuExpanded, q.toolsExpanded, q.propsExpanded)
    if (parsed.sectionId !== 'administration' || parsed.subId !== 'tenants') {
      tenantConfigStore.getState().clearSelection()
    }
    if (parsed.sectionId !== 'workflows') {
      workflowConfigurationStore.getState().clearSelection()
      graphLogStore.getState().clearSelection()
    } else if (parsed.subId === 'log') {
      workflowConfigurationStore.getState().clearSelection()
    } else {
      graphLogStore.getState().clearSelection()
    }
  }, [location.pathname, location.search, location.key, navigate, setSectionSub, setRunId, setTenantId])
}

export function useAppDataLoading(sectionId: SectionId | null, subId: string) {
  const isTenantAdmin = sectionId === 'administration' && subId === 'tenants'
  const isScenarioAdmin = sectionId === 'administration' && subId === 'scenarios'
  const isWorkflowLog = sectionId === 'workflows' && subId === 'log'
  const needsCatalog =
    sectionId === 'workflows' && (subId === 'builder' || subId === 'agents' || subId === 'log')
  const needsWorkflows = sectionId === 'workflows'

  useEffect(() => {
    tenantConfigStore.getState().loadTenants()
  }, [])

  useEffect(() => {
    if (isTenantAdmin) {
      tenantConfigStore.getState().loadTenants()
    }
  }, [isTenantAdmin])

  useEffect(() => {
    if (isScenarioAdmin) {
      configurationFolderStore.getState().loadFolders()
    }
  }, [isScenarioAdmin])

  useEffect(() => {
    if (needsCatalog && !catalogStore.getState().catalog && !catalogStore.getState().loading) {
      catalogStore.getState().loadCatalog()
    }
  }, [needsCatalog])

  useEffect(() => {
    if (needsWorkflows) {
      workflowConfigurationStore.getState().loadWorkflows()
    }
    if (isWorkflowLog) {
      graphLogStore.getState().loadLogs()
    }
    if (needsWorkflows && !catalogStore.getState().catalog && !catalogStore.getState().loading) {
      catalogStore.getState().loadCatalog()
    }
  }, [needsWorkflows, isWorkflowLog])

  return { isTenantAdmin, isWorkflowLog, isScenarioAdmin }
}

export function useAppNavigationLog(sectionId: SectionId | null, subId: string, runId: string) {
  useEffect(() => {
    if (sectionId != null) {
      logEvent('navigation', { section: sectionId, sub: subId, runId: runId || undefined })
    }
  }, [sectionId, subId, runId])
}

export function useDefaultTenantRedirect() {
  const location = useLocation()
  const navigate = useNavigate()
  const tenants = tenantConfigStore((s) => s.tenants)

  useEffect(() => {
    const q = parseQuery(location.search)
    if (q.tenantId !== '') return
    if (tenants.length === 0) return
    const last = getLastTenantId()
    const defaultId = tenants.some((t) => t.id === last) ? last : tenants[0].id
    const params = parsedToPanelParams(q)
    navigate(location.pathname + '?' + buildQuery({ ...params, tenantId: defaultId }), {
      replace: true,
    })
  }, [location.pathname, location.search, tenants, navigate])
}
