/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import type { MenuContextMenu } from '../../hooks/useLeftPanelCategories'
import type { SectionId } from '../../types/layout'

export interface LeftPanelContextMenuProps {
  menuContext: MenuContextMenu
  onCollapse: (id: SectionId) => void
  onExpand: (id: SectionId) => void
  onCollapseAll: () => void
  onExpandAll: () => void
  onClose: () => void
}

export function LeftPanelContextMenu({
  menuContext,
  onCollapse,
  onExpand,
  onCollapseAll,
  onExpandAll,
  onClose,
}: LeftPanelContextMenuProps) {
  return (
    <>
      <div
        className="left-panel-menu-context-backdrop"
        onClick={onClose}
        onContextMenu={onClose}
        aria-hidden
      />
      <div
        className="left-panel-menu-context-menu"
        style={{ left: menuContext.x, top: menuContext.y }}
        role="menu"
      >
        {menuContext.targetSectionId != null && (
          <>
            <button
              type="button"
              className="left-panel-menu-context-item"
              role="menuitem"
              onClick={() => {
                onCollapse(menuContext.targetSectionId!)
                onClose()
              }}
            >
              Collapse
            </button>
            <button
              type="button"
              className="left-panel-menu-context-item"
              role="menuitem"
              onClick={() => {
                onExpand(menuContext.targetSectionId!)
                onClose()
              }}
            >
              Expand
            </button>
          </>
        )}
        <button
          type="button"
          className="left-panel-menu-context-item"
          role="menuitem"
          onClick={() => {
            onCollapseAll()
            onClose()
          }}
        >
          Collapse all
        </button>
        <button
          type="button"
          className="left-panel-menu-context-item"
          role="menuitem"
          onClick={() => {
            onExpandAll()
            onClose()
          }}
        >
          Expand all
        </button>
      </div>
    </>
  )
}
