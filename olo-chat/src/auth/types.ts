/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Permission & role scaffolding. Even if not used yet, future multi-user access control becomes additive.
 */

export type Role = 'admin' | 'editor' | 'viewer'

export type Permission =
  | 'deleteTenant'
  | 'editTenant'
  | 'createTenant'
  | 'viewTenants'
  | 'manageConfig'
  | 'viewRuns'
  | 'manageRuns'

/** Role -> permissions (scaffold; extend when implementing real auth). */
export const ROLE_PERMISSIONS: Record<Role, Permission[]> = {
  admin: ['deleteTenant', 'editTenant', 'createTenant', 'viewTenants', 'manageConfig', 'viewRuns', 'manageRuns'],
  editor: ['editTenant', 'createTenant', 'viewTenants', 'viewRuns', 'manageRuns'],
  viewer: ['viewTenants', 'viewRuns'],
}
