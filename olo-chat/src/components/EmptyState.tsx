/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

export interface EmptyStateProps {
  /** Message to show when there is no data / no selection */
  message: string
  /** Optional secondary text or hint */
  hint?: string
  className?: string
}

/**
 * Standard empty state: message + optional hint.
 * Use when a list is empty or nothing is selected so empty UX is consistent.
 */
export function EmptyState({ message, hint, className = '' }: EmptyStateProps) {
  return (
    <div className={`empty-state ${className}`.trim()}>
      <p className="empty-state-message">{message}</p>
      {hint && <p className="empty-state-hint">{hint}</p>}
    </div>
  )
}
