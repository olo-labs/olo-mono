/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
export const DEFAULT_BUILDER_RUN_PROMPT = 'hi'

export interface BuilderRunPromptPreset {
  id: string
  label: string
  message: string
}

/** One-click test messages for the workflow run dialog. */
export const BUILDER_RUN_PROMPT_PRESETS: BuilderRunPromptPreset[] = [
  { id: 'hi', label: 'Hi', message: 'hi' },
  { id: 'help', label: 'Help', message: 'What can you help me with?' },
  { id: 'plan', label: 'Plan', message: 'Plan a simple three-step task to test this workflow.' },
  { id: 'summarize', label: 'Summarize', message: 'Summarize what this workflow does in two sentences.' },
]
