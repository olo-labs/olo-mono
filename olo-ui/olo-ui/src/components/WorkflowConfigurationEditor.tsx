/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { useMemo } from 'react'
import { catalogStore } from '../store/catalogStore'
import type { CatalogParameter } from '../types/catalog'
import type { WorkflowDocument } from '../types/workflow'
import { workflowVariables } from '../lib/workflowResources'
import { WorkflowGlobalProperties } from './WorkflowGlobalProperties'
import {
  WorkflowParameterField,
  updateWorkflowParameterValue,
} from './WorkflowParameterField'

export interface WorkflowConfigurationEditorProps {
  workflow: WorkflowDocument | null
  catalogParameters: CatalogParameter[]
  dirty: boolean
  onChange: (workflow: WorkflowDocument) => void
  onSave: () => void
  onDelete: () => void
}

export function WorkflowConfigurationEditor({
  workflow,
  catalogParameters,
  dirty,
  onChange,
  onSave,
  onDelete,
}: WorkflowConfigurationEditorProps) {
  const sortedParameters = useMemo(
    () =>
      [...catalogParameters].sort(
        (a, b) => (a.ui?.order ?? 0) - (b.ui?.order ?? 0),
      ),
    [catalogParameters],
  )
  const workflowVariableNames = useMemo(
    () => (workflow ? workflowVariables(workflow).map((variable) => variable.name) : []),
    [workflow],
  )
  const catalogTools = catalogStore((state) => state.catalog?.tools ?? [])

  if (!workflow) {
    return <p className="tenant-config-form-empty">Select a workflow preset to edit.</p>
  }

  return (
    <div className="tenant-config-form-inner workflow-config-editor">
      <h2 className="tenant-config-form-title">{workflow.label ?? workflow.id}</h2>

      <WorkflowGlobalProperties workflow={workflow} onChange={onChange} />

      {sortedParameters.length > 0 ? (
        <section className="workflow-config-section">
          <h3 className="workflow-config-section-title">Parameters (catalog)</h3>
          {sortedParameters.map((descriptor) => (
            <div key={descriptor.id}>
              <WorkflowParameterField
                descriptor={descriptor}
                value={workflow.parameters?.[descriptor.id]?.defaultValue}
                workflow={workflow}
                workflowVariableNames={workflowVariableNames}
                catalogTools={catalogTools}
                onWorkflowChange={onChange}
                onChange={(value) => onChange(updateWorkflowParameterValue(workflow, descriptor.id, value))}
              />
              {descriptor.ui?.help || descriptor.description ? (
                <p className="workflow-param-help">{descriptor.ui?.help ?? descriptor.description}</p>
              ) : null}
            </div>
          ))}
        </section>
      ) : null}

      <section className="workflow-config-section">
        <h3 className="workflow-config-section-title">Graph</h3>
        <p className="workflow-config-graph-summary">
          {workflow.nodes?.length ?? 0} nodes · {workflow.edges?.length ?? 0} edges
        </p>
        <ul className="workflow-config-node-list">
          {(workflow.nodes ?? []).map((node) => (
            <li key={node.id}>
              <code>{node.id}</code> <span className="workflow-config-node-type">{node.type}</span>
            </li>
          ))}
        </ul>
      </section>

      <section className="workflow-config-section">
        <h3 className="workflow-config-section-title">JSON</h3>
        <textarea
          className="tenant-config-input workflow-config-json"
          value={JSON.stringify(workflow, null, 2)}
          readOnly
        />
        <p className="workflow-param-help">Use Agents for full JSON round-trip outside the catalog fields.</p>
      </section>

      <div className="tenant-config-form-actions tenant-config-form-actions-bottom">
        <button type="button" className="tenant-config-btn danger" onClick={onDelete}>
          Delete
        </button>
        <button
          type="button"
          className="tenant-config-btn primary"
          onClick={onSave}
          disabled={!dirty}
        >
          {dirty ? 'Save changes' : 'Saved'}
        </button>
      </div>
    </div>
  )
}
