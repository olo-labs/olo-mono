/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import App from './App'

vi.mock('./api/rest', () => ({
  getHealth: vi.fn().mockResolvedValue({ status: 'UP', service: 'olo-be' }),
  getTenants: vi.fn().mockResolvedValue([]),
}))
vi.mock('./config/features', async (importOriginal) => {
  const actual = await importOriginal<typeof import('./config/features')>()
  return {
    ...actual,
    isFeatureEnabled: vi.fn((id: keyof typeof actual.features) => actual.features[id] ?? true),
  }
})

import { isFeatureEnabled } from './config/features'

describe('App routing integration', () => {
  beforeEach(() => {
    vi.mocked(isFeatureEnabled).mockImplementation((id) => {
      const defaults: Record<string, boolean> = {
        chat: true,
        knowledge: true,
        documents: true,
      }
      return defaults[id as string] ?? true
    })
  })

  it('redirects to default path when section is disabled (e.g. knowledge off)', async () => {
    vi.mocked(isFeatureEnabled).mockImplementation((id) => id !== 'knowledge')
    render(
      <MemoryRouter initialEntries={['/knowledge/sources']}>
        <App />
      </MemoryRouter>
    )
    const heading = await screen.findByRole('heading', { name: /Chat/i }, { timeout: 3000 })
    expect(heading).toBeTruthy()
  })

  it('renders chat deep link', async () => {
    vi.mocked(isFeatureEnabled).mockReturnValue(true)
    render(
      <MemoryRouter initialEntries={['/chat/conversation?tenant=abc']}>
        <App />
      </MemoryRouter>
    )
    const heading = await screen.findByRole('heading', { name: /Chat/i }, { timeout: 3000 })
    expect(heading).toBeTruthy()
  })
})
