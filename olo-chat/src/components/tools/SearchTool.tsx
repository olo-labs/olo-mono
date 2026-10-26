/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Example contextual tool: uses only context.storeContext (owning-store slice).
 * Does not import API or other domain stores.
 */
import type { ToolContext } from '../../config/toolRegistry'

export interface SearchToolProps {
  context: ToolContext
}

export function SearchTool({ context }: SearchToolProps) {
  const { storeContext, sectionId } = context
  return (
    <div className="tools-list-item-inner">
      <span className="tools-list-label">Search</span>
      <span className="tools-list-desc">
        Find in this view
        {sectionId && ` (${sectionId})`}
        {Object.keys(storeContext).length > 0 && ' · context available'}
      </span>
    </div>
  )
}
