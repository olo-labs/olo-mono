/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

/** REST prefix: direct backend URL when VITE_API_BASE is set, otherwise same-origin /api (Vite or nginx proxy). */
export function getApiPathPrefix(): string {
  const base = import.meta.env.VITE_API_BASE
  if (base && typeof base === 'string' && base.trim()) {
    return `${base.trim().replace(/\/$/, '')}/api`
  }
  return '/api'
}
