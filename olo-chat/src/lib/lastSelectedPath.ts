/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

const KEY = 'olo-last-selected-path'

export function getLastSelectedPath(): string | null {
  try {
    return localStorage.getItem(KEY)
  } catch {
    return null
  }
}

export function setLastSelectedPath(path: string): void {
  try {
    localStorage.setItem(KEY, path)
  } catch {
    // ignore
  }
}
