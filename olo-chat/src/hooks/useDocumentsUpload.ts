/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  uploadCapabilitySourceFiles,
  reprocessCapabilitySourceDocument,
  getConfiguredCapabilitySources,
  listResourceUploadSources,
  deleteCapabilitySource,
} from '../api/documentsUploadApi'
import { documentUploadsStore, type DocumentUploadRow } from '../store/documentUploadsStore'
import { useUIStore } from '../store/ui'

export function useDocumentsUpload() {
  const rows = documentUploadsStore((s) => s.rows)
  const customSources = documentUploadsStore((s) => s.customSources)
  const addRows = documentUploadsStore((s) => s.addRows)
  const hydrateRowsFromServer = documentUploadsStore((s) => s.hydrateRowsFromServer)
  const updateRow = documentUploadsStore((s) => s.updateRow)
  const removeRow = documentUploadsStore((s) => s.removeRow)
  const removeRowsBySource = documentUploadsStore((s) => s.removeRowsBySource)
  const selectFile = documentUploadsStore((s) => s.selectFile)
  const addCustomSource = documentUploadsStore((s) => s.addCustomSource)

  const envOptions = useMemo(() => getConfiguredCapabilitySources(), [])

  const [sourceFilter, setSourceFilter] = useState<string>('all')
  const [searchQuery, setSearchQuery] = useState('')
  const [modalOpen, setModalOpen] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState<DocumentUploadRow | null>(null)
  const [sourceDeleteTarget, setSourceDeleteTarget] = useState<string | null>(null)
  const [reprocessId, setReprocessId] = useState<string | null>(null)
  const [refreshingSources, setRefreshingSources] = useState(false)
  const [deletingSource, setDeletingSource] = useState(false)
  const [sourceActionError, setSourceActionError] = useState<string | null>(null)

  const mergedSources = useMemo(() => {
    const fromRows = [...new Set(rows.map((r) => r.source))]
    return [...new Set([...envOptions, ...customSources, ...fromRows])].sort((a, b) => a.localeCompare(b))
  }, [envOptions, customSources, rows])

  const filteredRows = useMemo(() => {
    let list = [...rows]
    if (sourceFilter !== 'all') list = list.filter((r) => r.source === sourceFilter)
    const q = searchQuery.trim().toLowerCase()
    if (q) list = list.filter((r) => r.fileName.toLowerCase().includes(q))
    return list.sort((a, b) => b.createdAt - a.createdAt)
  }, [rows, sourceFilter, searchQuery])

  const defaultModalSource = sourceFilter !== 'all' ? sourceFilter : mergedSources[0] || ''
  const canDeleteSelectedSource = sourceFilter !== 'all' && mergedSources.includes(sourceFilter)

  const refreshUploadedSources = useCallback(async () => {
    setRefreshingSources(true)
    setSourceActionError(null)
    try {
      const sources = await listResourceUploadSources()
      hydrateRowsFromServer(sources)
    } catch (err) {
      setSourceActionError(err instanceof Error ? err.message : 'Failed to refresh source files.')
    } finally {
      setRefreshingSources(false)
    }
  }, [hydrateRowsFromServer])

  useEffect(() => {
    for (const row of rows) {
      if (row.status === 'processing' && !row.runId) {
        updateRow(row.id, {
          status: 'uploaded',
          errorMessage: undefined,
        })
      }
    }
  }, [rows, updateRow])

  useEffect(() => {
    void refreshUploadedSources()
  }, [refreshUploadedSources])

  const onStartUpload = useCallback(
    async (files: File[], source: string) => {
      const capabilitySource = source.trim()
      if (!capabilitySource || files.length === 0) return

      const newRows: DocumentUploadRow[] = files.map((f) => ({
        id: crypto.randomUUID(),
        fileName: f.name,
        source: capabilitySource,
        status: 'uploading',
        createdAt: Date.now(),
      }))
      addRows(newRows)

      for (let i = 0; i < files.length; i++) {
        const file = files[i]
        const row = newRows[i]
        try {
          const result = await uploadCapabilitySourceFiles({ capabilitySource, files: [file] })
          if (!result.success) {
            updateRow(row.id, {
              status: 'failed',
              errorMessage: result.message ?? 'Upload failed',
            })
            continue
          }

          const per = result.files?.find((x) => x.fileName === file.name)
          const runId = per?.runId ?? result.runId ?? result.runIds?.[i] ?? result.runIds?.[0]

          updateRow(row.id, {
            status: runId ? 'processing' : 'uploaded',
            runId,
            errorMessage: undefined,
          })
          void refreshUploadedSources()
        } catch (err) {
          updateRow(row.id, {
            status: 'failed',
            errorMessage: err instanceof Error ? err.message : 'Upload failed',
          })
        }
      }
    },
    [addRows, refreshUploadedSources, updateRow]
  )

  const handleReprocess = useCallback(
    async (row: DocumentUploadRow) => {
      setReprocessId(row.id)
      updateRow(row.id, { status: 'processing', errorMessage: undefined })
      const res = await reprocessCapabilitySourceDocument({
        capabilitySource: row.source,
        fileName: row.fileName,
      })
      if (res.success && res.runId) {
        updateRow(row.id, { status: 'processing', runId: res.runId })
      } else if (res.success) {
        updateRow(row.id, {
          status: 'failed',
          runId: undefined,
          errorMessage: 'Reprocess did not return a run id.',
        })
      } else {
        updateRow(row.id, {
          status: 'failed',
          runId: undefined,
          errorMessage: 'Reprocess failed.',
        })
      }
      setReprocessId(null)
    },
    [updateRow]
  )

  const handleViewFile = useCallback(
    (row: DocumentUploadRow) => {
      selectFile({ source: row.source, fileName: row.fileName })
      useUIStore.getState().setPropertiesPanelExpanded(true)
    },
    [selectFile]
  )

  const handleDeleteSource = useCallback(async () => {
    const source = sourceDeleteTarget?.trim()
    if (!source) return
    setDeletingSource(true)
    setSourceActionError(null)
    try {
      const result = await deleteCapabilitySource(source)
      if (!result.success) {
        setSourceActionError(result.message ?? 'Failed to delete source.')
        return
      }
      removeRowsBySource(source)
      setSourceDeleteTarget(null)
      if (sourceFilter === source) {
        setSourceFilter('all')
      }
      void refreshUploadedSources()
    } finally {
      setDeletingSource(false)
    }
  }, [refreshUploadedSources, removeRowsBySource, sourceDeleteTarget, sourceFilter])

  return {
    rows,
    customSources,
    envOptions,
    sourceFilter,
    setSourceFilter,
    searchQuery,
    setSearchQuery,
    modalOpen,
    setModalOpen,
    deleteTarget,
    setDeleteTarget,
    sourceDeleteTarget,
    setSourceDeleteTarget,
    reprocessId,
    refreshingSources,
    deletingSource,
    sourceActionError,
    mergedSources,
    filteredRows,
    defaultModalSource,
    canDeleteSelectedSource,
    addCustomSource,
    removeRow,
    onStartUpload,
    refreshUploadedSources,
    handleReprocess,
    handleViewFile,
    handleDeleteSource,
  }
}
