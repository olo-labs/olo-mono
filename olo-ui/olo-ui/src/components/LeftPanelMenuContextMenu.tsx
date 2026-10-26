/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { SectionId } from '../types/layout'

export type MenuContextMenu = { x: number; y: number; targetSectionId: SectionId | null }

export function LeftPanelMenuContextMenu({
  menuContext,
  onClose,
  onCollapseCategory,
  onExpandCategory,
  onCollapseAll,
  onExpandAll,
}: {
  menuContext: MenuContextMenu
  onClose: () => void
  onCollapseCategory: (sectionId: SectionId) => void
  onExpandCategory: (sectionId: SectionId) => void
  onCollapseAll: () => void
  onExpandAll: () => void
}) {
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
                onCollapseCategory(menuContext.targetSectionId!)
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
                onExpandCategory(menuContext.targetSectionId!)
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
