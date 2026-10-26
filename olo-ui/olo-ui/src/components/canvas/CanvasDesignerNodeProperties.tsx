/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { useMemo } from 'react'
import { catalogStore } from '../../store/catalogStore'
import { resolveNodePresentation } from '../../lib/nodePresentation'
import { readNodeConfigValue } from '../../lib/nodeConfiguration'
import { normalizeNodeType } from '../../lib/boundaryNodes'
import type { WorkflowDocument, WorkflowNode } from '../../types/workflow'
import { NodeLabelInput } from './NodeLabelInput'
import { InlinePropertyBlock } from './InlinePropertyBlock'

export function CanvasDesignerNodeProperties({
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
  const catalog = catalogStore((s) => s.catalog)
  const presentation = useMemo(
    () => resolveNodePresentation(workflow, node, catalog),
    [catalog, node, workflow],
  )
  const toolId = readNodeConfigValue(node, 'toolId')

  return (
    <div className="canvas-node-properties">
      <div className="side-panel-title">{typeLabel} node</div>
      {presentation.description ? (
        <p className="canvas-node-properties-hint">{presentation.description}</p>
      ) : null}
      <NodeLabelInput
        nodeId={node.id}
        label={node.label}
        placeholder={node.id}
        onChange={onChange}
      />
      <p className="canvas-node-properties-meta">Node id: {node.id}</p>
      {toolId ? <p className="canvas-node-properties-meta">Tool: {toolId}</p> : null}
      {presentation.inlineProperties.length === 0 ? (
        <p className="builder-empty">No designer properties for this node type.</p>
      ) : (
        <div className="canvas-node-designer-properties">
          {presentation.inlineProperties.map((property) => (
            <InlinePropertyBlock
              key={property.id}
              property={property}
              workflow={workflow}
              node={node}
              catalog={catalog}
              onChange={onChange}
            />
          ))}
        </div>
      )}
      {dirty ? <p className="canvas-node-properties-dirty">Unsaved changes</p> : null}
    </div>
  )
}

export function isDesignerConfigurableNode(node: WorkflowNode): boolean {
  const type = normalizeNodeType(node.type)
  return type === 'TOOL' || type === 'VECTOR_SEARCH'
}
