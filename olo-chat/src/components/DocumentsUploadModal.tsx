/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { useState, useEffect } from 'react'
import { DocumentsUploadDropzone } from './documents/DocumentsUploadDropzone'
import { DocumentsUploadSourceField } from './documents/DocumentsUploadSourceField'

export interface DocumentsUploadModalProps {
  open: boolean
  onClose: () => void
  envOptions: string[]
  customSources: string[]
  onAddCustomSource: (name: string) => void
  defaultSource: string
  onStartUpload: (files: File[], source: string) => Promise<void>
}

export function DocumentsUploadModal({
  open,
  onClose,
  envOptions,
  customSources,
  onAddCustomSource,
  defaultSource,
  onStartUpload,
}: DocumentsUploadModalProps) {
  const [selectedSource, setSelectedSource] = useState('')
  const [newSourceOpen, setNewSourceOpen] = useState(false)
  const [newSourceName, setNewSourceName] = useState('')
  const [files, setFiles] = useState<File[]>([])
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (open) {
      setSelectedSource(defaultSource || customSources[0] || envOptions[0] || '')
      setFiles([])
      setNewSourceOpen(false)
      setNewSourceName('')
    }
  }, [open, defaultSource, customSources, envOptions])

  const allSources = [...new Set([...envOptions, ...customSources])].sort((a, b) => a.localeCompare(b))
  const effectiveSource = newSourceOpen ? newSourceName.trim() : selectedSource
  const canSubmit = !!effectiveSource && files.length > 0 && !busy

  const handleSubmit = async () => {
    if (!canSubmit) return
    const src = newSourceOpen ? newSourceName.trim() : selectedSource
    if (newSourceOpen && newSourceName.trim()) onAddCustomSource(newSourceName.trim())
    setBusy(true)
    try {
      await onStartUpload(files, src)
      setFiles([])
      onClose()
    } finally {
      setBusy(false)
    }
  }

  if (!open) return null

  return (
    <div className="documents-modal-overlay" role="presentation" onClick={() => !busy && onClose()}>
      <div
        className="documents-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="documents-upload-modal-title"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="documents-modal-header">
          <h2 id="documents-upload-modal-title" className="documents-modal-title">
            Upload Files
          </h2>
          <button type="button" className="documents-modal-close" onClick={() => !busy && onClose()} aria-label="Close">
            ×
          </button>
        </div>
        <div className="documents-modal-body">
          <DocumentsUploadSourceField
            allSources={allSources}
            selectedSource={selectedSource}
            newSourceOpen={newSourceOpen}
            newSourceName={newSourceName}
            busy={busy}
            onSelectChange={(v) => {
              if (v === '__new__') {
                setNewSourceOpen(true)
              } else {
                setNewSourceOpen(false)
                setSelectedSource(v)
              }
            }}
            onNewSourceNameChange={setNewSourceName}
            onCreateSource={() => {
              const n = newSourceName.trim()
              if (!n) return
              onAddCustomSource(n)
              setSelectedSource(n)
              setNewSourceOpen(false)
            }}
          />
          <DocumentsUploadDropzone files={files} busy={busy} onFilesChange={setFiles} />
        </div>
        <div className="documents-modal-footer">
          <button type="button" className="documents-modal-btn-secondary" onClick={() => onClose()} disabled={busy}>
            Cancel
          </button>
          <button type="button" className="documents-modal-btn-primary" onClick={handleSubmit} disabled={!canSubmit}>
            {busy ? 'Uploading…' : 'Upload'}
          </button>
        </div>
      </div>
    </div>
  )
}
