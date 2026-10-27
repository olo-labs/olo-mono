/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { useLayoutEffect, useRef, useState } from 'react'
import type { ConfigFileNodeData, ConfigFolderNodeData } from '../../lib/configurationTree'
import { CONFIG_FILE_NODE, CONFIG_FOLDER_NODE } from '../../lib/configurationTree'

export type AgentsContextTarget =
  | { kind: 'file'; fileName: string; label: string }
  | { kind: 'folder'; path: string; label: string }
  | { kind: 'canvas' }

export interface AgentsContextMenuState {
  x: number
  y: number
  target: AgentsContextTarget
}

export interface AgentsFileActions {
  onOpen: (fileName: string) => void
  onEditInBuilder: (fileName: string) => void
  onDebug: (fileName: string) => void
  onCopy: (fileName: string) => Promise<void>
  onRename: (fileName: string) => Promise<void>
  onDelete: (fileName: string) => Promise<void>
  onExport: (fileName: string) => Promise<void>
  onCopyPath: (path: string) => void
}

export interface AgentsCanvasActions {
  onReload: () => Promise<void>
  onImport: () => void
}

export interface AgentsContextMenuProps {
  menu: AgentsContextMenuState
  fileActions: AgentsFileActions
  canvasActions: AgentsCanvasActions
  onClose: () => void
}

function MenuItem({
  label,
  onClick,
  danger,
  disabled,
}: {
  label: string
  onClick?: () => void
  danger?: boolean
  disabled?: boolean
}) {
  return (
    <button
      type="button"
      className={`canvas-context-item${danger ? ' danger' : ''}${disabled ? ' disabled' : ''}`}
      role="menuitem"
      disabled={disabled}
      onClick={() => {
        if (disabled) return
        onClick?.()
      }}
    >
      {label}
    </button>
  )
}

export function AgentsContextMenu({
  menu,
  fileActions,
  canvasActions,
  onClose,
}: AgentsContextMenuProps) {
  const menuRef = useRef<HTMLDivElement>(null)
  const [position, setPosition] = useState({ left: menu.x, top: menu.y })
  useLayoutEffect(() => {
    const fit = () => {
      const bounds = menuRef.current?.getBoundingClientRect()
      if (!bounds) return
      setPosition({
        left: Math.max(8, Math.min(menu.x, window.innerWidth - bounds.width - 8)),
        top: Math.max(8, Math.min(menu.y, window.innerHeight - bounds.height - 8)),
      })
    }
    fit()
    window.addEventListener('resize', fit)
    return () => window.removeEventListener('resize', fit)
  }, [menu])
  const closeAnd = (action?: () => void | Promise<void>) => {
    onClose()
    if (action) void action()
  }

  const target = menu.target

  return (
    <>
      <div
        className="canvas-context-backdrop"
        onClick={onClose}
        onContextMenu={onClose}
        aria-hidden
      />
      <div
        ref={menuRef}
        className="canvas-context-menu"
        style={{ ...position, maxWidth: 'calc(100vw - 16px)', maxHeight: 'calc(100vh - 16px)', overflowY: 'auto' }}
        role="menu"
      >
        {target.kind === 'file' ? (
          <>
            <MenuItem label="Open" onClick={() => closeAnd(() => fileActions.onOpen(target.fileName))} />
            <MenuItem
              label="Edit in Builder"
              onClick={() => closeAnd(() => fileActions.onEditInBuilder(target.fileName))}
            />
            <MenuItem
              label="Debug"
              onClick={() => closeAnd(() => fileActions.onDebug(target.fileName))}
            />
            <MenuItem
              label="Copy path"
              onClick={() => closeAnd(() => fileActions.onCopyPath(target.fileName))}
            />
            <MenuItem
              label="Duplicate"
              onClick={() => closeAnd(() => fileActions.onCopy(target.fileName))}
            />
            <MenuItem
              label="Rename…"
              onClick={() => closeAnd(() => fileActions.onRename(target.fileName))}
            />
            <MenuItem
              label="Export JSON"
              onClick={() => closeAnd(() => fileActions.onExport(target.fileName))}
            />
            <MenuItem
              label="Delete"
              danger
              onClick={() => closeAnd(() => fileActions.onDelete(target.fileName))}
            />
          </>
        ) : null}

        {target.kind === 'folder' ? (
          <>
            <MenuItem
              label="Copy path"
              onClick={() => closeAnd(() => fileActions.onCopyPath(
                target.path ? `${target.path}/` : target.label,
              ))}
            />
            <MenuItem
              label="Reload folder"
              onClick={() => closeAnd(() => canvasActions.onReload())}
            />
          </>
        ) : null}

        {target.kind === 'canvas' ? (
          <>
            <MenuItem label="Reload" onClick={() => closeAnd(() => canvasActions.onReload())} />
            <MenuItem label="Import JSON…" onClick={() => closeAnd(() => canvasActions.onImport())} />
          </>
        ) : null}
      </div>
    </>
  )
}

export function agentsContextTargetFromNode(
  nodeType: string | undefined,
  data: unknown,
): AgentsContextTarget | null {
  if (nodeType === CONFIG_FILE_NODE) {
    const file = data as ConfigFileNodeData
    return { kind: 'file', fileName: file.fileName, label: file.label }
  }
  if (nodeType === CONFIG_FOLDER_NODE) {
    const folder = data as ConfigFolderNodeData
    return { kind: 'folder', path: folder.path, label: folder.label }
  }
  return null
}
