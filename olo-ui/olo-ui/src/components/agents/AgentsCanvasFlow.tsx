/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { useCallback, useMemo } from 'react'
import {
  ReactFlow,
  Background,
  Controls,
  MiniMap,
  type Node,
} from '@xyflow/react'
import {
  CONFIG_FILE_NODE,
  CONFIG_FOLDER_NODE,
  workflowsToDirectoryGraph,
  type ConfigFileNodeData,
} from '../../lib/configurationTree'
import {
  AgentsContextMenu,
  agentsContextTargetFromNode,
  type AgentsCanvasActions,
  type AgentsContextMenuState,
  type AgentsFileActions,
} from './AgentsContextMenu'
import { ConfigurationFileNode } from './ConfigurationFileNode'
import { ConfigurationFolderNode } from './ConfigurationFolderNode'
import type { WorkflowSummary } from '../../types/workflow'

const nodeTypes = {
  [CONFIG_FOLDER_NODE]: ConfigurationFolderNode,
  [CONFIG_FILE_NODE]: ConfigurationFileNode,
}

export function AgentsCanvasFlow({
  workflows,
  configurationRoot,
  loading,
  selectedFileName,
  fileActions,
  canvasActions,
  contextMenu,
  onContextMenuChange,
}: {
  workflows: WorkflowSummary[]
  configurationRoot: string
  loading: boolean
  selectedFileName: string | null
  fileActions: AgentsFileActions
  canvasActions: AgentsCanvasActions
  contextMenu: AgentsContextMenuState | null
  onContextMenuChange: (menu: AgentsContextMenuState | null) => void
}) {
  const graph = useMemo(
    () => workflowsToDirectoryGraph(workflows, configurationRoot),
    [workflows, configurationRoot],
  )

  const nodes = useMemo(
    () =>
      graph.nodes.map((node) => ({
        ...node,
        selected: node.type === CONFIG_FILE_NODE
          && (node.data as unknown as ConfigFileNodeData).fileName === selectedFileName,
      })),
    [graph.nodes, selectedFileName],
  )

  const closeContextMenu = useCallback(() => onContextMenuChange(null), [onContextMenuChange])

  const handleNodeClick = useCallback(
    (_event: React.MouseEvent, node: Node) => {
      if (node.type !== CONFIG_FILE_NODE) return
      const data = node.data as unknown as ConfigFileNodeData
      fileActions.onOpen(data.fileName)
    },
    [fileActions],
  )

  const handleNodeContextMenu = useCallback(
    (event: React.MouseEvent, node: Node) => {
      event.preventDefault()
      const target = agentsContextTargetFromNode(node.type, node.data)
      if (!target) return
      if (target.kind === 'file') {
        fileActions.onOpen(target.fileName)
      }
      onContextMenuChange({ x: event.clientX, y: event.clientY, target })
    },
    [fileActions, onContextMenuChange],
  )

  const handlePaneContextMenu = useCallback((event: MouseEvent | React.MouseEvent) => {
    event.preventDefault()
    onContextMenuChange({ x: event.clientX, y: event.clientY, target: { kind: 'canvas' } })
  }, [onContextMenuChange])

  return (
    <div className="agents-canvas-flow">
      {loading && workflows.length === 0 ? (
        <p className="agents-canvas-message">Loading directory…</p>
      ) : workflows.length === 0 ? (
        <p className="agents-canvas-message">No workflows. Import a JSON preset to begin.</p>
      ) : (
        <ReactFlow
          nodes={nodes}
          edges={graph.edges}
          nodeTypes={nodeTypes}
          onNodeClick={handleNodeClick}
          onNodeContextMenu={handleNodeContextMenu}
          onPaneContextMenu={handlePaneContextMenu}
          onPaneClick={closeContextMenu}
          nodesDraggable={false}
          nodesConnectable={false}
          elementsSelectable
          fitView
          fitViewOptions={{ padding: 0.2 }}
          minZoom={0.2}
          maxZoom={1.5}
          proOptions={{ hideAttribution: true }}
        >
          <Background gap={20} size={1} />
          <Controls showInteractive={false} />
          <MiniMap
            nodeColor={(node) => (node.type === CONFIG_FILE_NODE ? '#3b82f6' : '#52525b')}
            maskColor="rgba(9, 9, 11, 0.75)"
          />
        </ReactFlow>
      )}

      {contextMenu ? (
        <AgentsContextMenu
          menu={contextMenu}
          fileActions={fileActions}
          canvasActions={canvasActions}
          onClose={closeContextMenu}
        />
      ) : null}
    </div>
  )
}
