/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { useCallback, useEffect, useState } from 'react'
import type { ChatProfileDto } from '../api/chatApi'
import { conversationPanelStore } from '../store/conversationPanel'
import { formatProfileOptionLabel } from '../lib/chatProfileUi'
import { profileByRunStorageKey } from '../lib/chatProgressStorage'

export function useChatProfileByRun(sessionId: string | null, chatProfiles: ChatProfileDto[]) {
  const [profileByRunId, setProfileByRunId] = useState<Record<string, { profileId: string; label: string }>>({})

  const recordProfileForRun = useCallback(
    (runId: string) => {
      if (!runId?.trim() || chatProfiles.length === 0) return
      const { selectedProfileId: pid } = conversationPanelStore.getState()
      const prof = chatProfiles.find((p) => p.id === pid) ?? chatProfiles[0]
      if (!prof) return
      const idx = chatProfiles.findIndex((p) => p.id === prof.id)
      const label = formatProfileOptionLabel(prof, idx >= 0 ? idx : 0)
      setProfileByRunId((prev) => {
        if (prev[runId]?.profileId === prof.id && prev[runId]?.label === label) return prev
        return { ...prev, [runId]: { profileId: prof.id, label } }
      })
    },
    [chatProfiles]
  )

  useEffect(() => {
    if (!sessionId || chatProfiles.length === 0) {
      setProfileByRunId({})
      return
    }
    try {
      const raw = sessionStorage.getItem(profileByRunStorageKey(sessionId))
      if (raw) {
        const parsed = JSON.parse(raw) as Record<string, { profileId: string; label: string }>
        if (parsed && typeof parsed === 'object') setProfileByRunId(parsed)
      } else {
        setProfileByRunId({})
      }
    } catch {
      setProfileByRunId({})
    }
  }, [sessionId, chatProfiles.length])

  useEffect(() => {
    if (!sessionId || chatProfiles.length === 0) return
    try {
      sessionStorage.setItem(profileByRunStorageKey(sessionId), JSON.stringify(profileByRunId))
    } catch {
      /* quota */
    }
  }, [sessionId, chatProfiles.length, profileByRunId])

  return { profileByRunId, recordProfileForRun }
}
