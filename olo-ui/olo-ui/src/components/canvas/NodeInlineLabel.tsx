/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { useEffect, useState } from 'react'
import { applyNodeLabel } from '../../lib/workflowGraph'
import { workflowConfigurationStore } from '../../store/workflowConfigurationStore'
import type { WorkflowDocument } from '../../types/workflow'

export function NodeInlineLabel({
  nodeId,
  label,
  placeholder,
  readOnly,
  onChange,
}: {
  nodeId: string
  label?: string
  placeholder: string
  readOnly?: boolean
  onChange: (workflow: WorkflowDocument) => void
}) {
  const [text, setText] = useState(label ?? '')

  useEffect(() => {
    setText(label ?? '')
  }, [nodeId, label])

  const commit = () => {
    const workflow = workflowConfigurationStore.getState().draft
    if (!workflow) return
    const trimmed = text.trim()
    const current = (label ?? '').trim()
    if (trimmed === current) return
    onChange(applyNodeLabel(workflow, nodeId, text))
  }

  if (readOnly) {
    return <span className="catalog-flow-node-label">{label?.trim() || placeholder}</span>
  }

  return (
    <input
      className="catalog-flow-node-label-input nodrag nopan"
      type="text"
      value={text}
      placeholder={placeholder}
      onChange={(e) => setText(e.target.value)}
      onBlur={commit}
      onKeyDown={(e) => {
        if (e.key === 'Enter') {
          e.preventDefault()
          e.currentTarget.blur()
        }
      }}
      onPointerDown={(e) => e.stopPropagation()}
      onMouseDown={(e) => e.stopPropagation()}
    />
  )
}
