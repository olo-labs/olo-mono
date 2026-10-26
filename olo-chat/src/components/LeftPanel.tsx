/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { type SectionId } from '../types/layout'
import { useVisibleSections } from '../hooks/useFeature'
import { useUIStore } from '../store/ui'
import { useLeftPanelCategories } from '../hooks/useLeftPanelCategories'
import { LeftPanelContextMenu } from './left-panel/LeftPanelContextMenu'
import { LeftPanelCollapsedBell, LeftPanelFooter } from './left-panel/LeftPanelFooter'
import { LeftPanelMenu } from './left-panel/LeftPanelMenu'

export interface LeftPanelProps {
  expanded: boolean
  onToggle: () => void
  /** User line in footer (from GET /api/ui/context). */
  userLabel: string
  /** Tenant display name for footer line "Tenant: …" (from GET /api/ui/context). */
  tenantLabel: string
  /** Olo backend version (olo.version from GET /api/ui/context). */
  oloVersion: string
  /** olo-chat package version (package.json). */
  chatVersion: string
  sectionId: SectionId | null
  subId: string
  runSelected: boolean
  onSectionSubSelect: (sectionId: SectionId, subId: string) => void
}

export function LeftPanel({
  expanded,
  onToggle,
  userLabel,
  tenantLabel,
  oloVersion,
  chatVersion,
  sectionId,
  subId,
  runSelected,
  onSectionSubSelect,
}: LeftPanelProps) {
  const sections = useVisibleSections()
  const runEventsBellUnread = useUIStore((s) => s.runEventsBellUnread)
  const categories = useLeftPanelCategories(sections.map((s) => s.id))

  return (
    <aside className={`left-panel ${expanded ? 'expanded' : 'collapsed'}`}>
      {expanded && (
        <div className="left-panel-inner">
          <LeftPanelMenu
            sections={sections}
            expandedCategories={categories.expandedCategories}
            sectionId={sectionId}
            subId={subId}
            runSelected={runSelected}
            onToggleCategory={categories.toggleCategory}
            onSectionSubSelect={onSectionSubSelect}
            onContextMenu={(e, target) => categories.handleMenuContextMenu(e, target, expanded)}
          />
          {categories.menuContext && (
            <LeftPanelContextMenu
              menuContext={categories.menuContext}
              onCollapse={categories.collapseCategory}
              onExpand={categories.expandCategory}
              onCollapseAll={categories.collapseAll}
              onExpandAll={categories.expandAll}
              onClose={categories.closeMenuContext}
            />
          )}
          <LeftPanelFooter
            userLabel={userLabel}
            tenantLabel={tenantLabel}
            oloVersion={oloVersion}
            chatVersion={chatVersion}
            runEventsBellUnread={runEventsBellUnread}
            onToggle={onToggle}
          />
        </div>
      )}
      {!expanded && (
        <>
          <div className="left-panel-collapsed-spacer" />
          <button
            type="button"
            className="left-panel-toggle"
            onClick={onToggle}
            title="Expand"
            aria-label="Expand menu"
          >
            <span className="left-panel-collapsed-label">Menu</span>
          </button>
          <div className="left-panel-collapsed-spacer" />
          <LeftPanelCollapsedBell runEventsBellUnread={runEventsBellUnread} />
        </>
      )}
    </aside>
  )
}
