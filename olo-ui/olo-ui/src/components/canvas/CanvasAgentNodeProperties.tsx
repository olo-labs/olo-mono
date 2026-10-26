/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import {
  agentModelOptions,
  applyAgentModelSelection,
  readAgentModelSelection,
} from '../../lib/workflowModelProviders'
import type { WorkflowDocument, WorkflowNode } from '../../types/workflow'
import { NodeLabelInput } from './NodeLabelInput'

export function CanvasAgentNodeProperties({
  workflow,
  node,
  dirty,
  onChange,
}: {
  workflow: WorkflowDocument
  node: WorkflowNode
  dirty: boolean
  onChange: (workflow: WorkflowDocument) => void
}) {
  const options = agentModelOptions(workflow)
  const selected = readAgentModelSelection(node, workflow)
  const selectedOption = options.find((option) => option.value === selected)
  const routingOptions = options.filter((option) => option.group === 'routing')
  const providerOptions = options.filter((option) => option.group === 'provider')

  return (
    <div className="canvas-node-properties">
      <div className="side-panel-title">Agent node</div>
      <p className="canvas-node-properties-hint">
        Choose model routing and provider for this agent. Edit the system prompt on the canvas node or in workflow parameters.
      </p>
      <NodeLabelInput
        nodeId={node.id}
        label={node.label}
        placeholder={node.id}
        onChange={onChange}
      />
      <p className="canvas-node-properties-meta">Node id: {node.id}</p>
      {options.length === 0 ? (
        <p className="builder-empty">Add model providers in the Builder panel first.</p>
      ) : (
        <label className="canvas-node-select-field">
          <span>Model routing / provider</span>
          <select
            className="workflow-canvas-select"
            value={selected}
            onChange={(e) =>
              onChange(applyAgentModelSelection(workflow, node.id, e.target.value))
            }
          >
            {routingOptions.length > 0 ? (
              <optgroup label="Routing">
                {routingOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label} — {option.description}
                  </option>
                ))}
              </optgroup>
            ) : null}
            {providerOptions.length > 0 ? (
              <optgroup label="Providers">
                {providerOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label} ({option.description})
                  </option>
                ))}
              </optgroup>
            ) : null}
          </select>
        </label>
      )}
      {selectedOption ? (
        <p className="canvas-node-properties-meta">{selectedOption.description}</p>
      ) : null}
      {dirty ? <p className="canvas-node-properties-dirty">Unsaved changes</p> : null}
    </div>
  )
}
