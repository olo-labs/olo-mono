/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { useUIStore } from '../../store/ui'

function BellButton({ runEventsBellUnread }: { runEventsBellUnread: boolean }) {
  return (
    <button
      type="button"
      className="left-panel-bell-wrap"
      title={
        runEventsBellUnread
          ? 'New run events — click to open Events panel'
          : 'Notifications — click to open Events panel'
      }
      onClick={() => useUIStore.getState().togglePropertiesPanel()}
      aria-label={
        runEventsBellUnread ? 'New run events — open Events panel' : 'Toggle Events panel'
      }
    >
      <span className="left-panel-bell-tooltip" role="tooltip">
        {runEventsBellUnread ? 'New run events — open Events panel' : 'Notifications'}
      </span>
      <span
        className={`left-panel-footer-bell ${runEventsBellUnread ? 'left-panel-footer-bell-has-events' : ''}`}
        aria-hidden
      >
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
          <path d="M13.73 21a2 2 0 0 1-3.46 0" />
        </svg>
      </span>
    </button>
  )
}

export interface LeftPanelFooterProps {
  userLabel: string
  tenantLabel: string
  oloVersion: string
  chatVersion: string
  runEventsBellUnread: boolean
  onToggle: () => void
}

export function LeftPanelFooter({
  userLabel,
  tenantLabel,
  oloVersion,
  chatVersion,
  runEventsBellUnread,
  onToggle,
}: LeftPanelFooterProps) {
  return (
    <>
      <footer className="left-panel-footer">
        <div className="left-panel-footer-top">
          <div className="left-panel-user-block">
            <div className="left-panel-user">
              <span className="left-panel-user-bubble" aria-hidden />
              <span className="left-panel-user-label">{userLabel}</span>
            </div>
            <div className="left-panel-tenant-line" title={`Tenant: ${tenantLabel}`}>
              Tenant: {tenantLabel}
            </div>
          </div>
          <BellButton runEventsBellUnread={runEventsBellUnread} />
        </div>
        <div className="left-panel-version" title="Olo backend (GET /api/ui/context)">
          OLO {oloVersion}
        </div>
        <div className="left-panel-chat-version" title="olo-chat (package.json)">
          olo-chat v{chatVersion}
        </div>
        <div className="left-panel-copyright">© {new Date().getFullYear()} OLO</div>
      </footer>
      <button
        type="button"
        className="left-panel-toggle"
        onClick={onToggle}
        title="Collapse"
        aria-label="Collapse menu"
      >
        {'<'}
      </button>
    </>
  )
}

export function LeftPanelCollapsedBell({ runEventsBellUnread }: { runEventsBellUnread: boolean }) {
  return (
  <div className="left-panel-collapsed-bell">
    <BellButton runEventsBellUnread={runEventsBellUnread} />
  </div>
  )
}
