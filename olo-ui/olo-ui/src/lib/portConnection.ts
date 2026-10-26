/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
/**
 * Studio drag-and-drop port wiring rules — mirrors org.olo.spi.port.PortSchemaCompatibility.
 */

export const PORT_WILDCARDS = ['any', '*'] as const

/** Closed vocabulary for port wire types — keep in sync with {@code org.olo.definition.port.PortWireType}. */
export const PORT_WIRE_TYPES = ['any', 'message', 'capabilities', 'agent-plug'] as const

export type PortWireTypeName = (typeof PORT_WIRE_TYPES)[number]

export const PORT_PRIMITIVES = [
  'string',
  'number',
  'integer',
  'boolean',
  'object',
  'array',
] as const

export interface PortWireLike {
  id?: string
  label?: string
  name?: string
  schema?: string
  type?: string
  acceptType?: string | string[]
  direction?: string
  shortDescription?: string
  required?: boolean
  minConnections?: number
  maxConnections?: number | null
  ui?: {
    position?: string
    color?: string
  }
}

const PRIMITIVE_ALIASES: Record<string, string> = {
  string: 'string',
  str: 'string',
  number: 'number',
  double: 'number',
  float: 'number',
  decimal: 'number',
  integer: 'integer',
  int: 'integer',
  long: 'integer',
  boolean: 'boolean',
  bool: 'boolean',
  object: 'object',
  json: 'object',
  map: 'object',
  array: 'array',
  list: 'array',
}

const TYPE_COLORS: Record<string, string> = {
  any: '#94a3b8',
  message: '#ef4444',
  capabilities: '#22c55e',
  'agent-plug': '#a855f7',
  string: '#a3e635',
  number: '#38bdf8',
  integer: '#38bdf8',
  boolean: '#fb923c',
  object: '#c084fc',
  array: '#f472b6',
}

function acceptsAny(schema: string): boolean {
  const trimmed = schema.trim()
  return trimmed.toLowerCase() === 'any' || trimmed === '*'
}

function primitiveAlias(raw: string): string | null {
  const key = raw.trim().toLowerCase()
  return PRIMITIVE_ALIASES[key] ?? null
}

function canonicalizeElement(element: string): string {
  return primitiveAlias(element) ?? element
}

/** Canonical form used for comparisons. */
export function canonicalizePortSchema(schema: string): string {
  const trimmed = schema.trim()
  if (!trimmed) return trimmed
  if (acceptsAny(trimmed)) return 'any'
  if (trimmed.endsWith('[]')) {
    const element = trimmed.slice(0, -2).trim()
    return `${canonicalizeElement(element)}[]`
  }
  return canonicalizeElement(trimmed)
}

function isKnownWireType(value: string): boolean {
  return (PORT_WIRE_TYPES as readonly string[]).includes(value)
}

function canonicalizeWireType(wireType: string): string {
  const trimmed = wireType.trim().toLowerCase()
  if (!trimmed) return trimmed
  if ((PORT_WIRE_TYPES as readonly string[]).includes(trimmed)) {
    return trimmed
  }
  return canonicalizePortSchema(wireType)
}

export function portDisplayLabel(port: PortWireLike): string {
  return port.label?.trim() || port.name?.trim() || port.id?.trim() || 'port'
}

export function portWireType(port: PortWireLike): string {
  return port.type?.trim() || port.schema?.trim() || 'any'
}

export function portAcceptTypes(port: PortWireLike): string[] {
  const raw = port.acceptType ?? port.type ?? port.schema ?? 'any'
  if (Array.isArray(raw)) {
    return raw.map((value) => value.trim()).filter(Boolean)
  }
  return raw
    .split(',')
    .map((value) => value.trim())
    .filter(Boolean)
}

export function portDisplayColor(
  port: PortWireLike,
  portColors?: Record<string, string>,
): string | undefined {
  const explicit = port.ui?.color?.trim()
  if (explicit) return explicit
  const wireType = canonicalizeWireType(portWireType(port))
  if (portColors?.[wireType]) return portColors[wireType]
  return TYPE_COLORS[wireType]
}

export function portCardinalityLabel(port: PortWireLike): string {
  const min = port.minConnections ?? 0
  const max = port.maxConnections
  if (max != null && max === min) {
    return String(min)
  }
  if (max != null) {
    return `${min}-${max}`
  }
  if (min === 0) {
    return '0+'
  }
  return `${min}+`
}

export function portAcceptTypeLabel(port: PortWireLike): string {
  return portAcceptTypes(port).join(', ')
}

/** Multi-line Studio tooltip for port indicators. */
export function portTooltipLines(port: PortWireLike): string[] {
  const lines = [portDisplayLabel(port)]
  const description = port.shortDescription?.trim()
  if (description) {
    lines.push(description)
  }
  const isInput = port.direction?.toUpperCase() === 'INPUT'
  if (isInput) {
    lines.push(`Accepts: ${portAcceptTypeLabel(port)}`)
  } else {
    lines.push(`Type: ${portWireType(port)}`)
  }
  lines.push(`Cardinality: ${portCardinalityLabel(port)}`)
  return lines
}

/** Single-line fallback (e.g. aria). */
export function portTooltipText(port: PortWireLike): string {
  return portTooltipLines(port).join(' · ')
}

/** Whether an output wire type may connect to an input accept type while editing. */
export function arePortSchemasCompatible(
  outputSchema: string,
  inputSchema: string,
): boolean {
  if (!outputSchema?.trim() || !inputSchema?.trim()) return false
  const output = canonicalizeWireType(outputSchema)
  const input = canonicalizeWireType(inputSchema)
  if (isKnownWireType(output) && isKnownWireType(input)) {
    return output === input
  }
  if (acceptsAny(input) || acceptsAny(output)) return true
  return output === input
}

export function arePortsCompatible(outputPort: PortWireLike, inputPort: PortWireLike): boolean {
  const usesWireContract =
    Boolean(outputPort.type?.trim() || outputPort.acceptType)
    || Boolean(inputPort.type?.trim() || inputPort.acceptType)

  const outputType = portWireType(outputPort)
  const accepted = portAcceptTypes(inputPort)

  if (usesWireContract) {
    return accepted.some((acceptType) => arePortSchemasCompatible(outputType, acceptType))
  }

  if (accepted.length === 0) {
    return arePortSchemasCompatible(outputType, portWireType(inputPort))
  }
  return accepted.some((acceptType) => arePortSchemasCompatible(outputType, acceptType))
}
