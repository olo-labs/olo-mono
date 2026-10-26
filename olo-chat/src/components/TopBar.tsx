/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { useNavigate, useLocation } from 'react-router-dom'
import { useUIStore } from '../store/ui'
import { DEFAULT_PATH, buildPathWithQuery, parseQuery, parsedToPanelParams } from '../routes'
import { OloLogo } from './OloLogo'

export function TopBar() {
  const { theme, toggleTheme } = useUIStore()
  const navigate = useNavigate()
  const location = useLocation()

  const goToRoot = () => {
    const q = parseQuery(location.search)
    const params = parsedToPanelParams(q)
    navigate(buildPathWithQuery(DEFAULT_PATH, params))
  }

  return (
    <header className="top-bar">
      <button
        type="button"
        className="top-bar-brand"
        onClick={goToRoot}
        title="Go to home"
        aria-label="Olo – go to home"
      >
        <OloLogo variant="full" size={20} className="top-bar-logo" />
      </button>
      <button
        type="button"
        className="top-bar-theme-toggle"
        onClick={toggleTheme}
        title={theme === 'dark' ? 'Switch to light theme' : 'Switch to dark theme'}
        aria-label={`Theme: ${theme}. Click to switch.`}
      >
        {theme === 'dark' ? '☀' : '☽'}
      </button>
    </header>
  )
}
