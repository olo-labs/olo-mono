/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
/** REST prefix for olo workflow runtime (port 7080). Studio config API stays on /api/v1 via olo-be. */
export function getOloRuntimeApiPrefix(): string {
  const base = import.meta.env.VITE_OLO_RUNTIME_API_BASE
  if (base && typeof base === 'string' && base.trim()) {
    return `${base.trim().replace(/\/$/, '')}`
  }
  return '/runtime-api'
}
