/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { memo } from 'react'
import { Handle, Position, type NodeProps } from '@xyflow/react'
import type { ConfigFileNodeData } from '../../lib/configurationTree'

function ConfigurationFileNodeComponent({ data, selected }: NodeProps) {
  const file = data as unknown as ConfigFileNodeData
  return (
    <div className={`agents-tree-node agents-tree-file ${selected ? 'selected' : ''}`}>
      <Handle type="target" position={Position.Top} className="agents-tree-handle" />
      <span className="agents-tree-node-icon" aria-hidden>📄</span>
      <div className="agents-tree-node-text">
        <span className="agents-tree-node-title">{file.label}</span>
        <span className="agents-tree-node-meta">{file.fileName}</span>
      </div>
      <Handle type="source" position={Position.Bottom} className="agents-tree-handle" />
    </div>
  )
}

export const ConfigurationFileNode = memo(ConfigurationFileNodeComponent)
