/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { describe, expect, it } from 'vitest'
import {
  arePortSchemasCompatible,
  arePortsCompatible,
  canonicalizePortSchema,
  portCardinalityLabel,
  portTooltipText,
  portTooltipLines,
} from './portConnection'

describe('portConnection', () => {
  it('accepts matching primitives regardless of case', () => {
    expect(arePortSchemasCompatible('string', 'string')).toBe(true)
    expect(arePortSchemasCompatible('String', 'string')).toBe(true)
    expect(canonicalizePortSchema('String')).toBe('string')
  })

  it('rejects primitive mismatches without coercion', () => {
    expect(arePortSchemasCompatible('string', 'number')).toBe(false)
    expect(arePortSchemasCompatible('number', 'string')).toBe(false)
  })

  it('accepts wildcard ports', () => {
    expect(arePortSchemasCompatible('string', 'any')).toBe(true)
    expect(arePortSchemasCompatible('any', 'number')).toBe(true)
  })

  it('preserves domain type casing', () => {
    expect(arePortSchemasCompatible('Stock[]', 'Stock[]')).toBe(true)
    expect(arePortSchemasCompatible('Stock[]', 'stock[]')).toBe(false)
  })

  it('uses acceptType on inputs for compatibility', () => {
    expect(
      arePortsCompatible(
        { type: 'string', schema: 'any' },
        { acceptType: 'string', type: 'any' },
      ),
    ).toBe(true)
    expect(
      arePortsCompatible(
        { type: 'number' },
        { acceptType: 'string' },
      ),
    ).toBe(false)
    expect(
      arePortsCompatible(
        { type: 'string' },
        { acceptType: 'any' },
      ),
    ).toBe(true)
  })

  it('requires matching message wire type on both ends', () => {
    expect(
      arePortsCompatible(
        { type: 'message', schema: 'message' },
        { type: 'message', acceptType: 'message', schema: 'message' },
      ),
    ).toBe(true)
    expect(
      arePortsCompatible(
        { type: 'message', schema: 'message' },
        { type: 'any', acceptType: 'any', schema: 'any' },
      ),
    ).toBe(false)
  })

  it('formats port tooltips with label, accept type, and cardinality', () => {
    expect(
      portTooltipLines({
        id: 'in',
        label: 'message in',
        shortDescription: 'Incoming workflow message',
        direction: 'INPUT',
        type: 'message',
        acceptType: 'message',
        minConnections: 0,
      }),
    ).toEqual([
      'message in',
      'Incoming workflow message',
      'Accepts: message',
      'Cardinality: 0+',
    ])

    expect(
      portTooltipText({
        id: 'out',
        label: 'message out',
        shortDescription: 'Outgoing workflow message',
        direction: 'OUTPUT',
        type: 'message',
        minConnections: 1,
        maxConnections: 1,
        required: true,
      }),
    ).toBe('message out · Outgoing workflow message · Type: message · Cardinality: 1')
  })

  it('derives cardinality from required and bounds', () => {
    expect(portCardinalityLabel({ minConnections: 0 })).toBe('0+')
    expect(portCardinalityLabel({ minConnections: 2, maxConnections: 4 })).toBe('2-4')
    expect(portCardinalityLabel({ minConnections: 1, maxConnections: 1 })).toBe('1')
  })
})
