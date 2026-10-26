/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { useEffect, useState } from 'react'
import { type SectionId } from '../types/layout'
import { useVisibleSections } from '../hooks/useFeature'
import {
  LeftPanelCategory,
  handleLeftPanelSectionClick,
} from './LeftPanelCategory'
import {
  LeftPanelMenuContextMenu,
  type MenuContextMenu,
} from './LeftPanelMenuContextMenu'

export interface LeftPanelProps {
  expanded: boolean
  onToggle: () => void
  sectionId: SectionId | null
  subId: string
  onSectionSubSelect: (sectionId: SectionId, subId: string) => void
}

export function LeftPanel({
  expanded,
  onToggle,
  sectionId,
  subId,
  onSectionSubSelect,
}: LeftPanelProps) {
  const [expandedCategories, setExpandedCategories] = useState<Set<SectionId>>(new Set(['workflows']))
  const [menuContext, setMenuContext] = useState<MenuContextMenu | null>(null)
  const sections = useVisibleSections()

  useEffect(() => {
    if (sectionId) {
      setExpandedCategories((prev) => new Set(prev).add(sectionId))
    }
  }, [sectionId])

  const toggleCategory = (id: SectionId) => {
    setExpandedCategories((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  const expandCategory = (id: SectionId) => {
    setExpandedCategories((prev) => new Set(prev).add(id))
  }
  const collapseCategory = (id: SectionId) => {
    setExpandedCategories((prev) => {
      const next = new Set(prev)
      next.delete(id)
      return next
    })
  }
  const expandAll = () => {
    setExpandedCategories(new Set(sections.map((s) => s.id)))
  }
  const collapseAll = () => {
    setExpandedCategories(new Set())
  }

  const handleMenuContextMenu = (e: React.MouseEvent, targetSectionId: SectionId | null) => {
    e.preventDefault()
    if (!expanded) return
    setMenuContext({ x: e.clientX, y: e.clientY, targetSectionId })
  }

  return (
    <aside className={`left-panel ${expanded ? 'expanded' : 'collapsed'}`}>
      {expanded && (
        <div className="left-panel-inner">
          <nav
            className="left-panel-menu"
            onContextMenu={(e) => handleMenuContextMenu(e, null)}
          >
            {sections.map((section) => {
              const isCategoryExpanded = expandedCategories.has(section.id)
              const hasSubs = section.subOptions.length > 0
              const isActiveSection =
                sectionId === section.id
                && (!hasSubs || section.subOptions.some((s) => s.id === subId))

              return (
                <LeftPanelCategory
                  key={section.id}
                  section={section}
                  isCategoryExpanded={isCategoryExpanded}
                  isActiveSection={isActiveSection}
                  sectionId={sectionId}
                  subId={subId}
                  onSectionClick={(s) =>
                    handleLeftPanelSectionClick(s, sectionId, toggleCategory, onSectionSubSelect)
                  }
                  onSubSelect={onSectionSubSelect}
                  onContextMenu={handleMenuContextMenu}
                />
              )
            })}
          </nav>
          {menuContext && (
            <LeftPanelMenuContextMenu
              menuContext={menuContext}
              onClose={() => setMenuContext(null)}
              onCollapseCategory={collapseCategory}
              onExpandCategory={expandCategory}
              onCollapseAll={collapseAll}
              onExpandAll={expandAll}
            />
          )}
        </div>
      )}
      <button
        type="button"
        className="left-panel-toggle"
        onClick={onToggle}
        title={expanded ? 'Collapse' : 'Expand'}
        aria-label={expanded ? 'Collapse menu' : 'Expand menu'}
      >
        {expanded ? '<' : <span className="left-panel-collapsed-label">Menu</span>}
      </button>
    </aside>
  )
}
