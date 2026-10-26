/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { useEffect, useState } from 'react'
import { getUiContext, tenantIdForApiPath, type ChatProfileDto } from '../api/chatApi'
import { useUIStore } from '../store/ui'

const DEFAULT_OLO_VERSION = 'v1.0.0-Dev'

export function useAppUiContext() {
  const setTenantId = useUIStore((s) => s.setTenantId)
  const [uiFooter, setUiFooter] = useState({
    tenant: 'Default',
    user: 'Public',
    oloVersion: DEFAULT_OLO_VERSION,
  })
  const [chatProfiles, setChatProfiles] = useState<ChatProfileDto[]>([])

  useEffect(() => {
    let cancelled = false
    getUiContext().then((ctx) => {
      if (cancelled || !ctx) return
      setTenantId(tenantIdForApiPath(ctx.tenantId))
      setUiFooter({
        tenant: ctx.tenant,
        user: ctx.user,
        oloVersion: ctx.oloVersion?.trim() || DEFAULT_OLO_VERSION,
      })
      setChatProfiles(Array.isArray(ctx.chatProfiles) ? ctx.chatProfiles : [])
    })
    return () => {
      cancelled = true
    }
  }, [setTenantId])

  return { uiFooter, chatProfiles }
}
