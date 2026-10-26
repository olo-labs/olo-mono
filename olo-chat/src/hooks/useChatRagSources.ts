/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { useCallback, useEffect, useMemo, useState } from 'react'
import { getConfiguredCapabilitySources } from '../api/documentsUploadApi'
import { listKnowledgeSources } from '../api/ragIngestApi'

export function useChatRagSources() {
  const envSources = useMemo(() => getConfiguredCapabilitySources(), [])
  const [apiSources, setApiSources] = useState<string[]>([])

  const refresh = useCallback(async () => {
    const list = await listKnowledgeSources()
    setApiSources(list.map((s) => s.capabilitySource).filter(Boolean))
  }, [])

  useEffect(() => {
    void refresh()
  }, [refresh])

  const sources = useMemo(
    () => [...new Set([...envSources, ...apiSources])].sort((a, b) => a.localeCompare(b)),
    [envSources, apiSources]
  )

  return { sources, refresh }
}
