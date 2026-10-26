/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { useCallback, useMemo, useRef, useState } from 'react'
import { ReactFlowProvider } from '@xyflow/react'
import type { AgentsCanvasActions, AgentsFileActions } from './AgentsContextMenu'
import type { AgentsContextMenuState } from './AgentsContextMenu'
import { AgentsCanvasToolbar } from './AgentsCanvasToolbar'
import { AgentsCanvasFlow } from './AgentsCanvasFlow'
import type { WorkflowSummary } from '../../types/workflow'

export interface AgentsCanvasProps {
  workflows: WorkflowSummary[]
  configurationRoot: string
  loading: boolean
  error: string | null
  selectedFileName: string | null
  fileActions: AgentsFileActions
  canvasActions: AgentsCanvasActions
  onImportFile: (file: File) => Promise<void>
  exportDisabled: boolean
}

function AgentsCanvasInner({
  workflows,
  configurationRoot,
  loading,
  error,
  selectedFileName,
  fileActions,
  canvasActions,
  onImportFile,
  exportDisabled,
}: AgentsCanvasProps) {
  const [contextMenu, setContextMenu] = useState<AgentsContextMenuState | null>(null)
  const openImportDialogRef = useRef<(() => void) | null>(null)

  const menuCanvasActions = useMemo(
    (): AgentsCanvasActions => ({
      onReload: canvasActions.onReload,
      onImport: () => openImportDialogRef.current?.(),
    }),
    [canvasActions],
  )

  const handleImportDialogReady = useCallback((openImportDialog: () => void) => {
    openImportDialogRef.current = openImportDialog
  }, [])

  const handleImportFile = useCallback(
    async (file: File) => {
      await onImportFile(file)
    },
    [onImportFile],
  )

  return (
    <div className="agents-canvas">
      <AgentsCanvasToolbar
        configurationRoot={configurationRoot}
        loading={loading}
        selectedFileName={selectedFileName}
        exportDisabled={exportDisabled}
        fileActions={fileActions}
        canvasActions={canvasActions}
        onImportFile={handleImportFile}
        onImportDialogReady={handleImportDialogReady}
      />

      {error ? <p className="agents-canvas-error">{error}</p> : null}

      <AgentsCanvasFlow
        workflows={workflows}
        configurationRoot={configurationRoot}
        loading={loading}
        selectedFileName={selectedFileName}
        fileActions={fileActions}
        canvasActions={menuCanvasActions}
        contextMenu={contextMenu}
        onContextMenuChange={setContextMenu}
      />
    </div>
  )
}

export function AgentsCanvas(props: AgentsCanvasProps) {
  return (
    <ReactFlowProvider>
      <AgentsCanvasInner {...props} />
    </ReactFlowProvider>
  )
}
