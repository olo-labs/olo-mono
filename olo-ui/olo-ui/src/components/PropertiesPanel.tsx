/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
export interface PropertiesPanelProps {
  expanded: boolean
  onToggle: () => void
  children?: React.ReactNode
}

export function PropertiesPanel({ expanded, onToggle, children }: PropertiesPanelProps) {
  return (
    <aside className={`properties-panel side-panel ${expanded ? 'expanded' : 'collapsed'}`}>
      {expanded && (
        <div className="side-panel-inner">
          {children != null ? children : (
            <>
              <div className="side-panel-title">PROPERTIES</div>
              <div className="side-panel-placeholder">Properties content</div>
            </>
          )}
        </div>
      )}
      <button
        type="button"
        className="side-panel-toggle"
        onClick={onToggle}
        title={expanded ? 'Collapse' : 'Expand'}
        aria-label={expanded ? 'Collapse properties' : 'Expand properties'}
      >
        {expanded ? (
          '>'
        ) : (
          <span className="side-panel-collapsed-label">PROPERTIES</span>
        )}
      </button>
    </aside>
  )
}
