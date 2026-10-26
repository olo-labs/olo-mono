/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { useCallback, useEffect, useMemo, useState } from 'react'
import { getConfiguredCapabilitySources } from '../api/documentsUploadApi'
import {
  FILES_KNOWLEDGE_SOURCE_TYPE,
  getKnowledgeSourceTypeOptions,
  knowledgeSourceTypeLabel,
  listKnowledgeSourceCollections,
  listKnowledgeSources,
  listUploadedDocuments,
  startRagIngest,
  type KnowledgeSourceDto,
  type UploadedDocumentDto,
} from '../api/ragIngestApi'
import { knowledgeIngestStore, knowledgeStatusLabel } from '../store/knowledgeIngestStore'

function sourceFromId(id: string): KnowledgeSourceDto {
  return {
    capabilitySource: id,
    displayName: id,
    sourceType: FILES_KNOWLEDGE_SOURCE_TYPE,
    fileCount: 0,
  }
}

export function KnowledgeCreateView() {
  const envSources = useMemo(() => getConfiguredCapabilitySources().map(sourceFromId), [])
  const [apiSources, setApiSources] = useState<KnowledgeSourceDto[]>([])
  const [existingKnowledgeSources, setExistingKnowledgeSources] = useState<KnowledgeSourceDto[]>([])
  const [selectedType, setSelectedType] = useState(FILES_KNOWLEDGE_SOURCE_TYPE)
  const [selectedSource, setSelectedSource] = useState('')
  const [knowledgeName, setKnowledgeName] = useState('')
  const [documents, setDocuments] = useState<UploadedDocumentDto[]>([])
  const [selectedFiles, setSelectedFiles] = useState<Set<string>>(new Set())
  const [loadingDocs, setLoadingDocs] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const ingestRuns = knowledgeIngestStore((s) => s.runs)
  const addRun = knowledgeIngestStore((s) => s.addRun)
  const updateRun = knowledgeIngestStore((s) => s.updateRun)

  const sourceOptions = useMemo(() => {
    const map = new Map<string, KnowledgeSourceDto>()
    for (const source of [...envSources, ...apiSources]) {
      map.set(`${source.sourceType}:${source.capabilitySource}`, source)
    }
    for (const run of ingestRuns) {
      map.set(`${run.sourceType}:${run.capabilitySource}`, {
        capabilitySource: run.capabilitySource,
        sourceType: run.sourceType,
        displayName: run.capabilitySource,
        fileCount: run.fileNames.length,
        status: run.status === 'failed' ? 'failed' : run.status === 'success' ? 'success' : 'in_progress',
      })
    }
    return [...map.values()].sort((a, b) =>
      `${a.sourceType}:${a.displayName}`.localeCompare(`${b.sourceType}:${b.displayName}`)
    )
  }, [apiSources, envSources, ingestRuns])

  const typeOptions = useMemo(() => getKnowledgeSourceTypeOptions(sourceOptions), [sourceOptions])
  const filteredSources = useMemo(
    () => sourceOptions.filter((s) => s.sourceType === selectedType),
    [sourceOptions, selectedType]
  )
  const existingKnowledgeNames = useMemo(() => {
    const names = new Set<string>()
    for (const source of existingKnowledgeSources) {
      if (source.displayName.trim()) names.add(source.displayName.trim())
      if (source.capabilitySource.trim()) names.add(source.capabilitySource.trim())
    }
    for (const run of ingestRuns) {
      if (run.knowledgeName.trim()) names.add(run.knowledgeName.trim())
    }
    return [...names].sort((a, b) => a.localeCompare(b))
  }, [existingKnowledgeSources, ingestRuns])
  const effectiveSource = selectedSource.trim()
  const finalKnowledgeName = knowledgeName.trim()

  const refreshSources = useCallback(async () => {
    const [collections, knowledgeSources] = await Promise.all([
      listKnowledgeSourceCollections(),
      listKnowledgeSources(),
    ])
    setApiSources(collections)
    setExistingKnowledgeSources(knowledgeSources)
  }, [])

  useEffect(() => {
    void refreshSources()
  }, [refreshSources])

  useEffect(() => {
    if (!typeOptions.some((type) => type.id === selectedType)) {
      setSelectedType(typeOptions[0]?.id ?? FILES_KNOWLEDGE_SOURCE_TYPE)
    }
  }, [selectedType, typeOptions])

  useEffect(() => {
    if (!filteredSources.some((source) => source.capabilitySource === selectedSource)) {
      const next = filteredSources[0]?.capabilitySource ?? ''
      setSelectedSource(next)
    }
  }, [filteredSources, selectedSource])

  useEffect(() => {
    if (!effectiveSource) {
      setDocuments([])
      setSelectedFiles(new Set())
      return
    }
    let cancelled = false
    setLoadingDocs(true)
    void listUploadedDocuments(effectiveSource).then((files) => {
      if (cancelled) return
      setDocuments(files)
      setSelectedFiles(new Set(files.map((f) => f.fileName)))
      setLoadingDocs(false)
    })
    return () => {
      cancelled = true
    }
  }, [effectiveSource])

  const toggleFile = (fileName: string) => {
    setSelectedFiles((prev) => {
      const next = new Set(prev)
      if (next.has(fileName)) next.delete(fileName)
      else next.add(fileName)
      return next
    })
  }

  const handleStartIngest = async () => {
    setError(null)
    if (!selectedType) {
      setError('Select a knowledge source type.')
      return
    }
    if (!effectiveSource) {
      setError('Select a knowledge source.')
      return
    }
    if (!finalKnowledgeName) {
      setError('Enter a final knowledge source name.')
      return
    }
    const fileNames = [...selectedFiles]
    if (fileNames.length === 0) {
      setError('Select at least one uploaded file to index.')
      return
    }

    const rowId = crypto.randomUUID()
    addRun({
      id: rowId,
      sourceType: selectedType,
      capabilitySource: effectiveSource,
      knowledgeName: finalKnowledgeName,
      fileNames,
      status: 'pending',
      message: 'Waiting to start',
      createdAt: Date.now(),
    })

    setSubmitting(true)
    try {
      const result = await startRagIngest({
        sourceType: selectedType,
        capabilitySource: effectiveSource,
        knowledgeName: finalKnowledgeName,
        fileNames,
      })
      if (!result.success) {
        updateRun(rowId, { status: 'failed', message: result.message ?? 'Ingest failed' })
        setError(result.message ?? 'Ingest failed')
        return
      }
      updateRun(rowId, {
        runId: result.runId,
        status: 'in_progress',
        knowledgeName: result.knowledgeName ?? finalKnowledgeName,
        message: result.runId ? 'Indexing in progress' : 'Indexing request accepted',
      })
      void refreshSources()
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="knowledge-view knowledge-view-create">
      <p className="knowledge-view-description">
        Choose a source type, select a source collection, name the final tokenized knowledge source,
        then pick files from that source folder to index.
      </p>

      <div className="knowledge-form">
        <label className="knowledge-field">
          <span className="knowledge-field-label">Knowledge source type</span>
          <select
            className="knowledge-field-input"
            value={selectedType}
            onChange={(e) => {
              setSelectedType(e.target.value)
              setSelectedSource('')
              setKnowledgeName('')
            }}
          >
            {typeOptions.map((type) => (
              <option key={type.id} value={type.id}>
                {type.label}
              </option>
            ))}
          </select>
        </label>

        <label className="knowledge-field">
          <span className="knowledge-field-label">Knowledge source</span>
          <select
            className="knowledge-field-input"
            value={selectedSource}
            onChange={(e) => {
              setSelectedSource(e.target.value)
            }}
          >
            <option value="">Select a {knowledgeSourceTypeLabel(selectedType).toLowerCase()} source</option>
            {filteredSources.map((source) => (
              <option key={`${source.sourceType}:${source.capabilitySource}`} value={source.capabilitySource}>
                {source.displayName}
              </option>
            ))}
          </select>
        </label>

        <label className="knowledge-field">
          <span className="knowledge-field-label">Final knowledge source name</span>
          <input
            className="knowledge-field-input"
            type="text"
            list="knowledge-name-options"
            placeholder="Enter a new name, e.g. product-docs-rag"
            value={knowledgeName}
            onChange={(e) => setKnowledgeName(e.target.value)}
          />
          <datalist id="knowledge-name-options">
            {existingKnowledgeNames.map((name) => (
              <option key={name} value={name} />
            ))}
          </datalist>
          <span className="knowledge-field-hint">
            Choose an existing name from the dropdown or type a new one. This is separate from the
            selected document collection.
          </span>
        </label>

        <div className="knowledge-files-panel">
          <div className="knowledge-files-header">
            <span className="knowledge-field-label">Files in selected source folder</span>
            {effectiveSource && documents.length > 0 ? (
              <button
                type="button"
                className="knowledge-link-btn"
                onClick={() => {
                  setSelectedFiles(new Set(documents.map((d) => d.fileName)))
                }}
              >
                Select all
              </button>
            ) : null}
          </div>

          {!effectiveSource && <p className="knowledge-empty">Choose a source to list its files.</p>}
          {effectiveSource && loadingDocs && <p className="knowledge-empty">Loading files...</p>}
          {effectiveSource && !loadingDocs && documents.length === 0 && (
            <p className="knowledge-empty">
              No files found for <strong>{effectiveSource}</strong>. Upload documents first under{' '}
              <strong>Documents / Upload</strong>.
            </p>
          )}
          {effectiveSource && !loadingDocs && documents.length > 0 && (
            <ul className="knowledge-file-list">
              {documents.map((doc) => (
                <li key={doc.fileName}>
                  <label className="knowledge-file-item">
                    <input
                      type="checkbox"
                      checked={selectedFiles.has(doc.fileName)}
                      onChange={() => toggleFile(doc.fileName)}
                    />
                    <span className="knowledge-file-name">{doc.fileName}</span>
                    {typeof doc.sizeBytes === 'number' ? (
                      <span className="knowledge-file-meta">
                        {(doc.sizeBytes / 1024).toFixed(1)} KB
                      </span>
                    ) : null}
                  </label>
                </li>
              ))}
            </ul>
          )}
        </div>

        {error ? <p className="knowledge-error">{error}</p> : null}

        <button
          type="button"
          className="knowledge-primary-btn"
          disabled={submitting || !effectiveSource || !finalKnowledgeName || selectedFiles.size === 0}
          onClick={() => void handleStartIngest()}
        >
          {submitting ? 'Starting...' : 'Create knowledge'}
        </button>
      </div>

      {ingestRuns.length > 0 && (
        <div className="knowledge-runs">
          <h3 className="knowledge-runs-title">Recent knowledge jobs</h3>
          <ul className="knowledge-runs-list">
            {ingestRuns.map((run) => (
              <li key={run.id} className={`knowledge-run-row knowledge-run-${run.status}`}>
                <span className="knowledge-run-source">{run.knowledgeName}</span>
                <span className="knowledge-run-type">{knowledgeSourceTypeLabel(run.sourceType)}</span>
                <span className="knowledge-run-files">{run.fileNames.length} file(s)</span>
                <span className="knowledge-run-status">{knowledgeStatusLabel(run.status)}</span>
                {run.message ? <span className="knowledge-run-message">{run.message}</span> : null}
                {run.runId ? (
                  <span className="knowledge-run-id" title={run.runId}>
                    run: {run.runId.slice(0, 8)}...
                  </span>
                ) : null}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}

