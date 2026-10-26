/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { catalogStore } from '../../store/catalogStore'
import { workflowConfigurationStore } from '../../store/workflowConfigurationStore'
import { resolveNodeTypeLabel } from '../../lib/nodePresentation'
import {
  isEndNodeType,
  isStartNodeType,
} from '../../lib/boundaryNodes'
import {
  isAgentNodeType,
  isModelConsumerNodeType,
} from '../../lib/workflowModelProviders'
import type { WorkflowDocument, WorkflowNode } from '../../types/workflow'
import { CanvasStartNodeProperties } from './CanvasStartNodeProperties'
import { CanvasEndNodeProperties } from './CanvasEndNodeProperties'
import { CanvasAgentNodeProperties } from './CanvasAgentNodeProperties'
import { CanvasModelConsumerNodeProperties } from './CanvasModelConsumerNodeProperties'
import {
  CanvasDesignerNodeProperties,
  isDesignerConfigurableNode,
} from './CanvasDesignerNodeProperties'
import { NodeLabelInput } from './NodeLabelInput'

export interface CanvasNodePropertiesProps {
  workflow: WorkflowDocument
  node: WorkflowNode
  dirty: boolean
  onChange: (workflow: WorkflowDocument) => void
}

export function CanvasNodeProperties({
  workflow,
  node,
  dirty,
  onChange,
}: CanvasNodePropertiesProps) {
  const catalog = catalogStore((s) => s.catalog)
  const typeLabel = resolveNodeTypeLabel(workflow, node, catalog)

  if (isStartNodeType(node.type)) {
    return (
      <CanvasStartNodeProperties
        workflow={workflow}
        node={node}
        typeLabel={typeLabel}
        dirty={dirty}
        onChange={onChange}
      />
    )
  }

  if (isEndNodeType(node.type)) {
    return (
      <CanvasEndNodeProperties
        workflow={workflow}
        node={node}
        typeLabel={typeLabel}
        dirty={dirty}
        onChange={onChange}
      />
    )
  }

  if (isAgentNodeType(node.type)) {
    return (
      <CanvasAgentNodeProperties
        workflow={workflow}
        node={node}
        dirty={dirty}
        onChange={onChange}
      />
    )
  }

  if (isModelConsumerNodeType(node.type)) {
    return (
      <CanvasModelConsumerNodeProperties
        workflow={workflow}
        node={node}
        dirty={dirty}
        onChange={onChange}
      />
    )
  }

  if (isDesignerConfigurableNode(node)) {
    return (
      <CanvasDesignerNodeProperties
        workflow={workflow}
        node={node}
        typeLabel={typeLabel}
        dirty={dirty}
        onChange={onChange}
      />
    )
  }

  return (
    <div className="canvas-node-properties">
      <div className="side-panel-title">{typeLabel} node</div>
      <NodeLabelInput
        nodeId={node.id}
        label={node.label}
        placeholder={node.id}
        onChange={onChange}
      />
      <p className="canvas-node-properties-meta">Node id: {node.id}</p>
      {dirty ? <p className="canvas-node-properties-dirty">Unsaved changes</p> : null}
    </div>
  )
}

export function isCanvasNodePropertiesTarget(_node: WorkflowNode): boolean {
  return true
}

export function useSelectedCanvasNode(): WorkflowNode | null {
  const draft = workflowConfigurationStore((s) => s.draft)
  const nodeId = workflowConfigurationStore((s) => s.selectedCanvasNodeId)
  if (!draft || !nodeId) return null
  return draft.nodes?.find((n) => n.id === nodeId) ?? null
}
