/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

export interface DocumentsSourceDeleteDialogProps {
  source: string
  deleting: boolean
  onCancel: () => void
  onConfirm: () => void
}

export function DocumentsSourceDeleteDialog({
  source,
  deleting,
  onCancel,
  onConfirm,
}: DocumentsSourceDeleteDialogProps) {
  return (
    <div className="documents-modal-overlay" role="presentation" onClick={deleting ? undefined : onCancel}>
      <div
        className="documents-modal documents-modal--small"
        role="dialog"
        aria-modal="true"
        aria-labelledby="documents-source-delete-title"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 id="documents-source-delete-title" className="documents-modal-title">
          Delete source?
        </h2>
        <p className="documents-modal-text">
          Delete <strong>{source}</strong> and all raw files stored under this source folder? This removes the
          files from the backend shared upload location.
        </p>
        <div className="documents-modal-footer">
          <button type="button" className="documents-modal-btn-secondary" onClick={onCancel} disabled={deleting}>
            Cancel
          </button>
          <button type="button" className="documents-modal-btn-danger" onClick={onConfirm} disabled={deleting}>
            {deleting ? 'Deleting...' : 'Delete Source'}
          </button>
        </div>
      </div>
    </div>
  )
}
