/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { CatalogComponentBase } from '../types/catalog'
import type { WorkflowPort } from '../types/workflow'
import {
  AGENT_PLUG_WIRE,
  CAPABILITIES_WIRE,
  MESSAGE_WIRE,
  AGENT_PLUG_PORT_COLOR,
  CAPABILITIES_PORT_COLOR,
  MESSAGE_PORT_COLOR,
} from './workflowNodePortConstants'

/** Map catalog port descriptors to workflow ports (annotation export only — no UI fallbacks). */
export function catalogComponentToPorts(item: CatalogComponentBase): WorkflowPort[] {
  const ports: WorkflowPort[] = []
  for (const input of item.inputs ?? []) {
    ports.push(mapCatalogPort(input, 'INPUT'))
  }
  for (const output of item.outputs ?? []) {
    ports.push(mapCatalogPort(output, 'OUTPUT'))
  }
  return ports
}

/** Merge catalog ports missing from persisted workflow JSON (annotation is source of truth). */
export function mergeMissingCatalogPorts(
  workflowPorts: WorkflowPort[] | undefined,
  catalogPorts: WorkflowPort[],
): WorkflowPort[] {
  const ports = [...(workflowPorts ?? [])]
  const portIds = new Set(ports.map((port) => `${port.direction}:${port.id}`))
  for (const catalogPort of catalogPorts) {
    const key = `${catalogPort.direction}:${catalogPort.id}`
    if (!portIds.has(key)) {
      ports.push(catalogPort)
      portIds.add(key)
    }
  }
  return ports
}

export function syncCanonicalPortLayout(
  ports: WorkflowPort[],
  canonicalPorts: WorkflowPort[],
): WorkflowPort[] {
  const canonical = new Map(
    canonicalPorts.map((port) => [`${port.direction}:${port.id}`, port]),
  )
  return ports.map((port) => {
    const reference = canonical.get(`${port.direction}:${port.id}`)
    if (!reference) return port
    return {
      ...port,
      label: reference.label,
      schema: reference.schema,
      type: reference.type,
      acceptType: reference.acceptType,
      required: reference.required,
      minConnections: reference.minConnections,
      maxConnections: reference.maxConnections,
      shortDescription: reference.shortDescription ?? port.shortDescription,
      ui: {
        ...port.ui,
        position: reference.ui?.position,
        color: reference.ui?.color ?? port.ui?.color,
      },
    }
  })
}

function mapCatalogPort(
  port: NonNullable<CatalogComponentBase['inputs']>[number],
  direction: 'INPUT' | 'OUTPUT',
): WorkflowPort {
  const wireType = port.type ?? port.schema ?? 'any'
  const wireColor = portColorForWire(wireType)
  return {
    id: port.id,
    label: port.label ?? port.name ?? port.id,
    name: port.name ?? port.id,
    shortDescription: port.shortDescription ?? port.description,
    schema: port.schema ?? 'any',
    type: wireType,
    acceptType: port.acceptType ?? (direction === 'INPUT' ? port.type ?? port.schema : undefined),
    direction,
    required: port.required,
    minConnections: port.minConnections,
    maxConnections: port.maxConnections,
    ui: {
      position: port.ui?.position ?? (direction === 'INPUT' ? 'LEFT' : 'RIGHT'),
      color: port.ui?.color ?? wireColor,
    },
  }
}

function portColorForWire(wireType: string): string | undefined {
  switch (wireType.trim().toLowerCase()) {
    case MESSAGE_WIRE:
      return MESSAGE_PORT_COLOR
    case CAPABILITIES_WIRE:
      return CAPABILITIES_PORT_COLOR
    case AGENT_PLUG_WIRE:
      return AGENT_PLUG_PORT_COLOR
    default:
      return undefined
  }
}
