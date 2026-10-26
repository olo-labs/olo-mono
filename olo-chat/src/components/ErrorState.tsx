/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

export interface ErrorStateProps {
  /** Error message to show */
  message?: string
  /** Callback when user clicks retry */
  onRetry?: () => void
  className?: string
}

/**
 * Standard error state: message + optional retry button.
 * Use for failed fetches or operations so error UX is consistent.
 */
export function ErrorState({
  message = 'Something went wrong.',
  onRetry,
  className = '',
}: ErrorStateProps) {
  return (
    <div className={`error-state ${className}`.trim()} role="alert">
      <p className="error-state-message">{message}</p>
      {onRetry && (
        <button type="button" className="error-state-retry tenant-config-btn primary" onClick={onRetry}>
          Retry
        </button>
      )}
    </div>
  )
}
