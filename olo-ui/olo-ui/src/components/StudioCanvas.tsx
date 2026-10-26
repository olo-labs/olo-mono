/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { WorkflowCanvas, type WorkflowCanvasMode } from './canvas/WorkflowCanvas'

export interface StudioCanvasProps {
  mode?: WorkflowCanvasMode
}

export function StudioCanvas({ mode = 'builder' }: StudioCanvasProps) {
  const readOnly = mode === 'log'
  return (
    <div className="studio-canvas">
      <WorkflowCanvas readOnly={readOnly} mode={mode} />
    </div>
  )
}
