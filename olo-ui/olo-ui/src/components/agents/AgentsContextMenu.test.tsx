/* Copyright 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AgentsContextMenu } from './AgentsContextMenu'

afterEach(() => { cleanup(); vi.restoreAllMocks(); vi.unstubAllGlobals() })

describe('Agents context menu viewport bounds', () => {
  it('keeps the full file menu visible near the bottom-right corner and after resizing', () => {
    vi.stubGlobal('innerWidth', 800)
    vi.stubGlobal('innerHeight', 600)
    vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockReturnValue({ width: 180, height: 260 } as DOMRect)
    const onOpen = vi.fn()
    const onClose = vi.fn()
    render(<AgentsContextMenu menu={{ x: 790, y: 590, target: { kind: 'file', fileName: 'incident.json', label: 'Incident' } }}
      fileActions={{ onOpen, onEditInBuilder: vi.fn(), onDebug: vi.fn(), onCopy: vi.fn(), onRename: vi.fn(), onDelete: vi.fn(), onExport: vi.fn(), onCopyPath: vi.fn() }}
      canvasActions={{ onReload: vi.fn(), onImport: vi.fn() }} onClose={onClose} />)
    const menu = screen.getByRole('menu')
    expect(menu.style.left).toBe('612px')
    expect(menu.style.top).toBe('332px')
    vi.stubGlobal('innerWidth', 400)
    vi.stubGlobal('innerHeight', 300)
    fireEvent(window, new Event('resize'))
    expect(menu.style.left).toBe('212px')
    expect(menu.style.top).toBe('32px')
    fireEvent.click(screen.getByRole('menuitem', { name: 'Open' }))
    expect(onOpen).toHaveBeenCalledWith('incident.json')
    expect(onClose).toHaveBeenCalledOnce()
  })
})
