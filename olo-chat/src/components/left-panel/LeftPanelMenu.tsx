/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import type { SectionConfig, SectionId } from '../../types/layout'
import { isFeatureEnabled } from '../../config/features'

export interface LeftPanelMenuProps {
  sections: SectionConfig[]
  expandedCategories: Set<SectionId>
  sectionId: SectionId | null
  subId: string
  runSelected: boolean
  onToggleCategory: (id: SectionId) => void
  onSectionSubSelect: (sectionId: SectionId, subId: string) => void
  onContextMenu: (e: React.MouseEvent, targetSectionId: SectionId | null) => void
}

function getSubOptions(section: SectionConfig, runSelected: boolean) {
  if (section.id === 'chat') {
    return section.subOptions ?? []
  }
  const list =
    runSelected && section.runSelectedOptions?.length
      ? section.runSelectedOptions
      : section.subOptions
  return list.filter(
    (sub) => !sub.featureId || isFeatureEnabled(sub.featureId as import('../../config/features').FeatureId)
  )
}

export function LeftPanelMenu({
  sections,
  expandedCategories,
  sectionId,
  subId,
  runSelected,
  onToggleCategory,
  onSectionSubSelect,
  onContextMenu,
}: LeftPanelMenuProps) {
  return (
    <nav className="left-panel-menu" onContextMenu={(e) => onContextMenu(e, null)}>
      {sections.map((section) => {
        const isCategoryExpanded = expandedCategories.has(section.id)
        const subOptions = getSubOptions(section, runSelected)
        const hasSubs = subOptions.length > 0
        const categoryTooltip = `${section.label}: ${section.subtitle}`

        return (
          <div key={section.id} className="left-panel-category">
            <button
              type="button"
              className={`left-panel-category-header ${isCategoryExpanded ? 'expanded' : ''} ${sectionId === section.id && (!hasSubs || getSubOptions(section, runSelected).some((s) => s.id === subId)) ? 'active' : ''}`}
              onClick={() => (hasSubs ? onToggleCategory(section.id) : onSectionSubSelect(section.id, ''))}
              onContextMenu={(e) => onContextMenu(e, section.id)}
              aria-expanded={isCategoryExpanded}
              title={categoryTooltip}
            >
              <span className="left-panel-category-chevron">
                {hasSubs ? (isCategoryExpanded ? '▼' : '▶') : ''}
              </span>
              <span className="left-panel-category-label">{section.label}</span>
              <span className="left-panel-category-subtitle">{section.subtitle}</span>
            </button>
            {hasSubs && isCategoryExpanded && (
              <ul className="left-panel-sub-list">
                {subOptions.map((sub) => (
                  <li key={sub.id}>
                    <button
                      type="button"
                      className={`left-panel-sub-item ${sectionId === section.id && subId === sub.id ? 'active' : ''}`}
                      onClick={() => onSectionSubSelect(section.id, sub.id)}
                      title={sub.description ?? sub.label}
                    >
                      {sub.label}
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>
        )
      })}
    </nav>
  )
}
