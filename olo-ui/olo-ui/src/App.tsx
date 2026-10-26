/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { CSSProperties } from 'react'
import { TopBar } from './components/TopBar'
import { LeftPanel } from './components/LeftPanel'
import { BuilderSidePanel } from './components/builder/BuilderSidePanel'
import { MainContent } from './components/MainContent'
import { PropertiesPanel } from './components/PropertiesPanel'
import { PanelResizeHandle } from './components/PanelResizeHandle'
import { AppBackendLoading } from './components/AppBackendLoading'
import { AppPropertiesContent } from './components/AppPropertiesContent'
import { useUIStore } from './store/ui'
import { tenantConfigStore } from './store/tenantConfig'
import { workflowConfigurationStore } from './store/workflowConfigurationStore'
import { catalogStore } from './store/catalogStore'
import { presetParametersForWorkflow } from './lib/catalogLookup'
import {
  useAppBackendReady,
  useAppDataLoading,
  useAppNavigationLog,
  useAppRouteSync,
  useDefaultTenantRedirect,
} from './hooks/useAppEffects'
import { useAppPanelNavigation, useAppTenantActions, useAppWorkflowActions } from './hooks/useAppActions'

function App() {
  const backendReady = useAppBackendReady()
  useAppRouteSync()

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
  } = useUIStore()

  const isWorkflowAgents = sectionId === 'workflows' && subId === 'agents'
  const isWorkflowBuilder = sectionId === 'workflows' && subId === 'builder'
  const showComponentsPanel = isWorkflowBuilder
  const { isTenantAdmin } = useAppDataLoading(sectionId, subId)
  useAppNavigationLog(sectionId, subId, runId)
  useDefaultTenantRedirect()

  const { q, updatePanelQuery, handleSectionSubSelect } = useAppPanelNavigation()
  const { handleSelectTenant, handleAddNewTenant } = useAppTenantActions(updatePanelQuery)
  const { handleSelectWorkflow, handleImportWorkflowFile, agentsFileActions } =
    useAppWorkflowActions(updatePanelQuery, handleSectionSubSelect)

  const handleToggleLeftPanel = () => updatePanelQuery({ menu: q.menuExpanded ? 0 : 1 })
  const handleToggleComponentsPanel = () => updatePanelQuery({ tools: q.toolsExpanded ? 0 : 1 })
  const handleTogglePropertiesPanel = () => updatePanelQuery({ props: q.propsExpanded ? 0 : 1 })

  const tenants = tenantConfigStore((s) => s.tenants)
  const tenantsLoading = tenantConfigStore((s) => s.tenantsLoading)
  const configSelectedTenant = tenantConfigStore((s) => s.configSelectedTenant)
  const configIsAddingNew = tenantConfigStore((s) => s.configIsAddingNew)

  const workflows = workflowConfigurationStore((s) => s.workflows)
  const workflowsLoading = workflowConfigurationStore((s) => s.loading)
  const workflowsError = workflowConfigurationStore((s) => s.error)
  const configurationRoot = workflowConfigurationStore((s) => s.configurationRoot)
  const selectedWorkflowFile = workflowConfigurationStore((s) => s.selectedFileName)
  const workflowDraft = workflowConfigurationStore((s) => s.draft)
  const workflowDirty = workflowConfigurationStore((s) => s.dirty)
  const selectedCanvasNodeId = workflowConfigurationStore((s) => s.selectedCanvasNodeId)
  const catalog = catalogStore((s) => s.catalog)
  const workflowCatalogParameters = workflowDraft
    ? presetParametersForWorkflow(catalog, workflowDraft)
    : []

  if (!backendReady) {
    return <AppBackendLoading />
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
          onToggle={handleToggleLeftPanel}
          sectionId={sectionId}
          subId={subId}
          onSectionSubSelect={handleSectionSubSelect}
        />
        <PanelResizeHandle
          panel="left"
          visible={leftPanelExpanded}
          onResize={(delta) => useUIStore.getState().setPanelWidthLeft(useUIStore.getState().panelWidthLeft + delta)}
        />
        {showComponentsPanel && (
          <>
            <BuilderSidePanel
              expanded={toolsPanelExpanded}
              onToggle={handleToggleComponentsPanel}
            />
            <PanelResizeHandle
              panel="components"
              visible={toolsPanelExpanded}
              onResize={(delta) => useUIStore.getState().setPanelWidthTools(useUIStore.getState().panelWidthTools + delta)}
            />
          </>
        )}
        <MainContent
          sectionId={sectionId}
          subId={subId}
          tenants={tenants}
          tenantsLoading={tenantsLoading}
          configSelectedTenant={configSelectedTenant}
          onSelectTenant={handleSelectTenant}
          onAddNewTenant={handleAddNewTenant}
          onDeleteTenant={(id) => tenantConfigStore.getState().deleteTenant(id)}
          workflows={workflows}
          workflowsLoading={workflowsLoading}
          workflowsError={workflowsError}
          configurationRoot={configurationRoot}
          selectedWorkflowFile={selectedWorkflowFile}
          onSelectWorkflow={handleSelectWorkflow}
          onImportWorkflowFile={handleImportWorkflowFile}
          onReloadWorkflows={() => workflowConfigurationStore.getState().reloadFromDisk()}
          agentsFileActions={agentsFileActions}
          agentsCanvasActions={{
            onReload: () => workflowConfigurationStore.getState().reloadFromDisk(),
            onImport: () => {},
          }}
          workflowExportDisabled={!workflowDraft}
        />
        <PanelResizeHandle
          panel="properties"
          visible={propertiesPanelExpanded}
          onResize={(delta) => useUIStore.getState().setPanelWidthProperties(useUIStore.getState().panelWidthProperties + delta)}
        />
        <PropertiesPanel
          expanded={propertiesPanelExpanded}
          onToggle={handleTogglePropertiesPanel}
        >
          <AppPropertiesContent
            isTenantAdmin={isTenantAdmin}
            isWorkflowAgents={isWorkflowAgents}
            isWorkflowBuilder={isWorkflowBuilder}
            isWorkflowLog={sectionId === 'workflows' && subId === 'log'}
            configSelectedTenant={configSelectedTenant}
            configIsAddingNew={configIsAddingNew}
            workflowDraft={workflowDraft}
            workflowDirty={workflowDirty}
            workflowCatalogParameters={workflowCatalogParameters}
            selectedCanvasNodeId={selectedCanvasNodeId}
          />
        </PropertiesPanel>
      </div>
    </div>
  )
}

export default App
