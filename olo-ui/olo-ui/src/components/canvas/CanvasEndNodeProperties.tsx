/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { applyEndOutputMapping, readEndOutputMapping } from '../../lib/boundaryNodes'
import { workflowVariables } from '../../lib/workflowResources'
import type { WorkflowDocument, WorkflowNode } from '../../types/workflow'
import { NodeLabelInput } from './NodeLabelInput'

export function CanvasEndNodeProperties({
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
  const selected = readEndOutputMapping(node, workflow)

  return (
    <div className="canvas-node-properties">
      <div className="side-panel-title">{typeLabel} node</div>
      <p className="canvas-node-properties-hint">
        Select the workflow variable returned to the caller when the run completes.
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
        <label className="canvas-node-select-field">
          <span>Output variable</span>
          <select
            className="workflow-canvas-select"
            value={selected}
            onChange={(e) => {
              onChange(applyEndOutputMapping(workflow, node.id, e.target.value))
            }}
          >
            <option value="">Select variable…</option>
            {variables.map((variable) => (
              <option key={variable.name} value={variable.name}>
                {variable.name}
              </option>
            ))}
          </select>
        </label>
      )}
      {dirty ? <p className="canvas-node-properties-dirty">Unsaved changes</p> : null}
    </div>
  )
}
