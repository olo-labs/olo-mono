/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import {
  ReactFlow,
  Background,
  Controls,
  MiniMap,
} from '@xyflow/react'
import { resolveCanvasTheme } from '../../lib/workflowDesigner'
import type { WorkflowDocument } from '../../types/workflow'
import { CanvasContextMenu } from './CanvasContextMenu'
import { CanvasViewportController } from './CanvasViewportController'
import {
  defaultFlowEdgeOptions,
  workflowCanvasEdgeTypes,
  workflowCanvasNodeTypes,
} from './workflowCanvasConstants'
import type { useWorkflowCanvasGraph } from '../../hooks/useWorkflowCanvasGraph'
import type { useWorkflowCanvasHandlers } from '../../hooks/useWorkflowCanvasHandlers'

type GraphState = ReturnType<typeof useWorkflowCanvasGraph>
type HandlerState = ReturnType<typeof useWorkflowCanvasHandlers>

export interface WorkflowCanvasFlowProps {
  draft: WorkflowDocument
  readOnly: boolean
  allowNodeDrag: boolean
  graph: GraphState
  handlers: HandlerState
  syncingRef: React.MutableRefObject<boolean>
}

export function WorkflowCanvasFlow({
  draft,
  readOnly,
  allowNodeDrag,
  graph,
  handlers,
  syncingRef,
}: WorkflowCanvasFlowProps) {
  const canvasTheme = resolveCanvasTheme(draft)
  const handleNodesChange = readOnly
    ? (allowNodeDrag ? graph.onNodesChangeLayoutOnly : undefined)
    : graph.onNodesChange

  return (
    <>
      <ReactFlow
        nodes={graph.nodes}
        edges={graph.edges}
        defaultViewport={graph.savedCanvasView?.viewport}
        onNodesChange={handleNodesChange}
        onEdgesChange={readOnly ? undefined : graph.onEdgesChange}
        onConnect={handlers.onConnect}
        onConnectStart={readOnly ? undefined : handlers.onConnectStart}
        onConnectEnd={readOnly ? undefined : handlers.onConnectEnd}
        onMoveEnd={readOnly && !allowNodeDrag ? undefined : graph.schedulePersistCanvasView}
        connectionLineStyle={{ stroke: handlers.connectionLineColor, strokeWidth: 1.5 }}
        defaultEdgeOptions={defaultFlowEdgeOptions}
        defaultMarkerColor={handlers.connectionLineColor}
        isValidConnection={readOnly ? undefined : handlers.isValidConnection}
        onNodesDelete={handlers.onNodesDelete}
        onEdgesDelete={handlers.onEdgesDelete}
        onNodeDragStop={allowNodeDrag ? undefined : handlers.onNodeDragStop}
        nodesConnectable={!readOnly}
        onSelectionChange={(params) => handlers.onSelectionChange(params, syncingRef)}
        onNodeContextMenu={handlers.onNodeContextMenu}
        onEdgeContextMenu={handlers.onEdgeContextMenu}
        onPaneClick={handlers.closeContextMenu}
        onDragOver={handlers.onDragOver}
        onDrop={handlers.onDrop}
        nodeTypes={workflowCanvasNodeTypes}
        edgeTypes={workflowCanvasEdgeTypes}
        deleteKeyCode={readOnly ? null : ['Backspace', 'Delete']}
        proOptions={{ hideAttribution: true }}
      >
        <CanvasViewportController
          workflowKey={graph.workflowCanvasKey}
          savedView={graph.savedCanvasView}
          nodeCount={graph.nodes.length}
          onViewApplied={readOnly && !allowNodeDrag ? undefined : graph.finishCanvasHydration}
        />
        <Background gap={canvasTheme.gridGap} size={1} color={canvasTheme.backgroundColor} />
        <Controls showInteractive={!readOnly || allowNodeDrag} />
        <MiniMap
          nodeColor={canvasTheme.minimapNodeColor}
          maskColor="rgba(9, 9, 11, 0.75)"
          className="workflow-canvas-minimap"
        />
      </ReactFlow>
      {handlers.contextMenu ? (
        <CanvasContextMenu
          menu={handlers.contextMenu}
          readOnly={readOnly}
          onClose={handlers.closeContextMenu}
          onOpenProperties={handlers.openNodeProperties}
          onDeleteNode={handlers.deleteNodeById}
          onDeleteEdge={handlers.deleteEdgeById}
        />
      ) : null}
    </>
  )
}
