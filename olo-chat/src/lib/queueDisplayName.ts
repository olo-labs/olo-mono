/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Queue names from the API may include a version suffix (e.g. "olo-chat-queue-oolama:1.0").
 * For display and for taskQueue (API), use only the first part (before ":"); the second part is the version.
 * Decodes %3A so it works when subId comes from the URL still encoded.
 */
export function queueDisplayName(name: string): string {
  if (!name || typeof name !== 'string') return name
  const decoded = tryDecodeUriComponent(name)
  return decoded.includes(':') ? decoded.split(':')[0].trim() || decoded : decoded
}

function tryDecodeUriComponent(s: string): string {
  try {
    return decodeURIComponent(s)
  } catch {
    return s
  }
}
