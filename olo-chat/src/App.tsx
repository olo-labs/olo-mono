/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { useState } from 'react'
import { AppShell, useAppEffects } from './components/AppShell'
import { useUIStore } from './store/ui'
import { useWebSocketLiveness } from './hooks/useWebSocketLiveness'
import { useBackendReachable } from './hooks/useBackendReachable'
import { useAppUiContext } from './hooks/useAppUiContext'
import { useAppNavigation } from './hooks/useAppNavigation'

function App() {
  useWebSocketLiveness()
  const backendReachable = useBackendReachable()
  const nav = useAppNavigation()
  const { uiFooter, chatProfiles } = useAppUiContext()
  const [newChatTrigger, setNewChatTrigger] = useState(0)
  const { sectionId, subId, runId } = useUIStore()

  useAppEffects(sectionId, subId, runId)

  return (
    <AppShell
      newChatTrigger={newChatTrigger}
      setNewChatTrigger={setNewChatTrigger}
      uiFooter={uiFooter}
      chatProfiles={chatProfiles}
      backendReachable={backendReachable}
      nav={nav}
    />
  )
}

export default App
