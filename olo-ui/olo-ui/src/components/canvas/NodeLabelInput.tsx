/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { useEffect, useState } from 'react'
import { applyNodeLabel } from '../../lib/workflowGraph'
import { workflowConfigurationStore } from '../../store/workflowConfigurationStore'
import type { WorkflowDocument } from '../../types/workflow'

export interface NodeLabelInputProps {
  nodeId: string
  label?: string
  placeholder: string
  onChange: (workflow: WorkflowDocument) => void
}

/** Local input state so draft/canvas sync does not steal focus on every keystroke. */
export function NodeLabelInput({ nodeId, label, placeholder, onChange }: NodeLabelInputProps) {
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

  return (
    <label className="canvas-node-select-field">
      <span>Display label</span>
      <input
        className="builder-input"
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
      />
    </label>
  )
}
