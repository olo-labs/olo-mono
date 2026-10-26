/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
/** Versioned API base. Add /v2 etc. when introducing breaking changes. */
export const API_BASE = '/api/v1'

export function encodeWorkflowPath(fileName: string): string {
  return fileName
    .replace(/\\/g, '/')
    .split('/')
    .map((segment) => encodeURIComponent(segment))
    .join('/')
}
