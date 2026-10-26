/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { useMemo } from 'react'
import type { Permission, Role } from './types'
import { ROLE_PERMISSIONS } from './types'

/** Current user role. Replace with real auth context later. */
const CURRENT_ROLE: Role = 'admin'

/**
 * Auth hook. For now always allows; replace with real auth when adding multi-user.
 * Use hasPermission('deleteTenant') etc. so UI is ready for enterprise.
 */
export function useAuth(): { role: Role; hasPermission: (permission: Permission) => boolean } {
  return useMemo(() => {
    const permissions = new Set(ROLE_PERMISSIONS[CURRENT_ROLE])
    return {
      role: CURRENT_ROLE,
      hasPermission: (permission: Permission) => permissions.has(permission),
    }
  }, [])
}
