/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  knowledgeSourceTypeLabel,
  listKnowledgeSources,
  type KnowledgeSourceDto,
} from '../api/ragIngestApi'
import {
  knowledgeIngestStore,
  knowledgeStatusLabel,
  type KnowledgeIngestRun,
} from '../store/knowledgeIngestStore'

function sourceStatusLabel(source: KnowledgeSourceDto): string {
  if (source.status === 'success') return 'Success'
  if (source.status === 'failed') return 'Failed'
  if (source.status === 'in_progress') return 'In progress'
  return 'Unknown'
}

export function KnowledgeStatusView() {
  const runs = knowledgeIngestStore((s) => s.runs)
  const [executedSources, setExecutedSources] = useState<KnowledgeSourceDto[]>([])
  const [loadingSources, setLoadingSources] = useState(true)
  const activeRuns = runs.filter((r) => r.status === 'in_progress' || r.status === 'pending')
  const completedRuns = runs.filter((r) => r.status === 'success' || r.status === 'failed')
  const runStatusSignature = useMemo(
    () => runs.map((run) => `${run.runId ?? run.id}:${run.status}:${run.message ?? ''}`).join('|'),
    [runs]
  )

  const refreshSources = useCallback(async () => {
    setLoadingSources(true)
    const list = await listKnowledgeSources()
    setExecutedSources(list)
    setLoadingSources(false)
  }, [])

  useEffect(() => {
    void refreshSources()
  }, [refreshSources, runStatusSignature])

  useEffect(() => {
    const hasActiveBackendSource = executedSources.some((source) => source.status === 'in_progress')
    if (!hasActiveBackendSource && activeRuns.length === 0) return
    const timer = window.setInterval(() => {
      void refreshSources()
    }, 5000)
    return () => window.clearInterval(timer)
  }, [activeRuns.length, executedSources, refreshSources])

  const localFallbackRuns = useMemo(() => {
    const backendRunIds = new Set(executedSources.map((source) => source.runId).filter(Boolean))
    return [...runs]
      .filter((run) => !run.runId || !backendRunIds.has(run.runId))
      .sort((a, b) => b.createdAt - a.createdAt)
  }, [executedSources, runs])

  return (
    <div className="knowledge-view knowledge-view-status">
      <p className="knowledge-view-description">
        Track knowledge creation jobs. Long-running RAG indexing appears as in progress until the
        workflow reports success or failure.
      </p>

      <section className="knowledge-status-section">
        <h3 className="knowledge-runs-title">In progress</h3>
        {activeRuns.length === 0 ? (
          <p className="knowledge-empty">No active knowledge jobs.</p>
        ) : (
          <ul className="knowledge-runs-list">
            {activeRuns.map((run) => (
              <li key={run.id} className={`knowledge-run-row knowledge-run-${run.status}`}>
                <span className="knowledge-run-source">{run.knowledgeName}</span>
                <span className="knowledge-run-type">{knowledgeSourceTypeLabel(run.sourceType)}</span>
                <span className="knowledge-run-status">{knowledgeStatusLabel(run.status)}</span>
                {run.message ? <span className="knowledge-run-message">{run.message}</span> : null}
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="knowledge-status-section">
        <h3 className="knowledge-runs-title">Finished</h3>
        {completedRuns.length === 0 ? (
          <p className="knowledge-empty">No finished knowledge jobs in this session.</p>
        ) : (
          <ul className="knowledge-runs-list">
            {completedRuns.map((run) => (
              <li key={run.id} className={`knowledge-run-row knowledge-run-${run.status}`}>
                <span className="knowledge-run-source">{run.knowledgeName}</span>
                <span className="knowledge-run-type">{knowledgeSourceTypeLabel(run.sourceType)}</span>
                <span className="knowledge-run-status">{knowledgeStatusLabel(run.status)}</span>
                {run.message ? <span className="knowledge-run-message">{run.message}</span> : null}
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="knowledge-status-section">
        <div className="knowledge-files-header">
          <h3 className="knowledge-runs-title">Executed knowledge sources</h3>
          <button type="button" className="knowledge-link-btn" onClick={() => void refreshSources()}>
            Refresh
          </button>
        </div>
        {loadingSources ? (
          <p className="knowledge-empty">Loading executed knowledge sources...</p>
        ) : executedSources.length === 0 && localFallbackRuns.length === 0 ? (
          <p className="knowledge-empty">No knowledge creation jobs have been executed yet.</p>
        ) : (
          <ul className="knowledge-sources-server-list">
            {executedSources.map((source) => (
              <li
                key={source.runId ?? `${source.sourceType}:${source.capabilitySource}`}
                className={`knowledge-source-row knowledge-run-${source.status ?? 'unknown'}`}
              >
                <span className="knowledge-run-source">{source.displayName}</span>
                <span className="knowledge-run-type">{knowledgeSourceTypeLabel(source.sourceType)}</span>
                <span className="knowledge-run-files">{source.fileCount} file(s)</span>
                <span className="knowledge-run-status">{sourceStatusLabel(source)}</span>
                {source.sourceCollection ? (
                  <span className="knowledge-run-message">from {source.sourceCollection}</span>
                ) : source.message ? (
                  <span className="knowledge-run-message">{source.message}</span>
                ) : null}
                {source.runId ? (
                  <span className="knowledge-run-id" title={source.runId}>
                    run: {source.runId.slice(0, 8)}...
                  </span>
                ) : null}
              </li>
            ))}
            {localFallbackRuns.map((run: KnowledgeIngestRun) => (
              <li key={run.id} className={`knowledge-source-row knowledge-run-${run.status}`}>
                <span className="knowledge-run-source">{run.knowledgeName}</span>
                <span className="knowledge-run-type">{knowledgeSourceTypeLabel(run.sourceType)}</span>
                <span className="knowledge-run-files">{run.fileNames.length} file(s)</span>
                <span className="knowledge-run-status">{knowledgeStatusLabel(run.status)}</span>
                <span className="knowledge-run-message">from {run.capabilitySource}</span>
                {run.runId ? (
                  <span className="knowledge-run-id" title={run.runId}>
                    run: {run.runId.slice(0, 8)}...
                  </span>
                ) : null}
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}
