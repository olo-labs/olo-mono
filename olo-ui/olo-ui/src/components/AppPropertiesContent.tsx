/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { TenantConfigForm } from './TenantConfigForm'
import { WorkflowConfigurationEditor } from './WorkflowConfigurationEditor'
import { WorkflowGlobalProperties } from './WorkflowGlobalProperties'
import { CanvasNodeProperties, isCanvasNodePropertiesTarget } from './canvas/CanvasNodeProperties'
import { workflowConfigurationStore } from '../store/workflowConfigurationStore'
import { tenantConfigStore } from '../store/tenantConfig'
import type { WorkflowDocument } from '../types/workflow'
import type { CatalogParameter } from '../types/catalog'
import type { Tenant } from '../types/tenant'

export interface AppPropertiesContentProps {
  isTenantAdmin: boolean
  isWorkflowAgents: boolean
  isWorkflowBuilder: boolean
  isWorkflowLog: boolean
  configSelectedTenant: Tenant | null
  configIsAddingNew: boolean
  workflowDraft: WorkflowDocument | null
  workflowDirty: boolean
  workflowCatalogParameters: CatalogParameter[]
  selectedCanvasNodeId: string | null
}

export function AppPropertiesContent({
  isTenantAdmin,
  isWorkflowAgents,
  isWorkflowBuilder,
  isWorkflowLog,
  configSelectedTenant,
  configIsAddingNew,
  workflowDraft,
  workflowDirty,
  workflowCatalogParameters,
  selectedCanvasNodeId,
}: AppPropertiesContentProps) {
  const selectedCanvasNode =
    workflowDraft?.nodes?.find((n) => n.id === selectedCanvasNodeId) ?? null
  const showCanvasNodeProperties =
    isWorkflowBuilder
    && selectedCanvasNode != null
    && isCanvasNodePropertiesTarget(selectedCanvasNode)
  const showWorkflowGlobalProperties =
    isWorkflowBuilder && workflowDraft != null && selectedCanvasNode == null
  const showWorkflowProperties = (isWorkflowAgents || isWorkflowBuilder) && !isWorkflowLog

  if (isTenantAdmin) {
    return (
      <TenantConfigForm
        tenant={configSelectedTenant}
        isAddingNew={configIsAddingNew}
        onSave={(tenant) => tenantConfigStore.getState().saveTenant(tenant)}
      />
    )
  }

  if (showCanvasNodeProperties && selectedCanvasNode && workflowDraft) {
    return (
      <CanvasNodeProperties
        workflow={workflowDraft}
        node={selectedCanvasNode}
        dirty={workflowDirty}
        onChange={(doc) => workflowConfigurationStore.getState().updateDraft(doc)}
      />
    )
  }

  if (showWorkflowGlobalProperties && workflowDraft) {
    return (
      <div className="tenant-config-form-inner workflow-config-editor">
        <h2 className="tenant-config-form-title">{workflowDraft.label ?? workflowDraft.id}</h2>
        <WorkflowGlobalProperties
          workflow={workflowDraft}
          onChange={(doc) => workflowConfigurationStore.getState().updateDraft(doc)}
        />
        <div className="tenant-config-form-actions tenant-config-form-actions-bottom">
          <button
            type="button"
            className="tenant-config-btn danger"
            onClick={() => workflowConfigurationStore.getState().deleteSelected()}
          >
            Delete
          </button>
          <button
            type="button"
            className="tenant-config-btn primary"
            onClick={() => workflowConfigurationStore.getState().saveDraft()}
            disabled={!workflowDirty}
          >
            {workflowDirty ? 'Save changes' : 'Saved'}
          </button>
        </div>
      </div>
    )
  }

  if (showWorkflowProperties) {
    return (
      <WorkflowConfigurationEditor
        workflow={workflowDraft}
        catalogParameters={workflowCatalogParameters}
        dirty={workflowDirty}
        onChange={(doc) => workflowConfigurationStore.getState().updateDraft(doc)}
        onSave={() => workflowConfigurationStore.getState().saveDraft()}
        onDelete={() => workflowConfigurationStore.getState().deleteSelected()}
      />
    )
  }

  return undefined
}
