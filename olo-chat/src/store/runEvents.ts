/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { create } from 'zustand'
import type { RunEventDto } from '../api/chatApi'

/** Max workflow run events kept in localStorage and restored after refresh (liveness excluded). */
export const RUN_EVENTS_PERSIST_MAX = 200

const runEventsStorageKey = (runId: string) => `olo:run-events:${runId}`

/** sessionStorage key: last active run id for a chat session (for restore after refresh). */
export function getActiveRunStorageKey(sessionId: string): string {
  return `olo:chat-active-run:${sessionId}`
}

/** True when activeRunId is the run this chat session is tracking (not another session's run). */
export function isActiveRunForSession(
  sessionId: string | null | undefined,
  activeRunId: string | null | undefined,
): boolean {
  const sid = sessionId?.trim()
  const rid = activeRunId?.trim()
  if (!sid || !rid) return false
  try {
    return sessionStorage.getItem(getActiveRunStorageKey(sid))?.trim() === rid
  } catch {
    return false
  }
}

export function isLivenessEvent(e: RunEventDto): boolean {
  return (e.nodeType ?? '').toLowerCase() === 'liveness'
}

/** Workflow events for a single run id (excludes liveness). */
export function eventsForRun(events: RunEventDto[], runId: string | null | undefined): RunEventDto[] {
  const id = runId?.trim()
  if (!id) return []
  return events.filter((e) => (e.runId ?? '').trim() === id && !isLivenessEvent(e))
}

/**
 * Stable id for a workflow run event so we can skip duplicates (e.g. after refresh: hydrate from
 * localStorage then WebSocket replays the same events on SUBSCRIBE_RUN).
 * Liveness / synthetic events return null (not deduped against each other here).
 */
export function workflowEventDedupeKey(ev: RunEventDto): string | null {
  if (isLivenessEvent(ev) || ev.runId === '__liveness__') return null
  const rid = (ev.runId ?? '').trim()
  if (!rid) return null
  const seq = ev.sequenceNumber
  if (seq != null && Number.isFinite(Number(seq))) {
    return `${rid}:seq:${seq}`
  }
  const ts = ev.timestamp ?? 0
  const nid = ev.nodeId ?? ''
  const nt = (ev.nodeType ?? '').toLowerCase()
  const st = (ev.status ?? '').toLowerCase()
  return `${rid}:fb:${nid}:${nt}:${st}:${ts}`
}

function dedupeWorkflowEvents(events: RunEventDto[]): RunEventDto[] {
  const seen = new Set<string>()
  const out: RunEventDto[] = []
  for (const ev of events) {
    const key = workflowEventDedupeKey(ev)
    if (key) {
      if (seen.has(key)) continue
      seen.add(key)
    }
    out.push(ev)
  }
  return out
}

function persistWorkflowEventsForRun(runId: string, events: RunEventDto[]): void {
  if (!runId?.trim()) return
  const meaningful = events.filter((e) => !isLivenessEvent(e))
  const tail = meaningful.slice(-RUN_EVENTS_PERSIST_MAX)
  try {
    localStorage.setItem(runEventsStorageKey(runId.trim()), JSON.stringify(tail))
  } catch {
    /* quota */
  }
}

/** Loads up to {@link RUN_EVENTS_PERSIST_MAX} persisted workflow events for a run (or null). */
export function loadPersistedRunEvents(runId: string): RunEventDto[] | null {
  if (!runId?.trim()) return null
  try {
    const raw = localStorage.getItem(runEventsStorageKey(runId.trim()))
    if (!raw) return null
    const parsed = JSON.parse(raw) as unknown
    return Array.isArray(parsed) ? (parsed as RunEventDto[]) : null
  } catch {
    return null
  }
}

export type OnRunEventCallback = (runId: string, event: RunEventDto) => void

export interface RunEventsState {
  runId: string | null
  events: RunEventDto[]
  setRun: (runId: string | null) => void
  addEvent: (event: RunEventDto) => void
  /** Restore run id + events from localStorage after refresh (does not clear the on-run callback). */
  hydrate: (runId: string, events: RunEventDto[]) => void
  clear: () => void
  /** Called when an event is added (e.g. from WebSocket RUN_EVENT). Use to trigger getRunResponse, listMessages, etc. */
  setOnRunEventCallback: (cb: OnRunEventCallback | null) => void
  /** Called only when a new workflow event is appended (not hydrate, not duplicate, not liveness). UI uses this for the Events bell. */
  setOnWorkflowEventAppended: (cb: (() => void) | null) => void
}

let onRunEventCallback: OnRunEventCallback | null = null
let onWorkflowEventAppended: (() => void) | null = null

export const runEventsStore = create<RunEventsState>((set) => ({
  runId: null,
  events: [],

  /** Updates active run id without clearing accumulated session events. */
  setRun: (runId) => set({ runId }),

  addEvent: (event) => {
    let appended = false
    set((s) => {
      const eventRunId = (event.runId ?? '').trim()
      const activeRunId = (s.runId ?? '').trim()
      if (
        eventRunId &&
        eventRunId !== '__liveness__' &&
        activeRunId &&
        eventRunId !== activeRunId
      ) {
        return s
      }
      const key = workflowEventDedupeKey(event)
      if (key) {
        const already = s.events.some((e) => workflowEventDedupeKey(e) === key)
        if (already) {
          return s
        }
      }
      appended = true
      const next = [...s.events, event]
      const persistRunId = (event.runId ?? '').trim()
      if (persistRunId && persistRunId !== '__liveness__') {
        persistWorkflowEventsForRun(persistRunId, eventsForRun(next, persistRunId))
      }
      return { events: next }
    })
    const wfKey = workflowEventDedupeKey(event)
    if (appended && wfKey && onWorkflowEventAppended) {
      try {
        onWorkflowEventAppended()
      } catch {
        // ignore
      }
    }
    const runId = event.runId
    if (appended && runId && runId !== '__liveness__' && onRunEventCallback) {
      try {
        onRunEventCallback(runId, event)
      } catch {
        // ignore
      }
    }
  },

  hydrate: (runId, events) => {
    const id = runId?.trim()
    if (!id) return
    set({ runId: id, events: dedupeWorkflowEvents(events) })
  },

  clear: () => {
    onRunEventCallback = null
    set({ runId: null, events: [] })
  },

  setOnRunEventCallback: (cb) => {
    onRunEventCallback = cb
  },

  setOnWorkflowEventAppended: (cb) => {
    onWorkflowEventAppended = cb
  },
}))
