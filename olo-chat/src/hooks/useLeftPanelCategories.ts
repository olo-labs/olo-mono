/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { useState } from 'react'
import type { SectionId } from '../types/layout'

export type MenuContextMenu = { x: number; y: number; targetSectionId: SectionId | null }

export function useLeftPanelCategories(sectionIds: SectionId[]) {
  const [expandedCategories, setExpandedCategories] = useState<Set<SectionId>>(new Set())
  const [menuContext, setMenuContext] = useState<MenuContextMenu | null>(null)

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
    setExpandedCategories(new Set(sectionIds))
  }

  const collapseAll = () => {
    setExpandedCategories(new Set())
  }

  const handleMenuContextMenu = (e: React.MouseEvent, targetSectionId: SectionId | null, expanded: boolean) => {
    e.preventDefault()
    if (!expanded) return
    setMenuContext({ x: e.clientX, y: e.clientY, targetSectionId })
  }

  const closeMenuContext = () => setMenuContext(null)

  return {
    expandedCategories,
    menuContext,
    toggleCategory,
    expandCategory,
    collapseCategory,
    expandAll,
    collapseAll,
    handleMenuContextMenu,
    closeMenuContext,
  }
}
