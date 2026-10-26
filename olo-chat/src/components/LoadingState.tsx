/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

export interface LoadingStateProps {
  /** Optional message below the spinner */
  message?: string
  className?: string
}

/**
 * Standard loading state: spinner + optional message.
 * Use for async content (lists, detail views) so loading UX is consistent.
 */
export function LoadingState({ message = 'Loading…', className = '' }: LoadingStateProps) {
  return (
    <div className={`loading-state ${className}`.trim()} role="status" aria-live="polite">
      <div className="loading-state-spinner" aria-hidden />
      {message && <p className="loading-state-message">{message}</p>}
    </div>
  )
}
