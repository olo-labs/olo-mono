/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import {
  type SectionConfig,
  type SectionId,
  getSectionDefaultSubId,
  sectionIsComingSoon,
  subOptionIsComingSoon,
} from '../types/layout'
import { isFeatureEnabled } from '../config/features'

export function LeftPanelCategory({
  section,
  isCategoryExpanded,
  isActiveSection,
  sectionId,
  subId,
  onSectionClick,
  onSubSelect,
  onContextMenu,
}: {
  section: SectionConfig
  isCategoryExpanded: boolean
  isActiveSection: boolean
  sectionId: SectionId | null
  subId: string
  onSectionClick: (section: SectionConfig) => void
  onSubSelect: (sectionId: SectionId, subId: string) => void
  onContextMenu: (event: React.MouseEvent, sectionId: SectionId) => void
}) {
  const subOptions = section.subOptions.filter(
    (sub) => !sub.featureId || isFeatureEnabled(sub.featureId as import('../config/features').FeatureId),
  )
  const hasSubs = subOptions.length > 0
  const comingSoon = sectionIsComingSoon(section)

  return (
    <div className="left-panel-category">
      <button
        type="button"
        className={`left-panel-category-header ${isCategoryExpanded ? 'expanded' : ''} ${isActiveSection ? 'active' : ''}`}
        onClick={() => onSectionClick(section)}
        onContextMenu={(e) => onContextMenu(e, section.id)}
        aria-expanded={hasSubs ? isCategoryExpanded : undefined}
        title={section.subtitle}
      >
        <span className="left-panel-category-chevron">
          {hasSubs ? (isCategoryExpanded ? '▼' : '▶') : ''}
        </span>
        <span className="left-panel-category-emoji" aria-hidden>
          {section.emoji}
        </span>
        <span className="left-panel-category-label">{section.label}</span>
        {comingSoon ? (
          <span className="left-panel-soon-badge">{section.comingSoonLabel ?? 'Scheduled'}</span>
        ) : section.status === 'partial' ? (
          <span className="left-panel-partial-badge">Partial</span>
        ) : null}
      </button>
      {hasSubs && isCategoryExpanded && (
        <ul className="left-panel-sub-list">
          {subOptions.map((sub) => (
            <li key={sub.id}>
              <button
                type="button"
                className={`left-panel-sub-item ${sectionId === section.id && subId === sub.id ? 'active' : ''}`}
                onClick={() => onSubSelect(section.id, sub.id)}
                title={sub.description ?? sub.label}
              >
                {sub.label}
                {subOptionIsComingSoon(sub) ? (
                  <span className="left-panel-soon-badge small">{sub.comingSoonLabel ?? 'Scheduled'}</span>
                ) : null}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

export function getLeftPanelSubOptions(section: SectionConfig) {
  return section.subOptions.filter(
    (sub) => !sub.featureId || isFeatureEnabled(sub.featureId as import('../config/features').FeatureId),
  )
}

export function handleLeftPanelSectionClick(
  section: SectionConfig,
  sectionId: SectionId | null,
  toggleCategory: (id: SectionId) => void,
  onSectionSubSelect: (sectionId: SectionId, subId: string) => void,
) {
  const subs = getLeftPanelSubOptions(section)
  if (subs.length > 0) {
    toggleCategory(section.id)
    if (sectionId !== section.id) {
      onSectionSubSelect(section.id, getSectionDefaultSubId(section.id) || subs[0].id)
    }
    return
  }
  onSectionSubSelect(section.id, '')
}
