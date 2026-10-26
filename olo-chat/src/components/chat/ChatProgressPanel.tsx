/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import type { RunEventDto } from '../../api/chatApi'
import { summarizeMap } from '../../lib/chatEventSummary'

export interface ChatProgressPanelProps {
  expanded: boolean
  height: number
  resizing: boolean
  events: RunEventDto[]
  onExpand: () => void
  onCollapse: () => void
  onResizeStart: (clientY: number) => void
}

export function ChatProgressPanel({
  expanded,
  height,
  resizing,
  events,
  onExpand,
  onCollapse,
  onResizeStart,
}: ChatProgressPanelProps) {
  return (
    <div className={`chat-view-progress-panel ${expanded ? 'is-expanded' : 'is-collapsed'}`}>
      {!expanded && (
        <div className="chat-view-progress-expand-row chat-view-progress-expand-row--collapsed">
          <div className="chat-view-progress-collapsed-track" aria-hidden />
          <button
            type="button"
            className="chat-view-progress-icon-btn chat-view-progress-float-btn"
            onClick={onExpand}
            aria-expanded={false}
            title="Expand progress"
            aria-label="Expand progress"
          >
            ▸
          </button>
        </div>
      )}
      {expanded && (
        <>
          <div className="chat-view-progress-expand-row">
            <div
              className={`chat-view-progress-resizer ${resizing ? 'is-active' : ''}`}
              onPointerDown={(e) => onResizeStart(e.clientY)}
              onMouseDown={(e) => onResizeStart(e.clientY)}
              role="separator"
              aria-orientation="horizontal"
              aria-label="Resize progress panel"
              title="Drag to resize"
            />
            <button
              type="button"
              className="chat-view-progress-icon-btn chat-view-progress-float-btn"
              onClick={(e) => {
                e.stopPropagation()
                onCollapse()
              }}
              aria-expanded
              title="Collapse progress"
              aria-label="Collapse progress"
            >
              ▾
            </button>
          </div>
          <div className="chat-view-progress-body" style={{ height: `${height}px` }}>
            {events.length === 0 ? (
              <div className="chat-view-progress-empty">No worker events yet.</div>
            ) : (
              events.map((ev, i) => {
                const inputSummary = summarizeMap(ev.input)
                const outputSummary = summarizeMap(ev.output)
                return (
                  <div
                    key={`${ev.runId ?? 'run'}-${ev.sequenceNumber ?? i}-${ev.nodeId ?? 'node'}`}
                    className={`chat-view-progress-line chat-view-event-${(ev.nodeType ?? 'system').toLowerCase()}`}
                  >
                    <span className="chat-view-progress-head">
                      #{ev.sequenceNumber ?? i} {ev.nodeType ?? '—'} {ev.status ?? '—'} {ev.nodeId ?? ''}
                    </span>
                    {inputSummary && <span className="chat-view-progress-io">IN: {inputSummary}</span>}
                    {outputSummary && <span className="chat-view-progress-io">OUT: {outputSummary}</span>}
                  </div>
                )
              })
            )}
          </div>
        </>
      )}
    </div>
  )
}
