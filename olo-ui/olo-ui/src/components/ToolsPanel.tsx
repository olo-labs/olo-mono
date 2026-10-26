/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
/**
 * Contextual tools panel: shows tools for the current view (section + sub-option).
 * Tools array per sub-option; tool registry map; tool components receive only owning-store context.
 * Main always renders primary output; this panel is supplementary.
 */

import type { SectionId } from '../types/layout'
import { getToolsForView, getToolComponent, type ToolContext } from '../config/toolRegistry'

export interface ToolsPanelProps {
  expanded: boolean
  onToggle: () => void
  sectionId: SectionId | null
  subId: string
  runSelected: boolean
  /** Owning-store slice for the current section (e.g. tenantConfig for configuration). Tools use this only. */
  storeContext?: Record<string, unknown>
}

export function ToolsPanel({
  expanded,
  onToggle,
  sectionId,
  subId,
  runSelected,
  storeContext = {},
}: ToolsPanelProps) {
  const tools = getToolsForView(sectionId, subId, runSelected)
  const context: ToolContext = {
    sectionId,
    subId,
    runSelected,
    storeContext,
  }

  return (
    <aside className={`tools-panel side-panel ${expanded ? 'expanded' : 'collapsed'}`}>
      {expanded && (
        <div className="side-panel-inner">
          <div className="side-panel-title">TOOLS</div>
          <ul className="tools-list">
            {tools.length === 0 ? (
              <li className="tools-list-item tools-list-empty">No tools for this view</li>
            ) : (
              tools.map((t) => {
                const ToolComponent = getToolComponent(t.id)
                return (
                  <li key={t.id} className="tools-list-item">
                    {ToolComponent ? (
                      <ToolComponent context={context} />
                    ) : (
                      <>
                        <span className="tools-list-label">{t.label}</span>
                        {t.description && <span className="tools-list-desc">{t.description}</span>}
                      </>
                    )}
                  </li>
                )
              })
            )}
          </ul>
        </div>
      )}
      <button
        type="button"
        className="side-panel-toggle"
        onClick={onToggle}
        title={expanded ? 'Collapse' : 'Expand'}
        aria-label={expanded ? 'Collapse tools' : 'Expand tools'}
      >
        {expanded ? (
          '<'
        ) : (
          <span className="side-panel-collapsed-label">TOOLS</span>
        )}
      </button>
    </aside>
  )
}
