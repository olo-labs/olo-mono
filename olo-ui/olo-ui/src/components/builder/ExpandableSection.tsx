/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { ReactNode } from 'react'

export interface ExpandableSectionProps {
  title: string
  count?: number
  expanded: boolean
  onToggle: () => void
  children: ReactNode
  hint?: string
}

export function ExpandableSection({
  title,
  count,
  expanded,
  onToggle,
  children,
  hint,
}: ExpandableSectionProps) {
  return (
    <section className={`builder-section ${expanded ? 'expanded' : 'collapsed'}`}>
      <button
        type="button"
        className="builder-section-header"
        onClick={onToggle}
        aria-expanded={expanded}
      >
        <span className="builder-section-chevron" aria-hidden>
          {expanded ? '▼' : '▶'}
        </span>
        <span className="builder-section-title">{title}</span>
        {count !== undefined ? <span className="builder-section-count">{count}</span> : null}
      </button>
      {hint && expanded ? <p className="builder-section-hint">{hint}</p> : null}
      {expanded ? <div className="builder-section-body">{children}</div> : null}
    </section>
  )
}
