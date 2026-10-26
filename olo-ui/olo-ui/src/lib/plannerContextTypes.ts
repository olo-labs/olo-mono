/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
export const PLANNER_MACRO_AGENTS = 'availableAgentsJson'
export const PLANNER_MACROS = [PLANNER_MACRO_AGENTS] as const

export interface PlannerContextSelection {
  selectedVariables: string[]
  selectedAgents: string[]
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
