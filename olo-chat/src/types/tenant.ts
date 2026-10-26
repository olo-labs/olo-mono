/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Tenant type shared by API layer and components.
 * Components import from here only (no import from api/rest).
 */
export interface Tenant {
  id: string
  name: string
  description?: string
  configVersion?: string
}
