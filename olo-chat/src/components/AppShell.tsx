/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { useEffect } from 'react'
import type { CSSProperties } from 'react'
import { TopBar } from './TopBar'
import { LeftPanel } from './LeftPanel'
import { ToolsPanel } from './ToolsPanel'
import { MainContent } from './MainContent'
import { PropertiesPanel } from './PropertiesPanel'
import { PanelResizeHandle } from './PanelResizeHandle'
import { TenantConfigForm } from './TenantConfigForm'
import { EventsList } from './EventsList'
import { FileViewerPanel } from './FileViewerPanel'
import type { Tenant } from '../types/tenant'
import { useUIStore } from '../store/ui'
import { runEventsStore } from '../store/runEvents'
import { tenantConfigStore } from '../store/tenantConfig'
import { logEvent } from '../lib/observability'
import { OLO_CHAT_VERSION } from '../version'
import type { useAppNavigation } from '../hooks/useAppNavigation'

export interface AppShellProps {
  newChatTrigger: number
  setNewChatTrigger: React.Dispatch<React.SetStateAction<number>>
  uiFooter: { tenant: string; user: string; oloVersion: string }
  chatProfiles: import('../api/chatApi').ChatProfileDto[]
  backendReachable: boolean
  nav: ReturnType<typeof useAppNavigation>
}

export function AppShell({
  newChatTrigger,
  setNewChatTrigger,
  uiFooter,
  chatProfiles,
  backendReachable,
  nav,
}: AppShellProps) {
  const {
    leftPanelExpanded,
    toolsPanelExpanded,
    propertiesPanelExpanded,
    panelWidthLeft,
    panelWidthTools,
    panelWidthProperties,
    sectionId,
    subId,
    runId,
    tenantId,
  } = useUIStore()

  const runSelected = false
  const isTenantConfig = false

  const tenants = tenantConfigStore((s) => s.tenants)
  const tenantsLoading = tenantConfigStore((s) => s.tenantsLoading)
  const configSelectedTenant = tenantConfigStore((s) => s.configSelectedTenant)
  const configIsAddingNew = tenantConfigStore((s) => s.configIsAddingNew)

  const handleSelectTenant = (t: Tenant) => {
    tenantConfigStore.getState().selectTenant(t)
    nav.openPropertiesPanel()
  }

  const handleAddNewTenant = () => {
    tenantConfigStore.getState().startAddNew()
    nav.openPropertiesPanel()
  }

  return (
    <div className="app">
      <TopBar />
      <div
        className="app-body"
        style={
          {
            '--panel-width-left': `${panelWidthLeft}px`,
            '--panel-width-tools': `${panelWidthTools}px`,
            '--panel-width-properties': `${panelWidthProperties}px`,
          } as CSSProperties
        }
      >
        <LeftPanel
          expanded={leftPanelExpanded}
          onToggle={nav.handleToggleLeftPanel}
          userLabel={`${uiFooter.user} User`}
          tenantLabel={uiFooter.tenant}
          oloVersion={uiFooter.oloVersion}
          chatVersion={OLO_CHAT_VERSION}
          sectionId={sectionId}
          subId={subId}
          runSelected={runSelected}
          onSectionSubSelect={nav.handleSectionSubSelect}
        />
        <PanelResizeHandle
          panel="left"
          visible
          onResize={(delta) => useUIStore.getState().setPanelWidthLeft(useUIStore.getState().panelWidthLeft + delta)}
        />
        {!isTenantConfig && (sectionId === 'chat' || sectionId === 'knowledge') && (
          <>
            <ToolsPanel
              expanded={toolsPanelExpanded}
              onToggle={nav.handleToggleToolsPanel}
              sectionId={sectionId}
              subId={subId}
              runSelected={runSelected}
              tenantId={tenantId}
              storeContext={{}}
              onNewChat={() => setNewChatTrigger((t) => t + 1)}
              chatProfiles={chatProfiles}
            />
            <PanelResizeHandle
              panel="tools"
              visible
              onResize={(delta) => useUIStore.getState().setPanelWidthTools(useUIStore.getState().panelWidthTools + delta)}
            />
          </>
        )}
        <MainContent
          sectionId={sectionId}
          subId={subId}
          runSelected={runSelected}
          runId={runId}
          onRunIdChange={nav.handleRunIdChange}
          tenantId={tenantId}
          tenants={tenants}
          tenantsLoading={tenantsLoading}
          configSelectedTenant={configSelectedTenant}
          onSelectTenant={handleSelectTenant}
          onAddNewTenant={handleAddNewTenant}
          onDeleteTenant={(id) => tenantConfigStore.getState().deleteTenant(id)}
          newChatTrigger={newChatTrigger}
          chatProfiles={chatProfiles}
          backendReachable={backendReachable}
        />
        <PanelResizeHandle
          panel="properties"
          visible
          onResize={(delta) =>
            useUIStore.getState().setPanelWidthProperties(useUIStore.getState().panelWidthProperties + delta)
          }
        />
        <PropertiesPanel
          expanded={propertiesPanelExpanded}
          onToggle={nav.handleTogglePropertiesPanel}
          title={sectionId === 'documents' ? 'File Viewer' : 'Events'}
        >
          {sectionId === 'chat' ? (
            <EventsList />
          ) : sectionId === 'documents' ? (
            <FileViewerPanel />
          ) : sectionId === 'knowledge' ? undefined : isTenantConfig ? (
            <TenantConfigForm
              tenant={configSelectedTenant}
              isAddingNew={configIsAddingNew}
              onSave={(tenant) => tenantConfigStore.getState().saveTenant(tenant)}
            />
          ) : undefined}
        </PropertiesPanel>
      </div>
    </div>
  )
}

export function useAppEffects(sectionId: ReturnType<typeof useUIStore.getState>['sectionId'], subId: string, runId: string) {
  const { propertiesPanelExpanded, setRunEventsBellUnread } = useUIStore()

  useEffect(() => {
    runEventsStore.getState().setOnWorkflowEventAppended(() => {
      if (!useUIStore.getState().propertiesPanelExpanded) {
        useUIStore.getState().setRunEventsBellUnread(true)
      }
    })
    return () => runEventsStore.getState().setOnWorkflowEventAppended(null)
  }, [])

  useEffect(() => {
    if (propertiesPanelExpanded) {
      setRunEventsBellUnread(false)
    }
  }, [propertiesPanelExpanded, setRunEventsBellUnread])

  useEffect(() => {
    tenantConfigStore.getState().loadTenants()
  }, [])

  useEffect(() => {
    if (sectionId != null) {
      logEvent('navigation', { section: sectionId, sub: subId, runId: runId || undefined })
    }
  }, [sectionId, subId, runId])
}
