/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
/** Drag-and-drop payload from Components panel to canvas. */

export const CATALOG_DRAG_MIME = 'application/olo-catalog-component'

export type CatalogDragKind = 'NODE' | 'TOOL' | 'HOOK' | 'AGENT'

export interface CatalogDragPayload {
  catalogId: string
  kind: CatalogDragKind
  name?: string
  emoji?: string
}

export function writeCatalogDrag(dataTransfer: DataTransfer, payload: CatalogDragPayload): void {
  const json = JSON.stringify(payload)
  dataTransfer.setData(CATALOG_DRAG_MIME, json)
  dataTransfer.setData('text/plain', payload.name ?? payload.catalogId)
  dataTransfer.effectAllowed = 'copy'
}

export function readCatalogDrag(dataTransfer: DataTransfer): CatalogDragPayload | null {
  const raw = dataTransfer.getData(CATALOG_DRAG_MIME)
  if (!raw) return null
  try {
    const parsed = JSON.parse(raw) as CatalogDragPayload
    if (!parsed.catalogId || !parsed.kind) return null
    return parsed
  } catch {
    return null
  }
}
