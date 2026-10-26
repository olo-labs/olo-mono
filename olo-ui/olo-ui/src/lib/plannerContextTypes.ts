/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
export const PLANNER_MACRO_CAPABILITIES = 'CAPABILITIES'
export const PLANNER_MACRO_AGENTS = 'AGENTS'
export const PLANNER_MACROS = [PLANNER_MACRO_CAPABILITIES, PLANNER_MACRO_AGENTS] as const

export interface PlannerContextSelection {
  selectedVariables: string[]
  selectedTools: string[]
  selectedAgents: string[]
  injectCapabilities: boolean
  injectAgents: boolean
}

export interface PlannerPromptValidationIssue {
  code: 'missing-parameter' | 'dangling-placeholder' | 'missing-in-prompt'
  message: string
  name?: string
}

export interface AgentPromptInfo {
  id: string
  label: string
  description?: string
}

export const METADATA_KEY = 'plannerContext'
export const PLACEHOLDER_PATTERN = /\{([A-Za-z_][A-Za-z0-9_]*)\}/g

/** @deprecated Legacy bundles — migrated to selectedTools on read. */
export const CAPABILITY_BUNDLES = [
  {
    id: 'research',
    includes: [
      { catalogId: 'olo-core:web-search' },
      { catalogId: 'olo-core:http-tool' },
    ],
  },
  {
    id: 'utility',
    includes: [{ catalogId: 'olo-core:calculator' }],
  },
] as const
