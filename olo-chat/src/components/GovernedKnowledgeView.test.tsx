/* Copyright 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { knowledgeOperation } from '../api/governedKnowledgeApi'
import { GovernedKnowledgeView } from './GovernedKnowledgeView'
vi.mock('../api/governedKnowledgeApi', () => ({ knowledgeOperation: vi.fn() }))
afterEach(() => { cleanup(); vi.resetAllMocks() })

describe('governed knowledge results', () => {
  it('shows source text and provenance, then clears old evidence when access is denied', async () => {
    vi.mocked(knowledgeOperation).mockResolvedValueOnce({ chunks: [{ doc_id: 'incident-runbook', version: 2, chunk_id: 'passage-1', content: 'Independent approval is required before rollback.', resource: 'olo://tenant/kb/demo/doc/incident-runbook' }] })
    render(<GovernedKnowledgeView />)
    fireEvent.change(screen.getByLabelText('Search your knowledge'), { target: { value: 'What approval is required?' } })
    fireEvent.click(screen.getByRole('button', { name: 'Search' }))
    await screen.findByText('Independent approval is required before rollback.')
    expect(screen.getByText('Source 1 · Document version 2')).toBeTruthy()
    expect(screen.getByText('incident-runbook')).toBeTruthy()
    expect(screen.getByText('passage-1')).toBeTruthy()
    vi.mocked(knowledgeOperation).mockRejectedValueOnce(new Error('Knowledge request failed: 403'))
    fireEvent.click(screen.getByRole('button', { name: 'Search' }))
    await waitFor(() => expect(screen.getByRole('alert').textContent).toContain('403'))
    expect(screen.queryByText('Independent approval is required before rollback.')).toBeNull()
  })
})
