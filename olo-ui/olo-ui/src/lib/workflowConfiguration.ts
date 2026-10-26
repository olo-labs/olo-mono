/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { WorkflowDocument, WorkflowSummary } from '../types/workflow'

export function parseWorkflowJson(text: string): WorkflowDocument {
  const parsed = JSON.parse(text) as unknown
  if (parsed == null || typeof parsed !== 'object' || Array.isArray(parsed)) {
    throw new Error('Workflow JSON must be an object')
  }
  const doc = parsed as WorkflowDocument
  if (!doc.id || typeof doc.id !== 'string') {
    throw new Error('Workflow JSON must include string "id"')
  }
  return doc
}

export function workflowFileName(doc: WorkflowDocument): string {
  return `${doc.id}.json`
}

/** Next available duplicate path in the same folder (e.g. default/agent.json → default/agent-copy.json). */
export function copyWorkflowPath(fileName: string, existingFileNames: string[]): string {
  const normalized = fileName.replace(/\\/g, '/')
  const slash = normalized.lastIndexOf('/')
  const dir = slash >= 0 ? normalized.slice(0, slash + 1) : ''
  const base = slash >= 0 ? normalized.slice(slash + 1) : normalized
  const dot = base.lastIndexOf('.json')
  const stem = dot > 0 ? base.slice(0, dot) : base.replace(/\.json$/, '')

  const existing = new Set(existingFileNames.map((name) => name.replace(/\\/g, '/')))
  let candidate = `${dir}${stem}-copy.json`
  let n = 2
  while (existing.has(candidate)) {
    candidate = `${dir}${stem}-copy-${n}.json`
    n += 1
  }
  return candidate
}

export function renameWorkflowPath(fileName: string, newBaseName: string): string {
  const normalized = fileName.replace(/\\/g, '/')
  const slash = normalized.lastIndexOf('/')
  const dir = slash >= 0 ? normalized.slice(0, slash + 1) : ''
  const trimmed = newBaseName.trim().replace(/\\/g, '/')
  const base = trimmed.includes('/') ? trimmed.split('/').pop()! : trimmed
  if (!base || base.includes('..')) {
    throw new Error('Invalid file name')
  }
  return dir + (base.endsWith('.json') ? base : `${base}.json`)
}

export function workflowPathStem(fileName: string): string {
  const base = fileName.replace(/\\/g, '/').split('/').pop() ?? fileName
  return base.endsWith('.json') ? base.slice(0, -5) : base
}

/** Resolve configuration file for a delegate child workflow preset id. */
export function resolveDelegateWorkflowFileName(
  delegateAgentId: string,
  workflows: WorkflowSummary[],
): string | null {
  const id = delegateAgentId.trim()
  if (!id) return null
  const byId = workflows.find((workflow) => workflow.id === id)
  if (byId) return byId.fileName
  const byStem = workflows.find((workflow) => workflowPathStem(workflow.fileName) === id)
  if (byStem) return byStem.fileName
  const candidate = `${id}.json`
  if (workflows.some((workflow) => workflow.fileName.replace(/\\/g, '/') === candidate)) {
    return candidate
  }
  return workflows.length > 0 ? candidate : null
}

export function childWorkflowDisplayTitle(
  delegateAgentId: string,
  nodeLabel: string | undefined,
  workflows: WorkflowSummary[],
): string {
  const id = delegateAgentId.trim()
  const summary = workflows.find((workflow) => workflow.id === id)
    ?? workflows.find((workflow) => workflowPathStem(workflow.fileName) === id)
  return nodeLabel?.trim() || summary?.label?.trim() || id
}

export function duplicateWorkflowDocument(
  document: WorkflowDocument,
  newFileName: string,
): WorkflowDocument {
  const stem = workflowPathStem(newFileName)
  const cloned = structuredClone(document) as WorkflowDocument
  cloned.id = stem
  return cloned
}

export function downloadWorkflowJson(doc: WorkflowDocument, fileName?: string): void {
  const name = fileName ?? workflowFileName(doc)
  const blob = new Blob([JSON.stringify(doc, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = name
  anchor.click()
  URL.revokeObjectURL(url)
}

export async function readWorkflowFile(file: File): Promise<WorkflowDocument> {
  const text = await file.text()
  return parseWorkflowJson(text)
}
