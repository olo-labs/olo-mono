/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
/**
 * Persist last selected workflow file for builder dropdown restore on refresh.
 */
const KEY = 'olo:lastWorkflowFileName'

export function getLastWorkflowFileName(): string {
  if (typeof window === 'undefined') return ''
  try {
    return window.localStorage.getItem(KEY) ?? ''
  } catch {
    return ''
  }
}

export function setLastWorkflowFileName(fileName: string): void {
  if (typeof window === 'undefined') return
  try {
    if (fileName) window.localStorage.setItem(KEY, fileName)
    else window.localStorage.removeItem(KEY)
  } catch {
    // ignore
  }
}
