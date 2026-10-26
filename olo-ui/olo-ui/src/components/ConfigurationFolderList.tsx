/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { configurationFolderStore, refreshConfigurationStack } from '../store/configurationFolderStore'

export function ConfigurationFolderList() {
  const catalogRoot = configurationFolderStore((s) => s.catalogRoot)
  const activeDirectory = configurationFolderStore((s) => s.activeDirectory)
  const activeFolderId = configurationFolderStore((s) => s.activeFolderId)
  const folders = configurationFolderStore((s) => s.folders)
  const loading = configurationFolderStore((s) => s.loading)
  const activatingFolderId = configurationFolderStore((s) => s.activatingFolderId)
  const error = configurationFolderStore((s) => s.error)
  const lastRefreshSteps = configurationFolderStore((s) => s.lastRefreshSteps)
  const loadFolders = configurationFolderStore((s) => s.loadFolders)
  const activateFolder = configurationFolderStore((s) => s.activateFolder)

  const handleRefresh = async () => {
    try {
      await refreshConfigurationStack()
    } catch (e) {
      const message = e instanceof Error ? e.message : 'Refresh failed'
      window.alert(`Stack refresh incomplete:\n\n${message}`)
    }
  }

  return (
    <div className="configuration-folder-list">
      <div className="configuration-folder-list-header">
        <span className="configuration-folder-list-title">Configuration scenarios</span>
        <button
          type="button"
          className="tenant-config-btn small"
          onClick={() => void loadFolders()}
          disabled={loading || activatingFolderId !== null}
        >
          Reload list
        </button>
      </div>

      <p className="configuration-folder-meta">
        Catalog: <code title={catalogRoot}>{catalogRoot || '—'}</code>
      </p>
      <p className="configuration-folder-meta">
        Active: <code title={activeDirectory}>{activeDirectory || '—'}</code>
        {activeFolderId ? (
          <span className="configuration-folder-active-badge">source: {activeFolderId}</span>
        ) : null}
      </p>

      {error ? <p className="configuration-folder-error">{error}</p> : null}

      {loading ? (
        <p className="configuration-folder-message">Loading folders…</p>
      ) : folders.length === 0 ? (
        <p className="configuration-folder-message">No scenario folders found under the catalog root.</p>
      ) : (
        <ul className="configuration-folder-list-ul">
          {folders.map((folder) => {
            const busy = activatingFolderId === folder.id
            return (
              <li key={folder.id} className={`configuration-folder-list-item ${folder.active ? 'active' : ''}`}>
                <div className="configuration-folder-item-main">
                  <span className="configuration-folder-item-id">{folder.id}</span>
                  {folder.active ? (
                    <span className="configuration-folder-active-pill">Active</span>
                  ) : null}
                </div>
                <button
                  type="button"
                  className="tenant-config-btn small"
                  onClick={() => void activateFolder(folder.id)}
                  disabled={folder.active || busy || activatingFolderId !== null}
                  title={
                    folder.active
                      ? 'Already active in current-active'
                      : `Copy ${folder.id} into current-active and refresh worker + studio`
                  }
                >
                  {busy ? 'Activating…' : folder.active ? 'Activated' : 'Activate'}
                </button>
              </li>
            )
          })}
        </ul>
      )}

      <div className="configuration-folder-actions">
        <button
          type="button"
          className="tenant-config-btn"
          onClick={() => void handleRefresh()}
          disabled={loading || activatingFolderId !== null}
        >
          Refresh stack
        </button>
      </div>

      {lastRefreshSteps.length > 0 ? (
        <div className="configuration-folder-refresh-steps">
          <p className="configuration-folder-refresh-title">Last refresh</p>
          <ul>
            {lastRefreshSteps.map((step) => (
              <li key={step}>{step}</li>
            ))}
          </ul>
        </div>
      ) : null}

      <p className="configuration-folder-hint">
        Activate copies every file and subfolder from the selected scenario into{' '}
        <code>current-active</code>, removes the previous active files, then signals olo-worker and reloads
        studio configuration.
      </p>
    </div>
  )
}
