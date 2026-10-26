/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { useCallback, useRef, useState } from 'react'
import { getFilesFromDataTransfer } from '../../lib/fileDrop'

export interface DocumentsUploadDropzoneProps {
  files: File[]
  busy: boolean
  onFilesChange: (files: File[]) => void
}

export function DocumentsUploadDropzone({ files, busy, onFilesChange }: DocumentsUploadDropzoneProps) {
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [dragOver, setDragOver] = useState(false)

  const handleDrop = useCallback(
    async (e: React.DragEvent) => {
      e.preventDefault()
      setDragOver(false)
      try {
        const list = await getFilesFromDataTransfer(e.dataTransfer)
        if (list.length > 0) onFilesChange(list)
      } catch {
        /* ignore */
      }
    },
    [onFilesChange]
  )

  return (
    <div
      className={`documents-upload-dropzone documents-modal-dropzone ${dragOver ? 'drag-over' : ''} ${files.length > 0 ? 'has-file' : ''}`}
      onDrop={handleDrop}
      onDragOver={(e) => {
        e.preventDefault()
        setDragOver(true)
      }}
      onDragLeave={(e) => {
        e.preventDefault()
        setDragOver(false)
      }}
    >
      <input
        ref={fileInputRef}
        type="file"
        className="documents-upload-file-input-hidden"
        multiple
        onChange={(e) => {
          const list = e.target.files
          if (list) onFilesChange(Array.from(list))
          e.target.value = ''
        }}
        accept=".pdf,.txt,.md,.doc,.docx"
        aria-label="Select files"
      />
      {files.length === 0 ? (
        <div className="documents-upload-dropzone-label-wrap">
          <span className="documents-upload-dropzone-label">Drag &amp; drop files here</span>
          <div className="documents-upload-dropzone-buttons">
            <button
              type="button"
              className="documents-upload-browse-btn"
              onClick={() => fileInputRef.current?.click()}
            >
              Browse Files
            </button>
          </div>
        </div>
      ) : (
        <div className="documents-upload-file-list">
          <div className="documents-upload-file-list-header">
            <span>{files.length} file(s) selected</span>
            <button
              type="button"
              className="documents-upload-clear-files"
              onClick={() => onFilesChange([])}
              disabled={busy}
            >
              Clear all
            </button>
          </div>
          <ul className="documents-upload-file-ul">
            {files.slice(0, 30).map((file, i) => (
              <li key={`${file.name}-${i}`} className="documents-upload-file-li">
                <span className="documents-upload-file-name">{file.name}</span>
                <span className="documents-upload-file-size">{(file.size / 1024).toFixed(1)} KB</span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}
