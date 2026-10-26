/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { describe, expect, it } from 'vitest'
import {
  generatePlannerPrompt,
  PLANNER_MACRO_AGENTS,
  PLANNER_MACRO_CAPABILITIES,
  readPlannerContext,
  togglePlannerAgent,
  togglePlannerTool,
  updatePlannerContext,
} from './plannerContext'
import type { WorkflowDocument } from '../types/workflow'
import type { CatalogComponentBase } from '../types/catalog'

const catalogTools: CatalogComponentBase[] = [
  {
    id: 'olo-core:web-search',
    kind: 'TOOL',
    name: 'Web Search',
    description: 'Search the web',
  },
]

const emptyContextDoc: WorkflowDocument = {
  id: 'agent',
  nodes: [],
  metadata: {
    plannerContext: {
      selectedVariables: ['message'],
      injectCapabilities: true,
      injectAgents: true,
      selectedAgents: [],
      selectedTools: [],
    },
  },
}

describe('plannerContext', () => {
  it('reads planner exposure settings from metadata', () => {
    const doc: WorkflowDocument = {
      ...emptyContextDoc,
      metadata: {
        plannerContext: {
          selectedVariables: ['message'],
          selectedTools: ['olo-core:web-search'],
          selectedAgents: ['planner'],
          injectCapabilities: false,
          injectAgents: false,
        },
      },
    }

    expect(readPlannerContext(doc)).toMatchObject({
      selectedVariables: ['message'],
      selectedTools: ['olo-core:web-search'],
      selectedAgents: ['planner'],
      injectCapabilities: false,
      injectAgents: false,
    })
  })

  it('expands planner macros in generated prompt text', () => {
    const context = {
      ...readPlannerContext(emptyContextDoc),
      injectCapabilities: false,
      injectAgents: false,
      selectedTools: ['olo-core:web-search'],
      selectedAgents: ['planner'],
    }
    const prompt = `Task:\nInvestigate {message}\n\n{${PLANNER_MACRO_CAPABILITIES}}\n\n{${PLANNER_MACRO_AGENTS}}`
    const text = generatePlannerPrompt(prompt, context, catalogTools, [
      { id: 'planner', label: 'Planner', description: 'Plans work' },
    ])

    expect(text).toContain('Web Search')
    expect(text).not.toContain(`{${PLANNER_MACRO_CAPABILITIES}}`)
    expect(text).toContain('Planner')
    expect(text).not.toContain(`{${PLANNER_MACRO_AGENTS}}`)
  })

  it('appends tool and agent blocks when inject checkboxes are enabled', () => {
    const context = {
      ...readPlannerContext(emptyContextDoc),
      injectCapabilities: true,
      injectAgents: true,
      selectedTools: ['olo-core:web-search'],
      selectedAgents: ['planner'],
    }
    const text = generatePlannerPrompt('You are helpful.', context, catalogTools, [
      { id: 'planner', label: 'Planner', description: 'Plans work' },
    ])

    expect(text).toContain('## Available tools')
    expect(text).toContain('Web Search')
    expect(text).toContain('## Available agents')
    expect(text).toContain('Planner')
  })

  it('appends injected blocks in addition to template macros', () => {
    const context = {
      ...readPlannerContext(emptyContextDoc),
      injectCapabilities: true,
      injectAgents: false,
      selectedTools: ['olo-core:web-search'],
      selectedAgents: [],
    }
    const prompt = `Custom tools section:\n{${PLANNER_MACRO_CAPABILITIES}}`
    const text = generatePlannerPrompt(prompt, context, catalogTools, [])

    expect(text).toContain('Custom tools section:')
    expect(text).toContain('Web Search')
    expect(text).toContain('## Available tools')
    expect((text.match(/Web Search/g) ?? []).length).toBe(2)
  })

  it('toggles planner tools and agents', () => {
    let doc = togglePlannerTool(emptyContextDoc, 'olo-core:web-search', true, catalogTools)
    expect(readPlannerContext(doc).selectedTools).toContain('olo-core:web-search')

    doc = togglePlannerAgent(doc, 'planner', true, catalogTools)
    expect(readPlannerContext(doc).selectedAgents).toContain('planner')
  })

  it('persists planner context only in metadata', () => {
    const doc = updatePlannerContext(
      emptyContextDoc,
      { injectCapabilities: false, selectedTools: ['olo-core:web-search'] },
      catalogTools,
    )

    expect(doc.metadata?.plannerContext).toMatchObject({
      injectCapabilities: false,
      selectedTools: ['olo-core:web-search'],
    })
    expect(doc).not.toHaveProperty('prompts')
    expect(doc).not.toHaveProperty('defaultPromptId')
  })
})
