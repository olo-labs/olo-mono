/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

export interface DocumentsUploadSourceFieldProps {
  allSources: string[]
  selectedSource: string
  newSourceOpen: boolean
  newSourceName: string
  busy: boolean
  onSelectChange: (value: string) => void
  onNewSourceNameChange: (value: string) => void
  onCreateSource: () => void
}

export function DocumentsUploadSourceField({
  allSources,
  selectedSource,
  newSourceOpen,
  newSourceName,
  busy,
  onSelectChange,
  onNewSourceNameChange,
  onCreateSource,
}: DocumentsUploadSourceFieldProps) {
  return (
    <div className="documents-modal-field">
      <label htmlFor="documents-upload-modal-source" className="documents-modal-label">
        Capability source
      </label>
      <div className="documents-modal-source-row">
        <select
          id="documents-upload-modal-source"
          className="documents-upload-select documents-modal-select"
          value={newSourceOpen ? '__new__' : selectedSource}
          onChange={(e) => onSelectChange(e.target.value)}
          disabled={busy}
        >
          {allSources.length === 0 && <option value="">— Add a source below —</option>}
          {allSources.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
          <option value="__new__">+ New</option>
        </select>
      </div>
      {newSourceOpen && (
        <div className="documents-modal-new-source">
          <label htmlFor="documents-upload-modal-new-name" className="documents-modal-label">
            New capability source
          </label>
          <div className="documents-modal-new-source-row">
            <input
              id="documents-upload-modal-new-name"
              type="text"
              className="documents-upload-input"
              value={newSourceName}
              onChange={(e) => onNewSourceNameChange(e.target.value)}
              placeholder="e.g. product-docs"
              autoComplete="off"
              disabled={busy}
            />
            <button
              type="button"
              className="documents-modal-create-btn"
              disabled={!newSourceName.trim() || busy}
              onClick={onCreateSource}
            >
              Create
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
