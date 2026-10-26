/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { memo } from 'react'
import { Handle, Position, type NodeProps } from '@xyflow/react'
import type { ConfigFolderNodeData } from '../../lib/configurationTree'

function ConfigurationFolderNodeComponent({ data, selected }: NodeProps) {
  const folder = data as unknown as ConfigFolderNodeData
  return (
    <div className={`agents-tree-node agents-tree-folder ${selected ? 'selected' : ''}`}>
      <Handle type="target" position={Position.Top} className="agents-tree-handle" />
      <span className="agents-tree-node-icon" aria-hidden>📁</span>
      <div className="agents-tree-node-text">
        <span className="agents-tree-node-title">{folder.label}</span>
        {folder.path ? <span className="agents-tree-node-meta">{folder.path}/</span> : null}
      </div>
      <Handle type="source" position={Position.Bottom} className="agents-tree-handle" />
    </div>
  )
}

export const ConfigurationFolderNode = memo(ConfigurationFolderNodeComponent)
