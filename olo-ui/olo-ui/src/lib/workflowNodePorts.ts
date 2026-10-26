/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { StudioCatalog } from '../types/catalog'
import type { WorkflowPort } from '../types/workflow'
import {
  AGENT_HOST_PORTS,
  AGENT_PLUGIN_PORTS,
  CAPABILITY_PLUGIN_PORTS,
  CAPABILITIES_PORT_ID,
  AGENT_PLUG_PORT_ID,
  MESSAGE_IN_PORT_ID,
  MESSAGE_OUT_PORT_ID,
  PLANNER_ROUTED_MESSAGE_PORTS,
  type PortSide,
} from './workflowNodePortConstants'
import {
  catalogComponentToPorts,
  mergeMissingCatalogPorts,
  syncCanonicalPortLayout,
} from './workflowNodePortMapping'

export {
  MESSAGE_WIRE,
  CAPABILITIES_WIRE,
  AGENT_PLUG_WIRE,
  CAPABILITIES_PORT_ID,
  AGENT_PLUG_PORT_ID,
  MESSAGE_PORT_COLOR,
  CAPABILITIES_PORT_COLOR,
  AGENT_PLUG_PORT_COLOR,
  PLANNER_ROUTED_MESSAGE_PORT_COLOR,
  MESSAGE_IN_PORT_ID,
  MESSAGE_OUT_PORT_ID,
  AGENT_HOST_PORTS,
  PLANNER_ROUTED_MESSAGE_PORTS,
  CAPABILITY_PLUGIN_PORTS,
  AGENT_PLUGIN_PORTS,
  type CanvasPortProfileName,
  type PortSide,
} from './workflowNodePortConstants'
export { catalogComponentToPorts, mergeMissingCatalogPorts } from './workflowNodePortMapping'

function syncAgentHostPortLayout(ports: WorkflowPort[]): WorkflowPort[] {
  return syncCanonicalPortLayout(ports, AGENT_HOST_PORTS)
}

/** Ensure AGENT nodes expose annotation-defined plugin inputs when workflow JSON is stale. */
export function resolveAgentHostPorts(
  workflowType: string,
  workflowPorts: WorkflowPort[] | undefined,
  catalogPorts: WorkflowPort[],
): WorkflowPort[] {
  if (workflowType.toUpperCase() !== 'AGENT') {
    return workflowPorts ?? []
  }
  const merged = mergeMissingCatalogPorts(workflowPorts, catalogPorts)
  const withDefaults = mergeMissingCatalogPorts(merged, AGENT_HOST_PORTS)
  return syncAgentHostPortLayout(withDefaults)
}

export function isDelegateAgentNode(configuration?: Record<string, unknown> | null): boolean {
  return typeof configuration?.delegateAgentId === 'string' && configuration.delegateAgentId.length > 0
}

/** Tool, hook, and delegate child-workflow nodes route message flow through the planner/agent. */
export function usesPlannerRoutedMessagePorts(
  workflowType: string,
  configuration?: Record<string, unknown> | null,
): boolean {
  const normalized = workflowType.toUpperCase()
  if (normalized === 'TOOL' || normalized === 'HOOK') return true
  if (normalized === 'AGENT' && isDelegateAgentNode(configuration)) return true
  return false
}

export function isMessagePortId(portId?: string | null): boolean {
  return portId === MESSAGE_IN_PORT_ID || portId === MESSAGE_OUT_PORT_ID
}

export function isPlannerRoutedMessagePort(
  workflowType: string,
  portId: string | null | undefined,
  configuration?: Record<string, unknown> | null,
): boolean {
  return usesPlannerRoutedMessagePorts(workflowType, configuration) && isMessagePortId(portId)
}

