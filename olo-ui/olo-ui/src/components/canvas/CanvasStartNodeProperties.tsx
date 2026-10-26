/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { applyStartInputMappings, readStartInputMappings } from '../../lib/boundaryNodes'
import { workflowVariables } from '../../lib/workflowResources'
import type { WorkflowDocument, WorkflowNode } from '../../types/workflow'
import { NodeLabelInput } from './NodeLabelInput'

export function CanvasStartNodeProperties({
  workflow,
  node,
  typeLabel,
  dirty,
  onChange,
}: {
  workflow: WorkflowDocument
  node: WorkflowNode
  typeLabel: string
  dirty: boolean
  onChange: (workflow: WorkflowDocument) => void
}) {
  const variables = workflowVariables(workflow)
  const selected = new Set(readStartInputMappings(node))

  return (
    <div className="canvas-node-properties">
      <div className="side-panel-title">{typeLabel} node</div>
      <p className="canvas-node-properties-hint">
        Map workflow variables that receive caller input at the start of the run.
      </p>
      <NodeLabelInput
        nodeId={node.id}
        label={node.label}
        placeholder={node.id}
        onChange={onChange}
      />
      <p className="canvas-node-properties-meta">Node id: {node.id}</p>
      {variables.length === 0 ? (
        <p className="builder-empty">Define workflow variables in the Builder panel first.</p>
      ) : (
        <ul className="builder-check-list canvas-node-variable-list">
          {variables.map((variable) => (
            <li key={variable.name} className="builder-check-item">
              <label className="builder-check-label">
                <input
                  type="checkbox"
                  checked={selected.has(variable.name)}
                  onChange={(e) => {
                    const next = new Set(selected)
                    if (e.target.checked) next.add(variable.name)
                    else next.delete(variable.name)
                    onChange(applyStartInputMappings(workflow, node.id, [...next]))
                  }}
                />
                <span className="builder-check-text">
                  <span className="builder-check-name">{variable.name}</span>
                  {variable.description ? (
                    <span className="builder-check-desc">{variable.description}</span>
                  ) : null}
                </span>
              </label>
            </li>
          ))}
        </ul>
      )}
      {dirty ? <p className="canvas-node-properties-dirty">Unsaved changes</p> : null}
    </div>
  )
}
