/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { writeCatalogDrag } from '../../lib/canvasDrag'
import type { CatalogComponentBase } from '../../types/catalog'

export function handleNodeDragStart(event: React.DragEvent, item: CatalogComponentBase) {
  writeCatalogDrag(event.dataTransfer, {
    catalogId: item.id,
    kind: 'NODE',
    name: item.name,
    emoji: item.emoji,
  })
}

export function handleToolDragStart(event: React.DragEvent, item: CatalogComponentBase) {
  writeCatalogDrag(event.dataTransfer, {
    catalogId: item.id,
    kind: 'TOOL',
    name: item.name,
    emoji: item.emoji,
  })
}

export function handleHookDragStart(event: React.DragEvent, item: CatalogComponentBase) {
  writeCatalogDrag(event.dataTransfer, {
    catalogId: item.id,
    kind: 'HOOK',
    name: item.name,
    emoji: item.emoji,
  })
}

export function handleAgentDragStart(event: React.DragEvent, agentId: string, label: string) {
  writeCatalogDrag(event.dataTransfer, {
    catalogId: agentId,
    kind: 'AGENT',
    name: label,
    emoji: '🤖',
  })
}

export function BuilderPaletteItem({
  title,
  emoji,
  description,
  disabled,
  active,
  onDragStart,
}: {
  title: string
  emoji?: string
  description?: string
  disabled?: boolean
  active?: boolean
  onDragStart: (event: React.DragEvent) => void
}) {
  return (
    <li
      className={`builder-node-item draggable${active ? ' enabled' : ''}`}
      draggable={!disabled}
      onDragStart={onDragStart}
      title={description}
    >
      <span className="builder-check-emoji" aria-hidden>
        {emoji ?? '▢'}
      </span>
      <span>{title}</span>
    </li>
  )
}
