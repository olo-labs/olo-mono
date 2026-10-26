/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { OloLogo } from './OloLogo'

export function AppBackendLoading() {
  return (
    <div className="app app-waiting-backend" style={{ cursor: 'wait' }}>
      <div className="app-backend-loading">
        <OloLogo variant="full" size={32} className="app-backend-loading-logo" />
        <div className="app-backend-loading-spinner" />
        <p className="app-backend-loading-text">Waiting for backend…</p>
        <p className="app-backend-loading-hint">Ensure olo-be is running on port 8082.</p>
      </div>
    </div>
  )
}
