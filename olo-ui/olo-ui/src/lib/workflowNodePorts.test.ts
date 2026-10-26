/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { describe, expect, it } from 'vitest'

import {
  AGENT_HOST_PORTS,
  AGENT_PLUG_PORT_ID,
  CAPABILITIES_PORT_ID,
  catalogComponentToPorts,
  groupPortsBySide,
  isAgentPlugPort,
  isCapabilitiesPort,
  isDelegateAgentNode,
  isMessagePortId,
  isPlannerRoutedMessagePort,
  MESSAGE_IN_PORT_ID,
  MESSAGE_OUT_PORT_ID,
  resolveAgentHostPorts,
  resolveAgentPluginPorts,
  resolveCapabilityPluginPorts,
  resolveNodePorts,
  usesPlannerRoutedMessagePorts,
} from './workflowNodePorts'

describe('workflowNodePorts', () => {
  it('maps annotation-exported tool ports without UI fallbacks', () => {
    const ports = catalogComponentToPorts({
      id: 'olo-core:web-search',
      kind: 'TOOL',
      inputs: [
        {
          id: 'in',
          schema: 'message',
          type: 'message',
          acceptType: 'message',
          required: true,
        },
      ],
      outputs: [
        { id: 'out', schema: 'message', type: 'message' },
        { id: CAPABILITIES_PORT_ID, schema: 'capabilities', type: 'capabilities' },
      ],
    })

    expect(ports).toHaveLength(3)
    expect(ports.some((port) => port.id === CAPABILITIES_PORT_ID && port.direction === 'OUTPUT')).toBe(true)
    expect(isCapabilitiesPort(CAPABILITIES_PORT_ID)).toBe(true)
    expect(isAgentPlugPort(AGENT_PLUG_PORT_ID)).toBe(true)
  })

  it('returns empty ports when catalog component has no annotation ports', () => {
    expect(catalogComponentToPorts({ id: 'olo-core:calculator', kind: 'TOOL' })).toEqual([])
  })

  it('marks plugin node message ports as planner-routed', () => {
    expect(usesPlannerRoutedMessagePorts('TOOL', {})).toBe(true)
    expect(usesPlannerRoutedMessagePorts('HOOK', {})).toBe(true)
    expect(usesPlannerRoutedMessagePorts('AGENT', { delegateAgentId: 'debug' })).toBe(true)
    expect(usesPlannerRoutedMessagePorts('AGENT', {})).toBe(false)
    expect(isPlannerRoutedMessagePort('TOOL', 'in', {})).toBe(true)
    expect(isPlannerRoutedMessagePort('TOOL', 'capabilities', {})).toBe(false)
    expect(isMessagePortId('out')).toBe(true)
  })

  it('applies canonical AGENT host ports when catalog is stale', () => {
    const merged = resolveAgentHostPorts('AGENT', [], [])
    expect(merged).toHaveLength(AGENT_HOST_PORTS.length)
    expect(merged.some((port) => port.id === CAPABILITIES_PORT_ID && port.ui?.position === 'BOTTOM')).toBe(true)
    expect(merged.some((port) => port.id === AGENT_PLUG_PORT_ID && port.ui?.position === 'BOTTOM')).toBe(true)
    expect(merged.find((port) => port.id === 'in')?.ui?.color).toBe('#ef4444')
  })

  it('normalizes stale agent host plugin inputs onto the bottom edge', () => {
    const merged = resolveAgentHostPorts('AGENT', [
      {
        id: CAPABILITIES_PORT_ID,
        schema: 'capabilities',
        type: 'capabilities',
        direction: 'INPUT',
        ui: { position: 'TOP', color: '#22c55e' },
      },
    ], [])
    expect(merged.find((port) => port.id === CAPABILITIES_PORT_ID)?.ui?.position).toBe('BOTTOM')
  })

  it('groups agent host ports by canvas side', () => {
    const grouped = groupPortsBySide(AGENT_HOST_PORTS)
    expect(grouped.LEFT.map((port) => port.id)).toEqual(['in'])
    expect(grouped.RIGHT.map((port) => port.id)).toEqual(['out'])
    expect(grouped.TOP.map((port) => port.id)).toEqual([])
    expect(grouped.BOTTOM.map((port) => port.id)).toEqual([CAPABILITIES_PORT_ID, AGENT_PLUG_PORT_ID])
  })

  it('places tool plugin output on the top edge', () => {
    const merged = resolveCapabilityPluginPorts('TOOL', [], [])
    expect(merged).toHaveLength(3)
    expect(merged.find((port) => port.id === MESSAGE_IN_PORT_ID)?.ui?.position).toBe('LEFT')
    expect(merged.find((port) => port.id === MESSAGE_OUT_PORT_ID)?.ui?.position).toBe('RIGHT')
    expect(merged.find((port) => port.id === CAPABILITIES_PORT_ID)?.ui?.position).toBe('TOP')
  })

  it('places delegate child-workflow message ports and plugin output', () => {
    const merged = resolveAgentPluginPorts([], [])
    expect(merged).toHaveLength(3)
    expect(merged.find((port) => port.id === MESSAGE_IN_PORT_ID)?.ui?.position).toBe('LEFT')
    expect(merged.find((port) => port.id === MESSAGE_OUT_PORT_ID)?.ui?.position).toBe('RIGHT')
    expect(merged.find((port) => port.id === AGENT_PLUG_PORT_ID)?.ui?.position).toBe('TOP')
    expect(merged.find((port) => port.id === MESSAGE_IN_PORT_ID)?.ui?.color).toBe('#71717a')
  })

  it('routes delegate agents away from host port merge', () => {
    expect(isDelegateAgentNode({ delegateAgentId: 'debug' })).toBe(true)
    const hostPorts = resolveNodePorts('AGENT', [], [], { delegateAgentId: 'debug' })
    expect(hostPorts.some((port) => port.id === CAPABILITIES_PORT_ID && port.direction === 'INPUT')).toBe(false)
    expect(hostPorts.some((port) => port.id === AGENT_PLUG_PORT_ID && port.direction === 'OUTPUT')).toBe(true)
    expect(hostPorts.some((port) => port.id === MESSAGE_IN_PORT_ID && port.direction === 'INPUT')).toBe(true)
    expect(hostPorts.some((port) => port.id === MESSAGE_OUT_PORT_ID && port.direction === 'OUTPUT')).toBe(true)
  })

  it('merges missing capabilities output onto stale TOOL workflow ports', () => {
    const catalogPorts = catalogComponentToPorts({
      id: 'olo-core:calculator',
      kind: 'TOOL',
      inputs: [
        { id: 'in', schema: 'message', type: 'message', acceptType: 'message', ui: { color: '#ef4444' } },
      ],
      outputs: [
        { id: 'out', schema: 'message', type: 'message', ui: { color: '#ef4444' } },
        { id: CAPABILITIES_PORT_ID, schema: 'capabilities', type: 'capabilities', ui: { color: '#22c55e' } },
      ],
    })
    const merged = resolveCapabilityPluginPorts('TOOL', [
      {
        id: 'in',
        schema: 'message',
        type: 'message',
        direction: 'INPUT',
        ui: { color: '#ef4444' },
      },
      {
        id: 'out',
        schema: 'message',
        type: 'message',
        direction: 'OUTPUT',
        ui: { color: '#ef4444' },
      },
    ], catalogPorts)
    expect(merged.some((port) => port.id === CAPABILITIES_PORT_ID && port.direction === 'OUTPUT')).toBe(true)
  })

  it('resolveNodePorts routes TOOL and AGENT through the correct merge', () => {
    const catalogPorts = catalogComponentToPorts({
      id: 'olo-core:calculator',
      kind: 'TOOL',
      outputs: [{ id: CAPABILITIES_PORT_ID, schema: 'capabilities', type: 'capabilities' }],
    })
    expect(resolveCapabilityPluginPorts('TOOL', [], catalogPorts).some((port) => port.id === CAPABILITIES_PORT_ID)).toBe(true)
    expect(resolveCapabilityPluginPorts('TOOL', [], catalogPorts).find((port) => port.id === CAPABILITIES_PORT_ID)?.ui?.position).toBe('TOP')
    expect(resolveNodePorts('AGENT', [], [])).toHaveLength(AGENT_HOST_PORTS.length)
  })

  it('merges missing plugin inputs onto stale AGENT workflow ports', () => {
    const catalogPorts = catalogComponentToPorts({
      id: 'olo-core:AGENT',
      kind: 'NODE',
      inputs: [
        { id: 'in', schema: 'message', type: 'message', acceptType: 'message', ui: { color: '#ef4444' } },
        { id: CAPABILITIES_PORT_ID, schema: 'capabilities', type: 'capabilities' },
        { id: AGENT_PLUG_PORT_ID, schema: 'agent-plug', type: 'agent-plug' },
      ],
      outputs: [{ id: 'out', schema: 'message', type: 'message', ui: { color: '#ef4444' } }],
    })
    const merged = resolveAgentHostPorts('AGENT', [
      {
        id: 'in',
        schema: 'message',
        type: 'message',
        direction: 'INPUT',
        ui: { color: '#ef4444' },
      },
      {
        id: 'out',
        schema: 'message',
        type: 'message',
        direction: 'OUTPUT',
        ui: { color: '#ef4444' },
      },
    ], catalogPorts)
    expect(merged.some((port) => port.id === CAPABILITIES_PORT_ID)).toBe(true)
    expect(merged.some((port) => port.id === AGENT_PLUG_PORT_ID)).toBe(true)
  })

  it('normalizes stale required message ports on tool nodes', () => {
    const merged = resolveCapabilityPluginPorts('TOOL', [
      {
        id: 'in',
        label: 'message in',
        schema: 'message',
        type: 'message',
        acceptType: 'message',
        direction: 'INPUT',
        required: true,
        minConnections: 1,
        maxConnections: 1,
        ui: { position: 'LEFT', color: '#ef4444' },
      },
    ], [])
    const messageIn = merged.find((port) => port.id === 'in' && port.direction === 'INPUT')
    expect(messageIn?.required).toBe(false)
    expect(messageIn?.minConnections).toBe(0)
    expect(messageIn?.ui?.color).toBe('#71717a')
  })
})
