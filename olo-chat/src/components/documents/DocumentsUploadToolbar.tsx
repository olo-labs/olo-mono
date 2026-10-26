/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

export interface DocumentsUploadToolbarProps {
  mergedSources: string[]
  sourceFilter: string
  searchQuery: string
  onSourceFilterChange: (v: string) => void
  onSearchQueryChange: (v: string) => void
  onUploadClick: () => void
  onRefreshClick: () => void
  onDeleteSourceClick: () => void
  refreshing: boolean
  canDeleteSelectedSource: boolean
  deletingSource: boolean
}

export function DocumentsUploadToolbar({
  mergedSources,
  sourceFilter,
  searchQuery,
  onSourceFilterChange,
  onSearchQueryChange,
  onUploadClick,
  onRefreshClick,
  onDeleteSourceClick,
  refreshing,
  canDeleteSelectedSource,
  deletingSource,
}: DocumentsUploadToolbarProps) {
  return (
    <div className="documents-page-toolbar">
      <button type="button" className="documents-page-btn-primary" onClick={onUploadClick}>
        + Upload Files
      </button>

      <button
        type="button"
        className="documents-page-btn-secondary"
        onClick={onRefreshClick}
        disabled={refreshing}
      >
        {refreshing ? 'Refreshing...' : 'Refresh'}
      </button>

      <label className="documents-page-toolbar-field">
        <span className="documents-page-toolbar-label">Capability source:</span>
        <select
          className="documents-upload-select documents-page-filter-select"
          value={sourceFilter}
          onChange={(e) => onSourceFilterChange(e.target.value)}
          aria-label="Filter by capability source"
        >
          <option value="all">All Sources</option>
          {mergedSources.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
      </label>

      <button
        type="button"
        className="documents-page-btn-danger-inline"
        onClick={onDeleteSourceClick}
        disabled={!canDeleteSelectedSource || deletingSource}
        title={canDeleteSelectedSource ? `Delete source ${sourceFilter}` : 'Select a source to delete'}
      >
        {deletingSource ? 'Deleting...' : 'Delete Source'}
      </button>

      <div className="documents-page-search-wrap">
        <span className="documents-page-search-icon" aria-hidden>
          🔍
        </span>
        <input
          type="search"
          className="documents-page-search-input"
          placeholder="Search files…"
          value={searchQuery}
          onChange={(e) => onSearchQueryChange(e.target.value)}
          aria-label="Search files"
        />
      </div>
    </div>
  )
}
