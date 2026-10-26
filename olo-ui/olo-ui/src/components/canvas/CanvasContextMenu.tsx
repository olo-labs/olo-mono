/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
export type CanvasContextTarget =
  | { kind: 'node'; nodeId: string }
  | { kind: 'edge'; edgeId: string }

export interface CanvasContextMenuState {
  x: number
  y: number
  target: CanvasContextTarget
}

export interface CanvasContextMenuProps {
  menu: CanvasContextMenuState
  readOnly?: boolean
  onClose: () => void
  onOpenProperties?: (nodeId: string) => void
  onDeleteNode?: (nodeId: string) => void
  onDeleteEdge?: (edgeId: string) => void
}

export function CanvasContextMenu({
  menu,
  readOnly,
  onClose,
  onOpenProperties,
  onDeleteNode,
  onDeleteEdge,
}: CanvasContextMenuProps) {
  return (
    <>
      <div
        className="canvas-context-backdrop"
        onClick={onClose}
        onContextMenu={onClose}
        aria-hidden
      />
      <div
        className="canvas-context-menu"
        style={{ left: menu.x, top: menu.y }}
        role="menu"
      >
        {menu.target.kind === 'node' ? (
          <>
            <button
              type="button"
              className="canvas-context-item"
              role="menuitem"
              onClick={() => {
                if (menu.target.kind === 'node') onOpenProperties?.(menu.target.nodeId)
                onClose()
              }}
            >
              Properties
            </button>
            {!readOnly ? (
              <button
                type="button"
                className="canvas-context-item danger"
                role="menuitem"
                onClick={() => {
                  if (menu.target.kind === 'node') onDeleteNode?.(menu.target.nodeId)
                  onClose()
                }}
              >
                Delete node
              </button>
            ) : null}
          </>
        ) : (
          !readOnly ? (
            <button
              type="button"
              className="canvas-context-item danger"
              role="menuitem"
              onClick={() => {
                if (menu.target.kind === 'edge') onDeleteEdge?.(menu.target.edgeId)
                onClose()
              }}
            >
              Delete connection
            </button>
          ) : (
            <span className="canvas-context-item disabled" role="menuitem">
              Connection
            </span>
          )
        )}
      </div>
    </>
  )
}
