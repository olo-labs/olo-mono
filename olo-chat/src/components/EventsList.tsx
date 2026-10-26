/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { useState, useRef, useEffect } from 'react'
import { runEventsStore } from '../store/runEvents'

const RUN_EVENTS_DISPLAY_LIMIT = 25

/** Exclude ping/pong liveness events from run events display. */
function isRunEvent(ev: { nodeType?: string }): boolean {
  return (ev.nodeType ?? '').toLowerCase() !== 'liveness'
}

export function EventsList() {
  const { runId, events } = runEventsStore()
  const [expandedId, setExpandedId] = useState<number | null>(null)
  const listEndRef = useRef<HTMLUListElement>(null)
  const runEventsOnly = events.filter(isRunEvent)
  const displayEvents = runEventsOnly.slice(-RUN_EVENTS_DISPLAY_LIMIT)

  useEffect(() => {
    listEndRef.current?.scrollTo?.({ top: listEndRef.current.scrollHeight, behavior: 'smooth' })
  }, [runEventsOnly.length])

  if (displayEvents.length === 0) {
    return (
      <div className="events-list">
        <div className="events-list-header">Run events</div>
        <div className="events-list-empty">
          {runId ? 'Waiting for events…' : 'Send a message in Chat to see run events here.'}
        </div>
      </div>
    )
  }

  return (
    <div className="events-list">
      <div className="events-list-header">
        Run events {runId && <span className="events-list-run-id">{runId.slice(0, 8)}…</span>}
        {runEventsOnly.length > RUN_EVENTS_DISPLAY_LIMIT && (
          <span className="events-list-header-limit"> (last {RUN_EVENTS_DISPLAY_LIMIT} of {runEventsOnly.length})</span>
        )}
      </div>
      <ul className="events-list-ul" ref={listEndRef}>
        {displayEvents.map((ev, i) => {
          const isExpanded = expandedId === i
          return (
            <li
              key={`${ev.runId ?? 'run'}-${ev.sequenceNumber ?? i}-${ev.nodeId ?? 'node'}`}
              className={`events-list-item events-list-item-${(ev.nodeType ?? '').toLowerCase()}`}
            >
              <button
                type="button"
                className="events-list-item-head"
                onClick={() => setExpandedId(isExpanded ? null : i)}
                aria-expanded={isExpanded}
              >
                <span className="events-list-item-seq">#{ev.sequenceNumber ?? i}</span>
                <span className="events-list-item-type">{ev.nodeType ?? '—'}</span>
                <span className="events-list-item-status">{ev.status ?? '—'}</span>
                <span className="events-list-item-node">{ev.nodeId ?? ''}</span>
              </button>
              {isExpanded && (
                <div className="events-list-item-body">
                  {ev.timestamp != null && (
                    <div className="events-list-item-meta">
                      <span>Timestamp: {new Date(ev.timestamp).toISOString()}</span>
                    </div>
                  )}
                  {ev.input != null && Object.keys(ev.input).length > 0 && (
                    <div className="events-list-item-block">
                      <div className="events-list-item-label">Input</div>
                      <pre className="events-list-item-json">{JSON.stringify(ev.input, null, 2)}</pre>
                    </div>
                  )}
                  {ev.output != null && Object.keys(ev.output).length > 0 && (
                    <div className="events-list-item-block">
                      <div className="events-list-item-label">Output</div>
                      <pre className="events-list-item-json">{JSON.stringify(ev.output, null, 2)}</pre>
                    </div>
                  )}
                  {ev.metadata != null && Object.keys(ev.metadata).length > 0 && (
                    <div className="events-list-item-block">
                      <div className="events-list-item-label">Metadata</div>
                      <pre className="events-list-item-json">{JSON.stringify(ev.metadata, null, 2)}</pre>
                    </div>
                  )}
                </div>
              )}
            </li>
          )
        })}
      </ul>
    </div>
  )
}
