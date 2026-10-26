/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { useBuilderRunDialog } from '../../hooks/useBuilderRunDialog'
import { BuilderRunDialogContent } from './BuilderRunDialogContent'

export interface BuilderRunDialogProps {
  open: boolean
  initialWorkflowLabel?: string
  initialWorkflowId?: string
  initialTaskQueue?: string
  tenantId: string
  onClose: () => void
}

export function BuilderRunDialog({
  open,
  initialWorkflowLabel = '',
  initialWorkflowId = '',
  initialTaskQueue = '',
  tenantId,
  onClose,
}: BuilderRunDialogProps) {
  const run = useBuilderRunDialog(open, initialWorkflowId, initialTaskQueue, tenantId)
  if (!open) return null
  return (
    <BuilderRunDialogContent
      onClose={onClose}
      initialWorkflowLabel={initialWorkflowLabel}
      run={run}
    />
  )
}
