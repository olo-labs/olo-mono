/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { memo, useLayoutEffect, type CSSProperties } from 'react'
import { useNodeId, useUpdateNodeInternals, type NodeProps } from '@xyflow/react'
import { catalogStore } from '../../store/catalogStore'
import { graphLogStore } from '../../store/graphLogStore'
import { workflowConfigurationStore } from '../../store/workflowConfigurationStore'
import { nodeTooltipLines } from '../../lib/canvasLabels'
import { findCatalogComponent } from '../../lib/catalogLookup'
import { resolvePortColors } from '../../lib/workflowDesigner'
import {
  catalogComponentToPorts,
  groupPortsBySide,
  isDelegateAgentNode,
  isMessagePortId,
  resolveNodePorts,
  usesPlannerRoutedMessagePorts,
} from '../../lib/workflowNodePorts'
import type { CatalogFlowNodeData } from '../../lib/workflowGraph'
import type { WorkflowPort } from '../../types/workflow'
import { ChildWorkflowNodeBody } from './ChildWorkflowNodeBody'
import { NodeInlineLabel } from './NodeInlineLabel'
import { NodeInlineProperties } from './NodeInlineProperties'
import { BoundaryPorts, InlineLateralPorts, PortRow } from './CatalogFlowNodePorts'

function CatalogFlowNodeComponent({ id, data, selected }: NodeProps) {
  const nodeId = useNodeId()
  const updateNodeInternals = useUpdateNodeInternals()
  const catalog = catalogStore.getState().catalog
  const builderDraft = workflowConfigurationStore((s) => s.draft)
  const logDraft = graphLogStore((s) => s.draft)
  const updateDraft = workflowConfigurationStore((s) => s.updateDraft)
  const nodeData = data as CatalogFlowNodeData
  const readOnly = Boolean(nodeData.readOnly)
  const draft = readOnly && logDraft ? logDraft : builderDraft
  const workflowNode = draft?.nodes?.find((n) => n.id === id)
  const presentation = nodeData.presentation
  const portColors = resolvePortColors(draft)
  const descriptor = findCatalogComponent(catalog, nodeData.workflowType, workflowNode)
  const catalogPorts = descriptor ? catalogComponentToPorts(descriptor) : []
  const mergedPorts = resolveNodePorts(
    nodeData.workflowType,
    nodeData.workflowPorts,
    catalogPorts,
    workflowNode?.configuration,
  )
  const portsBySide = groupPortsBySide(
    mergedPorts.length > 0 ? mergedPorts : catalogPorts,
  )
  const tooltipLines = nodeTooltipLines(id, nodeData, descriptor)

  const dimMessagePorts = usesPlannerRoutedMessagePorts(
    nodeData.workflowType,
    workflowNode?.configuration,
  )
  const isChildWorkflow = isDelegateAgentNode(workflowNode?.configuration)
  const delegateAgentId =
    typeof workflowNode?.configuration?.delegateAgentId === 'string'
      ? workflowNode.configuration.delegateAgentId
      : ''
  const messageLeftPorts = portsBySide.LEFT.filter((port) => isMessagePortId(port.id))
  const messageRightPorts = portsBySide.RIGHT.filter((port) => isMessagePortId(port.id))

  const renderPortRow = (
    port: WorkflowPort,
    side: import('../../lib/workflowNodePorts').PortSide,
    index: number,
    count: number,
  ) => (
    <PortRow
      key={`${side}-${port.id}`}
      port={port}
      side={side}
      index={index}
      count={count}
      portColors={portColors}
      inline
      dimmed={dimMessagePorts && isMessagePortId(port.id)}
    />
  )

  const nodeStyle = {
    '--catalog-node-min-width': presentation ? `${presentation.width}px` : undefined,
    '--catalog-node-max-width': presentation ? `${presentation.width}px` : undefined,
    '--catalog-node-min-height': presentation ? `${presentation.height}px` : undefined,
    '--catalog-node-selection-border': presentation?.selectionBorder,
  } as CSSProperties

  const hasLateralPorts =
    !isChildWorkflow && (portsBySide.LEFT.length > 0 || portsBySide.RIGHT.length > 0)

  useLayoutEffect(() => {
    if (!nodeId) return
    updateNodeInternals(nodeId)
  }, [
    nodeId,
    updateNodeInternals,
    readOnly,
    isChildWorkflow,
    portsBySide.BOTTOM.length,
    portsBySide.TOP.length,
    portsBySide.LEFT.length,
    portsBySide.RIGHT.length,
    presentation?.height,
    presentation?.width,
  ])

  return (
    <div
      className={[
        'catalog-flow-node',
        isChildWorkflow ? 'catalog-flow-node-child-workflow' : '',
        selected ? 'selected' : '',
      ].filter(Boolean).join(' ')}
      style={nodeStyle}
    >
      <div className="catalog-flow-node-tooltip" role="tooltip">
        {tooltipLines.map((line) => (
          <span key={line}>{line}</span>
        ))}
      </div>

      <BoundaryPorts side="TOP" ports={portsBySide.TOP} portColors={portColors} />

      <div className="catalog-flow-node-body">
        {isChildWorkflow ? (
          <ChildWorkflowNodeBody
            delegateAgentId={delegateAgentId}
            nodeLabel={nodeData.label}
            leftPorts={messageLeftPorts}
            rightPorts={messageRightPorts}
            readOnly={readOnly}
            renderPortRow={renderPortRow}
          />
        ) : (
          <>
            <div className="catalog-flow-node-header">
              <span className="catalog-flow-node-emoji" aria-hidden>
                {presentation?.emoji ?? nodeData.emoji ?? descriptor?.emoji ?? '▢'}
              </span>
              <div className="catalog-flow-node-titles">
                <NodeInlineLabel
                  nodeId={id}
                  label={nodeData.label}
                  placeholder={id}
                  readOnly={readOnly}
                  onChange={updateDraft}
                />
                <span className="catalog-flow-node-type">{presentation?.typeLabel ?? nodeData.workflowType}</span>
              </div>
            </div>

            {hasLateralPorts ? (
              <InlineLateralPorts
                leftPorts={portsBySide.LEFT}
                rightPorts={portsBySide.RIGHT}
                portColors={portColors}
                dimMessagePorts={dimMessagePorts}
              />
            ) : null}

            {workflowNode && draft ? (
              <NodeInlineProperties
                workflow={draft}
                node={workflowNode}
                catalog={catalog}
                readOnly={readOnly}
                onChange={updateDraft}
              />
            ) : null}
          </>
        )}
      </div>

      <BoundaryPorts side="BOTTOM" ports={portsBySide.BOTTOM} portColors={portColors} />
    </div>
  )
}

export const CatalogFlowNode = memo(CatalogFlowNodeComponent)
