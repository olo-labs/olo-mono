/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { ReactNode } from 'react'
import { workflowConfigurationStore } from '../../store/workflowConfigurationStore'
import {
  childWorkflowDisplayTitle,
  resolveDelegateWorkflowFileName,
} from '../../lib/workflowConfiguration'
import type { WorkflowPort } from '../../types/workflow'

interface ChildWorkflowNodeBodyProps {
  delegateAgentId: string
  nodeLabel?: string
  leftPorts: WorkflowPort[]
  rightPorts: WorkflowPort[]
  readOnly: boolean
  renderPortRow: (port: WorkflowPort, side: 'LEFT' | 'RIGHT', index: number, count: number) => ReactNode
}

export function ChildWorkflowNodeBody({
  delegateAgentId,
  nodeLabel,
  leftPorts,
  rightPorts,
  readOnly,
  renderPortRow,
}: ChildWorkflowNodeBodyProps) {
  const workflows = workflowConfigurationStore((state) => state.workflows)
  const title = childWorkflowDisplayTitle(delegateAgentId, nodeLabel, workflows)
  const targetFile = resolveDelegateWorkflowFileName(delegateAgentId, workflows)

  const openChildWorkflow = async () => {
    if (!targetFile) {
      window.alert(`No workflow configuration found for "${delegateAgentId}".`)
      return
    }
    const { dirty, selectWorkflow } = workflowConfigurationStore.getState()
    if (dirty) {
      const discard = window.confirm(
        'You have unsaved changes. Discard them and open the child workflow?',
      )
      if (!discard) return
    }
    await selectWorkflow(targetFile)
  }

  return (
    <div className="catalog-flow-child-workflow">
      <div className="catalog-flow-child-workflow-title" title={delegateAgentId}>
        {title}
      </div>
      <div className="catalog-flow-child-message-block" aria-label="Message ports (planner routed)">
        <div className="catalog-flow-node-ports catalog-flow-node-ports-compact">
          {leftPorts.length > 0 ? (
            <div className="catalog-flow-node-ports-column catalog-flow-node-ports-column-input">
              {leftPorts.map((port, index) => renderPortRow(port, 'LEFT', index, leftPorts.length))}
            </div>
          ) : null}
          {rightPorts.length > 0 ? (
            <div className="catalog-flow-node-ports-column catalog-flow-node-ports-column-output">
              {rightPorts.map((port, index) => renderPortRow(port, 'RIGHT', index, rightPorts.length))}
            </div>
          ) : null}
        </div>
      </div>
      <button
        type="button"
        className="catalog-flow-child-open-btn"
        onClick={() => void openChildWorkflow()}
        disabled={readOnly || !targetFile}
        title={targetFile ? `Open ${targetFile}` : `Workflow not found for ${delegateAgentId}`}
      >
        Open workflow definition
      </button>
    </div>
  )
}
