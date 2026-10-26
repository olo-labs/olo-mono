/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import type { DocumentUploadRow } from '../../store/documentUploadsStore'
import { fileIcon, statusDisplay } from '../../lib/documentsUploadUi'

export interface DocumentsUploadTableProps {
  rows: DocumentUploadRow[]
  reprocessId: string | null
  onDelete: (row: DocumentUploadRow) => void
  onReprocess: (row: DocumentUploadRow) => void
  onView: (row: DocumentUploadRow) => void
}

export function DocumentsUploadTable({
  rows,
  reprocessId,
  onDelete,
  onReprocess,
  onView,
}: DocumentsUploadTableProps) {
  return (
    <div className="documents-page-table-wrap">
      <table className="documents-page-table">
        <thead>
          <tr>
            <th scope="col">File Name</th>
            <th scope="col">Source</th>
            <th scope="col">Status</th>
            <th scope="col">Chunks</th>
            <th scope="col">Actions</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => {
            const st = statusDisplay(row.status)
            return (
              <tr key={row.id}>
                <td>
                  <span className="documents-page-file-cell">
                    <span className="documents-page-file-icon" aria-hidden>
                      {fileIcon(row.fileName)}
                    </span>
                    <span className="documents-page-file-name" title={row.errorMessage}>
                      {row.fileName}
                    </span>
                  </span>
                </td>
                <td>{row.source}</td>
                <td>
                  <span className="documents-page-status">
                    <span aria-hidden>{st.icon}</span> {st.label}
                  </span>
                </td>
                <td>{row.chunks != null ? row.chunks : '-'}</td>
                <td>
                  <div className="documents-page-actions">
                    <button
                      type="button"
                      className="documents-page-action-btn"
                      title="Delete"
                      aria-label={`Delete ${row.fileName}`}
                      onClick={() => onDelete(row)}
                    >
                      X
                    </button>
                    <button
                      type="button"
                      className="documents-page-action-btn"
                      title="Reprocess"
                      aria-label={`Reprocess ${row.fileName}`}
                      disabled={reprocessId === row.id || row.status === 'uploading'}
                      onClick={() => onReprocess(row)}
                    >
                      R
                    </button>
                    <button
                      type="button"
                      className="documents-page-action-btn"
                      title="View"
                      aria-label={`View ${row.fileName}`}
                      onClick={() => onView(row)}
                    >
                      View
                    </button>
                  </div>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
