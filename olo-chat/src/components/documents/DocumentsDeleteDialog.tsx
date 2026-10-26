/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import type { DocumentUploadRow } from '../../store/documentUploadsStore'

export interface DocumentsDeleteDialogProps {
  target: DocumentUploadRow
  onCancel: () => void
  onConfirm: () => void
}

export function DocumentsDeleteDialog({ target, onCancel, onConfirm }: DocumentsDeleteDialogProps) {
  return (
    <div className="documents-modal-overlay" role="presentation" onClick={onCancel}>
      <div
        className="documents-modal documents-modal--small"
        role="dialog"
        aria-modal="true"
        aria-labelledby="documents-delete-title"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 id="documents-delete-title" className="documents-modal-title">
          Delete document?
        </h2>
        <p className="documents-modal-text">
          Remove <strong>{target.fileName}</strong> from this list? This does not call the server unless a
          delete API is configured.
        </p>
        <div className="documents-modal-footer">
          <button type="button" className="documents-modal-btn-secondary" onClick={onCancel}>
            Cancel
          </button>
          <button type="button" className="documents-modal-btn-danger" onClick={onConfirm}>
            Delete
          </button>
        </div>
      </div>
    </div>
  )
}
