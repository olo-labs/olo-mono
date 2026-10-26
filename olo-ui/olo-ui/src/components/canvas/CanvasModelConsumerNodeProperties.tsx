/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import {
  applyProviderRef,
  providerKind,
  readProviderRef,
  workflowModelProviders,
} from '../../lib/workflowModelProviders'
import type { WorkflowDocument, WorkflowNode } from '../../types/workflow'
import { NodeLabelInput } from './NodeLabelInput'

export function CanvasModelConsumerNodeProperties({
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
  const providers = workflowModelProviders(workflow)
  const selected = readProviderRef(node)
  const selectedProvider = providers.find((p) => p.id === selected)

  return (
    <div className="canvas-node-properties">
      <div className="side-panel-title">{node.type} node</div>
      <p className="canvas-node-properties-hint">
        Choose which workflow model provider this node uses at runtime.
      </p>
      <NodeLabelInput
        nodeId={node.id}
        label={node.label}
        placeholder={node.id}
        onChange={onChange}
      />
      <p className="canvas-node-properties-meta">Node id: {node.id}</p>
      {providers.length === 0 ? (
        <p className="builder-empty">Add model providers in the Builder panel first.</p>
      ) : (
        <label className="canvas-node-select-field">
          <span>Model provider</span>
          <select
            className="workflow-canvas-select"
            value={selected}
            onChange={(e) => onChange(applyProviderRef(workflow, node.id, e.target.value))}
          >
            <option value="">Select provider…</option>
            {providers.map((provider) => (
              <option key={provider.id} value={provider.id}>
                {provider.id} ({providerKind(provider)} · {provider.model || 'default'})
              </option>
            ))}
          </select>
        </label>
      )}
      {selectedProvider ? (
        <p className="canvas-node-properties-meta">
          {providerKind(selectedProvider)} → {selectedProvider.model || 'unspecified model'}
        </p>
      ) : null}
      {dirty ? <p className="canvas-node-properties-dirty">Unsaved changes</p> : null}
    </div>
  )
}
