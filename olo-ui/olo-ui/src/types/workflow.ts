/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
/** Subset of olo-definition WorkflowDefinition used by Studio. */

export type {
  NodeSize,
  InlinePropertyWidget,
  InlinePropertyConfig,
  NodeTypeDesignerConfig,
  WorkflowLayoutDesigner,
  WorkflowCanvasDesigner,
  WorkflowDesigner,
  NodeDesignerConfig,
} from './workflowDesigner'

export type {
  WorkflowDocument,
  WorkflowParameter,
  VariableScope,
  WorkflowVariable,
  WorkflowTool,
  WorkflowHook,
  ChildWorkflowRef,
  AgentReference,
  ModelProvider,
  ModelRoutingRule,
  ModelRouting,
  WorkflowNode,
  WorkflowNodeExecution,
  WorkflowPort,
  WorkflowEdge,
  WorkflowSummary,
} from './workflowDocument'
