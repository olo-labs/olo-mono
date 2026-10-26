/* Copyright (c) 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */

const unsupported = new Set(['tools', 'toolCalls', 'tool_requirements', 'toolCallPlanner'])

/** Fresh workflow contract: reject legacy execution fields before import, display or save. */
export function assertFreshWorkflow(value: unknown): void {
  if (Array.isArray(value)) {
    value.forEach(assertFreshWorkflow)
    return
  }
  if (value !== null && typeof value === 'object') {
    for (const [key, child] of Object.entries(value)) {
      if (unsupported.has(key)) throw new Error(`Unsupported workflow field: ${key}`)
      if (key === 'type' && child === 'TOOL') throw new Error('Unsupported workflow node type: TOOL')
      assertFreshWorkflow(child)
    }
    return
  }
  if (typeof value === 'string' && value.includes('{availableToolsJson}')) {
    throw new Error('Unsupported workflow placeholder: availableToolsJson')
  }
}