/** Ensure TOOL/HOOK nodes expose grayed message in/out plus top-aligned plugin output. */
export function resolveCapabilityPluginPorts(
  workflowType: string,
  workflowPorts: WorkflowPort[] | undefined,
  catalogPorts: WorkflowPort[],
): WorkflowPort[] {
  const normalized = workflowType.toUpperCase()
  if (normalized !== 'TOOL' && normalized !== 'HOOK') {
    return workflowPorts ?? []
  }
  const merged = mergeMissingCatalogPorts(workflowPorts, catalogPorts)
  const withMessages = mergeMissingCatalogPorts(merged, PLANNER_ROUTED_MESSAGE_PORTS)
  const withDefaults = mergeMissingCatalogPorts(withMessages, CAPABILITY_PLUGIN_PORTS)
  const syncedMessages = syncCanonicalPortLayout(withDefaults, PLANNER_ROUTED_MESSAGE_PORTS)
  return syncCanonicalPortLayout(syncedMessages, CAPABILITY_PLUGIN_PORTS)
}

/** Ensure delegate child-workflow AGENT nodes expose grayed message in/out plus top agentPlug output. */
export function resolveAgentPluginPorts(
  workflowPorts: WorkflowPort[] | undefined,
  catalogPorts: WorkflowPort[],
): WorkflowPort[] {
  const merged = mergeMissingCatalogPorts(workflowPorts, catalogPorts)
  const withMessages = mergeMissingCatalogPorts(merged, PLANNER_ROUTED_MESSAGE_PORTS)
  const withDefaults = mergeMissingCatalogPorts(withMessages, AGENT_PLUGIN_PORTS)
  const syncedMessages = syncCanonicalPortLayout(withDefaults, PLANNER_ROUTED_MESSAGE_PORTS)
  return syncCanonicalPortLayout(syncedMessages, AGENT_PLUGIN_PORTS)
}

export function resolveNodePorts(
  workflowType: string,
  workflowPorts: WorkflowPort[] | undefined,
  catalogPorts: WorkflowPort[],
  configuration?: Record<string, unknown> | null,
): WorkflowPort[] {
  const normalized = workflowType.toUpperCase()
  if (normalized === 'AGENT') {
    if (isDelegateAgentNode(configuration)) {
      return resolveAgentPluginPorts(workflowPorts, catalogPorts)
    }
    return resolveAgentHostPorts(normalized, workflowPorts, catalogPorts)
  }
  if (normalized === 'TOOL' || normalized === 'HOOK') {
    return resolveCapabilityPluginPorts(normalized, workflowPorts, catalogPorts)
  }
  return workflowPorts ?? []
}

export function resolvePortSide(port: WorkflowPort): PortSide {
  const position = port.ui?.position?.toUpperCase()
  if (position === 'TOP' || position === 'BOTTOM' || position === 'LEFT' || position === 'RIGHT') {
    return position
  }
  return port.direction === 'INPUT' ? 'LEFT' : 'RIGHT'
}

export function groupPortsBySide(ports: WorkflowPort[]): Record<PortSide, WorkflowPort[]> {
  const groups: Record<PortSide, WorkflowPort[]> = {
    LEFT: [],
    RIGHT: [],
    TOP: [],
    BOTTOM: [],
  }
  for (const port of ports) {
    groups[resolvePortSide(port)].push(port)
  }
  return groups
}

export function findWorkflowPresetPorts(
  catalog: StudioCatalog | null,
  presetId: string,
): WorkflowPort[] {
  const preset = catalog?.workflowPresets?.find((entry) => entry.id === presetId)
  if (!preset) return resolveAgentPluginPorts([], AGENT_PLUGIN_PORTS)
  return resolveAgentPluginPorts([], catalogComponentToPorts(preset))
}

export function isCapabilitiesPort(portId?: string | null): boolean {
  return portId === CAPABILITIES_PORT_ID
}

export function isAgentPlugPort(portId?: string | null): boolean {
  return portId === AGENT_PLUG_PORT_ID
}

export function isPluginPort(portId?: string | null): boolean {
  return isCapabilitiesPort(portId) || isAgentPlugPort(portId)
}
