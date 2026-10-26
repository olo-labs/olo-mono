/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { useLocation, useNavigate } from 'react-router-dom'
import { downloadWorkflowJson, readWorkflowFile, renameWorkflowPath } from '../lib/workflowConfiguration'
import { workflowConfigurationStore } from '../store/workflowConfigurationStore'
import { tenantConfigStore } from '../store/tenantConfig'
import type { Tenant } from '../types/tenant'
import type { AgentsFileActions } from '../components/agents/AgentsContextMenu'
import { buildPath, buildPathWithQuery, buildQuery, parseQuery, parsedToPanelParams } from '../routes'
import { getSubOption } from '../types/layout'
import type { SectionId } from '../types/layout'

export function useAppPanelNavigation() {
  const location = useLocation()
  const navigate = useNavigate()
  const q = parseQuery(location.search)

  const updatePanelQuery = (updates: { menu?: 0 | 1; tools?: 0 | 1; props?: 0 | 1 }) => {
    const params = parsedToPanelParams(q)
    const next = { ...params, ...updates }
    navigate(location.pathname + '?' + buildQuery(next), { replace: true })
  }

  const handleSectionSubSelect = (sid: SectionId, sub: string) => {
    const params = parsedToPanelParams(q)
    const subOption = getSubOption(sid, sub)
    const openProps = (sid === 'administration' && sub === 'tenants') || sub === 'agents'
    navigate(
      buildPathWithQuery(buildPath(sid, sub), {
        ...params,
        props: openProps ? params.props : 0,
        tools: subOption?.componentsPanel ? 1 : 0,
      }),
    )
  }

  return { q, updatePanelQuery, handleSectionSubSelect }
}

export function useAppWorkflowActions(
  updatePanelQuery: (updates: { menu?: 0 | 1; tools?: 0 | 1; props?: 0 | 1 }) => void,
  handleSectionSubSelect: (sid: SectionId, sub: string) => void,
) {
  const handleSelectWorkflow = (fileName: string) => {
    workflowConfigurationStore.getState().selectWorkflow(fileName)
    updatePanelQuery({ props: 1 })
  }

  const handleImportWorkflowFile = async (file: File) => {
    const document = await readWorkflowFile(file)
    await workflowConfigurationStore.getState().importWorkflow(document, file.name)
    updatePanelQuery({ props: 1 })
  }

  const handleOpenWorkflow = (fileName: string) => {
    void workflowConfigurationStore.getState().selectWorkflow(fileName)
    updatePanelQuery({ props: 1 })
  }

  const handleEditWorkflowInBuilder = async (fileName: string) => {
    await workflowConfigurationStore.getState().selectWorkflow(fileName)
    handleSectionSubSelect('workflows', 'builder')
  }

  const handleDebugWorkflow = async (fileName: string) => {
    await workflowConfigurationStore.getState().selectWorkflow(fileName)
    handleSectionSubSelect('workflows', 'debugger')
  }

  const handleCopyWorkflowPath = (path: string) => {
    void navigator.clipboard.writeText(path)
  }

  const handleDuplicateWorkflow = async (fileName: string) => {
    await workflowConfigurationStore.getState().copyWorkflow(fileName)
    updatePanelQuery({ props: 1 })
  }

  const handleRenameWorkflow = async (fileName: string) => {
    const base = fileName.replace(/\\/g, '/').split('/').pop() ?? fileName
    const next = window.prompt('Rename workflow file', base)
    if (!next?.trim()) return
    try {
      const newPath = renameWorkflowPath(fileName, next)
      await workflowConfigurationStore.getState().renameWorkflow(fileName, newPath)
      updatePanelQuery({ props: 1 })
    } catch (e) {
      window.alert(e instanceof Error ? e.message : 'Rename failed')
    }
  }

  const handleDeleteWorkflowFile = async (fileName: string) => {
    if (!window.confirm(`Delete ${fileName}?`)) return
    await workflowConfigurationStore.getState().deleteWorkflow(fileName)
  }

  const handleExportWorkflowFile = async (fileName: string) => {
    const doc = await workflowConfigurationStore.getState().exportWorkflow(fileName)
    downloadWorkflowJson(doc, fileName)
  }

  const agentsFileActions: AgentsFileActions = {
    onOpen: handleOpenWorkflow,
    onEditInBuilder: (fileName) => { void handleEditWorkflowInBuilder(fileName) },
    onDebug: (fileName) => { void handleDebugWorkflow(fileName) },
    onCopy: handleDuplicateWorkflow,
    onRename: handleRenameWorkflow,
    onDelete: handleDeleteWorkflowFile,
    onExport: handleExportWorkflowFile,
    onCopyPath: handleCopyWorkflowPath,
  }

  return {
    handleSelectWorkflow,
    handleImportWorkflowFile,
    agentsFileActions,
  }
}

export function useAppTenantActions(
  updatePanelQuery: (updates: { menu?: 0 | 1; tools?: 0 | 1; props?: 0 | 1 }) => void,
) {
  const handleSelectTenant = (t: Tenant) => {
    tenantConfigStore.getState().selectTenant(t)
    updatePanelQuery({ props: 1 })
  }

  const handleAddNewTenant = () => {
    tenantConfigStore.getState().startAddNew()
    updatePanelQuery({ props: 1 })
  }

  return { handleSelectTenant, handleAddNewTenant }
}
