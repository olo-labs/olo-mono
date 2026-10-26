/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Olo logo: transparent SVG symbol (two overlapping circles + vertical line).
 * Uses currentColor so it adapts to theme (light on dark, dark on light).
 */
export interface OloLogoProps {
  /** Symbol only (icon) or with "Olo" text. */
  variant?: 'symbol' | 'full'
  /** CSS class for the wrapper. */
  className?: string
  /** Width in pixels; height follows for symbol to keep aspect. */
  size?: number
  /** Accessible label when used as icon. */
  'aria-label'?: string
}

const DEFAULT_SIZE = 24

export function OloLogo({
  variant = 'symbol',
  className = '',
  size = DEFAULT_SIZE,
  'aria-label': ariaLabel,
}: OloLogoProps) {
  const symbol = (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="-1 -1 2 2"
      fill="none"
      stroke="currentColor"
      strokeWidth="0.12"
      strokeLinecap="round"
      aria-hidden={!!ariaLabel}
      style={{ width: size, height: size, flexShrink: 0 }}
    >
      <circle cx="-0.22" cy="0" r="0.5" />
      <circle cx="0.22" cy="0" r="0.5" />
      <line x1="0" y1="-0.52" x2="0" y2="0.52" />
    </svg>
  )

  if (variant === 'full') {
    return (
      <span
        className={className}
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: Math.round(size * 0.25),
        }}
        aria-label={ariaLabel}
      >
        {symbol}
        <span
          style={{
            fontWeight: 700,
            fontSize: size,
            lineHeight: 1,
            letterSpacing: '-0.02em',
          }}
        >
          Olo
        </span>
      </span>
    )
  }

  return (
    <span className={className} aria-label={ariaLabel}>
      {symbol}
    </span>
  )
}
