/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { WorkflowPort } from '../types/workflow'

export const MESSAGE_WIRE = 'message'
export const CAPABILITIES_WIRE = 'capabilities'
export const AGENT_PLUG_WIRE = 'agent-plug'

export const CAPABILITIES_PORT_ID = 'capabilities'
export const AGENT_PLUG_PORT_ID = 'agentPlug'

export const MESSAGE_PORT_COLOR = '#ef4444'
export const CAPABILITIES_PORT_COLOR = '#22c55e'
export const AGENT_PLUG_PORT_COLOR = '#a855f7'
/** Message ports on plugin nodes — wired by the planner/agent at runtime, not on the canvas. */
export const PLANNER_ROUTED_MESSAGE_PORT_COLOR = '#71717a'

export const MESSAGE_IN_PORT_ID = 'in'
export const MESSAGE_OUT_PORT_ID = 'out'

/** Canonical AGENT host ports — mirrors WorkflowBuilder.agentNode() / agent.json presets. */
export const AGENT_HOST_PORTS: WorkflowPort[] = [
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
    shortDescription: 'Incoming workflow message',
    ui: { position: 'LEFT', color: MESSAGE_PORT_COLOR },
  },
  {
    id: CAPABILITIES_PORT_ID,
    label: 'available tools',
    schema: CAPABILITIES_WIRE,
    type: CAPABILITIES_WIRE,
    acceptType: CAPABILITIES_WIRE,
    direction: 'INPUT',
    required: false,
    minConnections: 0,
    shortDescription: 'Tools and hooks registered for runtime prompt assembly (0 or more)',
    ui: { position: 'BOTTOM', color: CAPABILITIES_PORT_COLOR },
  },
  {
    id: AGENT_PLUG_PORT_ID,
    label: 'available agents',
    schema: AGENT_PLUG_WIRE,
    type: AGENT_PLUG_WIRE,
    acceptType: AGENT_PLUG_WIRE,
    direction: 'INPUT',
    required: false,
    minConnections: 0,
    shortDescription: 'Child workflows registered for runtime prompt assembly (0 or more)',
    ui: { position: 'BOTTOM', color: AGENT_PLUG_PORT_COLOR },
  },
  {
    id: 'out',
    label: 'message out',
    schema: 'message',
    type: 'message',
    direction: 'OUTPUT',
    required: false,
    minConnections: 0,
    shortDescription: 'Outgoing workflow message',
    ui: { position: 'RIGHT', color: MESSAGE_PORT_COLOR },
  },
]

/** Grayed message in/out on plugin nodes — shown for context, wired by planner at runtime. */
export const PLANNER_ROUTED_MESSAGE_PORTS: WorkflowPort[] = [
  {
    id: MESSAGE_IN_PORT_ID,
    label: 'message in',
    schema: MESSAGE_WIRE,
    type: MESSAGE_WIRE,
    acceptType: MESSAGE_WIRE,
    direction: 'INPUT',
    required: false,
    minConnections: 0,
    shortDescription: 'Incoming message (routed by planner/agent at runtime)',
    ui: { position: 'LEFT', color: PLANNER_ROUTED_MESSAGE_PORT_COLOR },
  },
  {
    id: MESSAGE_OUT_PORT_ID,
    label: 'message out',
    schema: MESSAGE_WIRE,
    type: MESSAGE_WIRE,
    direction: 'OUTPUT',
    required: false,
    minConnections: 0,
    shortDescription: 'Outgoing message (routed by planner/agent at runtime)',
    ui: { position: 'RIGHT', color: PLANNER_ROUTED_MESSAGE_PORT_COLOR },
  },
]

/** Plugin output on tool/hook canvas nodes — top boundary indicator for agent prompt assembly. */
export const CAPABILITY_PLUGIN_PORTS: WorkflowPort[] = [
  {
    id: CAPABILITIES_PORT_ID,
    label: 'available tools',
    schema: CAPABILITIES_WIRE,
    type: CAPABILITIES_WIRE,
    direction: 'OUTPUT',
    required: false,
    minConnections: 0,
    shortDescription: 'Capability indicator for runtime prompt assembly on a connected agent',
    ui: { position: 'TOP', color: CAPABILITIES_PORT_COLOR },
  },
]

/** Plugin output on delegate child-workflow nodes — top boundary indicator for agent prompt assembly. */
export const AGENT_PLUGIN_PORTS: WorkflowPort[] = [
  {
    id: AGENT_PLUG_PORT_ID,
    label: 'available agents',
    schema: AGENT_PLUG_WIRE,
    type: AGENT_PLUG_WIRE,
    direction: 'OUTPUT',
    required: false,
    minConnections: 0,
    shortDescription: 'Child workflow indicator for runtime prompt assembly on a connected agent',
    ui: { position: 'TOP', color: AGENT_PLUG_PORT_COLOR },
  },
]

export type CanvasPortProfileName = 'CAPABILITY_PLUGIN' | 'AGENT_PLUGIN' | 'PLANNER_HOST'

export type PortSide = 'LEFT' | 'RIGHT' | 'TOP' | 'BOTTOM'
