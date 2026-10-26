/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { create } from 'zustand'
import type { RunEventDto } from '../api/chatApi'
import { FILES_KNOWLEDGE_SOURCE_TYPE } from '../api/ragIngestApi'

export type KnowledgeIngestStatus = 'pending' | 'in_progress' | 'success' | 'failed'

export interface KnowledgeIngestRun {
  id: string
  sourceType: string
  capabilitySource: string
  knowledgeName: string
  fileNames: string[]
  runId?: string
  status: KnowledgeIngestStatus
  message?: string
  chunksIndexed?: number
  createdAt: number
}

interface KnowledgeIngestState {
  runs: KnowledgeIngestRun[]
  addRun: (run: KnowledgeIngestRun) => void
  updateRun: (id: string, patch: Partial<KnowledgeIngestRun>) => void
  patchRunFromEvent: (id: string, event: RunEventDto) => void
}

function eventPayload(event: RunEventDto): Record<string, unknown> | undefined {
  return event.output ?? event.metadata ?? event.input
}

function eventText(event: RunEventDto): string {
  const payload = eventPayload(event)
  if (payload && typeof payload.message === 'string') return payload.message
  if (payload && typeof payload.text === 'string') return payload.text
  if (payload && typeof payload.title === 'string') return payload.title
  if (typeof event.status === 'string' && event.status.trim()) return event.status
  return event.eventType ?? ''
}

function upper(value: unknown): string {
  return typeof value === 'string' ? value.trim().toUpperCase() : ''
}

function chunksFromPayload(payload: Record<string, unknown> | undefined): number | undefined {
  if (typeof payload?.chunksIndexed === 'number') return payload.chunksIndexed
  if (payload?.response && typeof payload.response === 'object') {
    const response = payload.response as Record<string, unknown>
    if (typeof response.chunksIndexed === 'number') return response.chunksIndexed
  }
  return undefined
}

function inProgressMessage(event: RunEventDto, fallback?: string): string {
  const nodeType = upper(event.nodeType)
  const status = upper(event.status)
  const text = eventText(event)
  if (nodeType === 'HUMAN' && status === 'WAITING') {
    return text || 'Waiting for human input'
  }
  if (text && text !== status) return text
  if (status === 'WAITING') return 'Waiting'
  if (event.nodeId) return `${event.nodeId}: ${status.toLowerCase()}`
  return fallback || 'In progress'
}

export function knowledgeStatusLabel(status: KnowledgeIngestStatus): string {
  if (status === 'in_progress') return 'in progress'
  return status
}

function normalizeRun(run: KnowledgeIngestRun): KnowledgeIngestRun {
  const status = run.status === ('processing' as KnowledgeIngestStatus) ? 'in_progress' : run.status
  return {
    ...run,
    sourceType: run.sourceType || FILES_KNOWLEDGE_SOURCE_TYPE,
    knowledgeName: run.knowledgeName || run.capabilitySource,
    status,
  }
}

export const knowledgeIngestStore = create<KnowledgeIngestState>((set) => ({
  runs: [],

  addRun: (run) =>
    set((s) => ({
      runs: [normalizeRun(run), ...s.runs].slice(0, 50),
    })),

  updateRun: (id, patch) =>
    set((s) => ({
      runs: s.runs.map((r) => (r.id === id ? normalizeRun({ ...r, ...patch }) : normalizeRun(r))),
    })),

  patchRunFromEvent: (id, event) =>
    set((s) => ({
      runs: s.runs.map((r) => {
        const run = normalizeRun(r)
        if (run.id !== id) return run
        const type = upper(event.eventType)
        const status = upper(event.status)
        const nodeType = upper(event.nodeType)
        const text = eventText(event)
        const payload = eventPayload(event)
        const nextBase =
          run.status === 'pending'
            ? { ...run, status: 'in_progress' as KnowledgeIngestStatus }
            : run

        if (status === 'FAILED' || type.includes('FAILED') || type.includes('ERROR')) {
          return { ...nextBase, status: 'failed', message: text || 'Knowledge creation failed' }
        }

        const terminalSuccess =
          nodeType === 'SYSTEM' &&
          (status === 'COMPLETED' || type.includes('COMPLETED') || type.includes('SUCCESS'))
        if (terminalSuccess || type.includes('FINISHED')) {
          const chunks = chunksFromPayload(payload) ?? nextBase.chunksIndexed
          return {
            ...nextBase,
            status: 'success',
            message: text || 'Indexing complete',
            chunksIndexed: chunks,
          }
        }

        if (
          status === 'STARTED' ||
          status === 'WAITING' ||
          status === 'RUNNING' ||
          status === 'IN_PROGRESS' ||
          type.includes('STARTED') ||
          type.includes('PROGRESS') ||
          type.includes('WAITING')
        ) {
          return {
            ...nextBase,
            status: 'in_progress',
            message: inProgressMessage(event, nextBase.message),
          }
        }

        if (nextBase !== run) {
          return {
            ...nextBase,
            message: inProgressMessage(event, nextBase.message),
          }
        }
        return run
      }),
    })),
}))
