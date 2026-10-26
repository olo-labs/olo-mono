/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { WorkflowDesigner } from './workflowDesigner'

/** Core workflow document and parameter types. */

export interface WorkflowDocument {
  id: string
  enabled?: boolean
  label?: string
  role?: string
  shortDescription?: string
  emoji?: string
  designer?: WorkflowDesigner
  queue?: string
  workflowType?: string
  runAgain?: boolean
  version?: string
  runtime?: Record<string, unknown>
  capability?: Record<string, unknown>
  parameters?: Record<string, WorkflowParameter>
  nodes?: WorkflowNode[]
  edges?: WorkflowEdge[]
  variables?: WorkflowVariable[]
  tools?: WorkflowTool[]
  hooks?: WorkflowHook[]
  childWorkflows?: ChildWorkflowRef[]
  availableAgents?: AgentReference[]
  modelProviders?: ModelProvider[]
  modelRouting?: ModelRouting[]
  metadata?: Record<string, unknown>
  [key: string]: unknown
}

export interface WorkflowParameter {
  type?: string
  label?: string
  description?: string
  defaultValue?: unknown
  required?: boolean
  validation?: {
    minLength?: number
    maxLength?: number
    minimum?: number
    maximum?: number
    step?: number
  }
  ui?: {
    widget?: string
    group?: string
    help?: string
    placeholder?: string
    order?: number
  }
}

export type VariableScope =
  | 'LOCAL'
  | 'READONLY_EXTERNAL'
  | 'EXTERNAL'
  | 'GLOBAL'
  | 'CREDENTIAL'

export interface WorkflowVariable {
  name: string
  type?: string
  description?: string
  defaultValue?: unknown
  required?: boolean
  scope?: VariableScope
  metadata?: Record<string, unknown>
}

export interface WorkflowTool {
  id: string
  type?: string
  capability?: {
    name?: string
    description?: string
    tags?: string[]
    examples?: string[]
    required_inputs?: string[]
    required_outputs?: string[]
    tool_requirements?: string[]
    required_context?: string[]
  }
  runtimeBinding?: {
    implementationId?: string
  }
  configuration?: Record<string, unknown>
}

export interface WorkflowHook {
  id: string
  pattern: string
  pre?: { implementationId?: string }
  onError?: { implementationId?: string }
  finally?: { implementationId?: string }
}

export interface ChildWorkflowRef {
  workflowId: string
  workflowVersion?: string
}

export interface AgentReference {
  id: string
}

export interface ModelProvider {
  id: string
  provider: string
  model?: string
  configuration?: Record<string, unknown>
}

export interface ModelRoutingRule {
  name?: string
  providerId?: string
  match?: Record<string, unknown>
}

export interface ModelRouting {
  id?: string
  defaultProviderId?: string
  rules?: ModelRoutingRule[]
  metadata?: Record<string, unknown>
}

export interface WorkflowNode {
  id: string
  type: string
  label?: string
  configuration?: Record<string, unknown>
  execution?: WorkflowNodeExecution
  ports?: WorkflowPort[]
  reads?: unknown[]
  writes?: unknown[]
}

export interface WorkflowNodeExecution {
  executionKind?: string
  executionModel?: string
  routers?: unknown[]
  workflowRef?: {
    workflowId?: string
    version?: string
    inputMapping?: Record<string, unknown>
    outputMapping?: Record<string, unknown>
  }
}

export interface WorkflowPort {
  id: string
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

export interface WorkflowEdge {
  sourceNodeId?: string
  targetNodeId?: string
  sourcePortId?: string
  targetPortId?: string
  from?: string
  to?: string
  source?: string
  target?: string
}

export interface WorkflowSummary {
  fileName: string
  id: string | null
  label: string | null
  description?: string | null
  queue?: string | null
  workflowType?: string | null
}
