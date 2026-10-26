/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Observability: logging abstraction and optional analytics.
 * Use logEvent / logError so we can swap to a real provider later without changing call sites.
 */

export type LogLevel = 'debug' | 'info' | 'warn' | 'error'

/** Set to true to enable debug logs in console */
const DEBUG = typeof import.meta !== 'undefined' && import.meta.env?.DEV

function log(level: LogLevel, message: string, data?: Record<string, unknown>) {
  const payload = data ? `${message} ${JSON.stringify(data)}` : message
  switch (level) {
    case 'debug':
      if (DEBUG) console.debug('[olo]', payload)
      break
    case 'info':
      console.info('[olo]', payload)
      break
    case 'warn':
      console.warn('[olo]', payload)
      break
    case 'error':
      console.error('[olo]', payload)
      break
  }
}

/** User/system events (navigation, actions). Replace with analytics provider later. */
export function logEvent(name: string, props?: Record<string, unknown>): void {
  log('info', `event: ${name}`, props)
  if (typeof window !== 'undefined' && (window as unknown as { __oloAnalytics?: (n: string, p?: Record<string, unknown>) => void }).__oloAnalytics) {
    (window as unknown as { __oloAnalytics: (n: string, p?: Record<string, unknown>) => void }).__oloAnalytics(name, props)
  }
}

/** Errors for debugging user-reported issues. */
export function logError(error: unknown, context?: Record<string, unknown>): void {
  const message = error instanceof Error ? error.message : String(error)
  log('error', message, { stack: error instanceof Error ? error.stack : undefined, ...context })
}

export const observability = {
  debug: (msg: string, data?: Record<string, unknown>) => log('debug', msg, data),
  info: (msg: string, data?: Record<string, unknown>) => log('info', msg, data),
  warn: (msg: string, data?: Record<string, unknown>) => log('warn', msg, data),
  error: logError,
  event: logEvent,
}
