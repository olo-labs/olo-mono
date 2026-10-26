/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { useMemo } from 'react'
import { ReactFlowProvider } from '@xyflow/react'
import { CanvasToolbar } from './CanvasToolbar'
import { WorkflowCanvasFlow } from './WorkflowCanvasFlow'
import type { WorkflowCanvasProps } from './workflowCanvasConstants'
import {
  usePersistGraph,
  useWorkflowCanvasDraft,
  useWorkflowCanvasGraph,
} from '../../hooks/useWorkflowCanvasGraph'
import { useWorkflowCanvasHandlers } from '../../hooks/useWorkflowCanvasHandlers'

function WorkflowCanvasInner({ readOnly = false, mode = 'builder' }: WorkflowCanvasProps) {
  const { isLogMode, draft, selectedFileName, catalog } = useWorkflowCanvasDraft(mode)
  const allowNodeDrag = isLogMode
  const flowOptions = useMemo(
    () => ({ readOnly, allowNodeDrag, autoLayout: isLogMode }),
    [readOnly, allowNodeDrag, isLogMode],
  )

  const graph = useWorkflowCanvasGraph(draft, catalog, flowOptions, selectedFileName, readOnly)
  const persistGraph = usePersistGraph(draft, catalog, readOnly, graph.syncingRef)
  const handlers = useWorkflowCanvasHandlers({
    draft,
    catalog,
    nodes: graph.nodes,
    edges: graph.edges,
    setNodes: graph.setNodes,
    setEdges: graph.setEdges,
    readOnly,
    allowNodeDrag,
    persistGraph,
  })

  if (!draft) {
    return (
      <div className="workflow-canvas">
        <CanvasToolbar readOnly={readOnly} mode={mode} />
        <div className="workflow-canvas-empty">
          {isLogMode ? (
            <p>Select a logged graph from the menu above. Logs appear when the runtime injects dynamic subgraphs.</p>
          ) : (
            <p>Select a workflow from the menu above, or import one under <strong>Agents</strong>.</p>
          )}
        </div>
      </div>
    )
  }

  return (
    <div className="workflow-canvas" ref={graph.canvasContainerRef}>
      <CanvasToolbar readOnly={readOnly} mode={mode} />
      <WorkflowCanvasFlow
        draft={draft}
        readOnly={readOnly}
        allowNodeDrag={allowNodeDrag}
        graph={graph}
        handlers={handlers}
        syncingRef={graph.syncingRef}
      />
    </div>
  )
}

export function WorkflowCanvas(props: WorkflowCanvasProps) {
  return (
    <ReactFlowProvider>
      <WorkflowCanvasInner {...props} />
    </ReactFlowProvider>
  )
}

export type { WorkflowCanvasMode, WorkflowCanvasProps } from './workflowCanvasConstants'
