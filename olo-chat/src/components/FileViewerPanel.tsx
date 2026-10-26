/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { useEffect, useMemo, useState } from 'react'
import { fetchCapabilitySourceFile } from '../api/documentsUploadApi'
import { documentUploadsStore } from '../store/documentUploadsStore'

function isTextFile(fileName: string, contentType?: string): boolean {
  const lower = fileName.toLowerCase()
  return (
    contentType?.startsWith('text/') ||
    lower.endsWith('.txt') ||
    lower.endsWith('.md') ||
    lower.endsWith('.csv') ||
    lower.endsWith('.json') ||
    lower.endsWith('.xml') ||
    lower.endsWith('.log')
  )
}

function isPdfFile(fileName: string, contentType?: string): boolean {
  return contentType?.includes('pdf') || fileName.toLowerCase().endsWith('.pdf')
}

function isImageFile(fileName: string, contentType?: string): boolean {
  return contentType?.startsWith('image/') || /\.(png|jpe?g|gif|webp|bmp|svg)$/i.test(fileName)
}

export function FileViewerPanel() {
  const selectedFile = documentUploadsStore((s) => s.selectedFile)
  const selectFile = documentUploadsStore((s) => s.selectFile)
  const [objectUrl, setObjectUrl] = useState<string | null>(null)
  const [text, setText] = useState<string | null>(null)
  const [contentType, setContentType] = useState<string | undefined>()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    setText(null)
    setContentType(undefined)
    setError(null)
    setObjectUrl((prev) => {
      if (prev) URL.revokeObjectURL(prev)
      return null
    })

    if (!selectedFile) return
    setLoading(true)
    void fetchCapabilitySourceFile({
      capabilitySource: selectedFile.source,
      fileName: selectedFile.fileName,
    }).then(async (result) => {
      if (cancelled) return
      setLoading(false)
      if (!result.success || !result.blob) {
        setError(result.message ?? 'Unable to load file.')
        return
      }
      const type = result.contentType || result.blob.type
      setContentType(type)
      if (isTextFile(selectedFile.fileName, type)) {
        setText(await result.blob.text())
        return
      }
      setObjectUrl(URL.createObjectURL(result.blob))
    })

    return () => {
      cancelled = true
    }
  }, [selectedFile])

  useEffect(() => {
    return () => {
      if (objectUrl) URL.revokeObjectURL(objectUrl)
    }
  }, [objectUrl])

  const viewerKind = useMemo(() => {
    if (!selectedFile) return 'empty'
    if (isPdfFile(selectedFile.fileName, contentType)) return 'pdf'
    if (isImageFile(selectedFile.fileName, contentType)) return 'image'
    if (text != null) return 'text'
    return 'unsupported'
  }, [contentType, selectedFile, text])

  if (!selectedFile) {
    return (
      <div className="file-viewer-panel">
        <div className="file-viewer-empty">Select View on a raw file to preview it here.</div>
      </div>
    )
  }

  return (
    <div className="file-viewer-panel">
      <div className="file-viewer-header">
        <div className="file-viewer-heading">
          <span className="file-viewer-name" title={selectedFile.fileName}>
            {selectedFile.fileName}
          </span>
          <span className="file-viewer-source" title={selectedFile.source}>
            {selectedFile.source}
          </span>
        </div>
        <button type="button" className="file-viewer-close" onClick={() => selectFile(null)}>
          Close
        </button>
      </div>

      {loading ? <div className="file-viewer-empty">Loading file...</div> : null}
      {error ? <div className="file-viewer-error">{error}</div> : null}
      {!loading && !error && viewerKind === 'text' ? <pre className="file-viewer-text">{text}</pre> : null}
      {!loading && !error && viewerKind === 'pdf' && objectUrl ? (
        <iframe className="file-viewer-frame" src={objectUrl} title={selectedFile.fileName} />
      ) : null}
      {!loading && !error && viewerKind === 'image' && objectUrl ? (
        <div className="file-viewer-image-wrap">
          <img className="file-viewer-image" src={objectUrl} alt={selectedFile.fileName} />
        </div>
      ) : null}
      {!loading && !error && viewerKind === 'unsupported' ? (
        <div className="file-viewer-empty">Preview is available for PDF, image, and text files.</div>
      ) : null}
    </div>
  )
}
