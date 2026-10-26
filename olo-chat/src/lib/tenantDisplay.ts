/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import type { Tenant } from '../types/tenant'

/** Display label for tenant: name if non-empty, else id. Use in components (no import from api/rest). */
export function tenantDisplayName(t: Tenant): string {
  return (t.name != null && t.name.trim() !== '') ? t.name.trim() : t.id
}
