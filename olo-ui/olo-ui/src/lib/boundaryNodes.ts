/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { WorkflowDocument, WorkflowNode } from '../types/workflow'

import { cloneWorkflowNodeTemplate } from './workflowNodeTemplates'



export const START_NODE_TYPE = 'START'

export const END_NODE_TYPE = 'END'

const LEGACY_START = 'INPUT'

const LEGACY_END = 'OUTPUT'



export const RETURN_VARIABLE_METADATA_KEY = 'returnVariable'



export function normalizeNodeType(type: string): string {

  if (type === LEGACY_START) return START_NODE_TYPE

  if (type === LEGACY_END) return END_NODE_TYPE

  return type

}



export function isStartNodeType(type: string): boolean {

  return normalizeNodeType(type) === START_NODE_TYPE

}



export function isEndNodeType(type: string): boolean {

  return normalizeNodeType(type) === END_NODE_TYPE

}



export function isBoundaryNodeType(type: string): boolean {

  return isStartNodeType(type) || isEndNodeType(type)

}



export function readStartInputMappings(node: WorkflowNode): string[] {

  const fromConfig = node.configuration?.inputVariableMappings

  if (Array.isArray(fromConfig)) {

    return fromConfig.filter((v): v is string => typeof v === 'string')

  }

  return (node.reads ?? [])

    .filter((r): r is string => typeof r === 'string' && r.startsWith('input.'))

    .map((r) => r.slice('input.'.length))

}



export function readEndOutputMapping(node: WorkflowNode, workflow: WorkflowDocument): string {

  const fromConfig = node.configuration?.outputVariableMapping

  if (typeof fromConfig === 'string' && fromConfig.trim()) return fromConfig

  const fromMeta = workflow.metadata?.[RETURN_VARIABLE_METADATA_KEY]

  if (typeof fromMeta === 'string' && fromMeta.trim()) return fromMeta

  const fromWrite = (node.writes ?? []).find(

    (w): w is string => typeof w === 'string' && w.startsWith('state.'),

  )

  if (fromWrite) return fromWrite.slice('state.'.length)

  return ''

}



export function applyStartInputMappings(

  workflow: WorkflowDocument,

  nodeId: string,

  variableNames: string[],

): WorkflowDocument {

  const nodes = (workflow.nodes ?? []).map((node) => {

    if (node.id !== nodeId || !isStartNodeType(node.type)) return node

    return {

      ...node,

      type: START_NODE_TYPE,

      reads: variableNames.map((name) => `input.${name}`),

      configuration: {

        ...node.configuration,

        inputVariableMappings: variableNames,

      },

    }

  })

  return { ...workflow, nodes }

}



export function applyEndOutputMapping(

  workflow: WorkflowDocument,

  nodeId: string,

  variableName: string,

): WorkflowDocument {

  const nodes = (workflow.nodes ?? []).map((node) => {

    if (node.id !== nodeId || !isEndNodeType(node.type)) return node

    return {

      ...node,

      type: END_NODE_TYPE,

      writes: variableName ? [`state.${variableName}`] : [],

      configuration: {

        ...node.configuration,

        outputVariableMapping: variableName,

      },

    }

  })

  return {

    ...workflow,

    nodes,

    metadata: {

      ...workflow.metadata,

      ...(variableName ? { [RETURN_VARIABLE_METADATA_KEY]: variableName } : {}),

    },

  }

}



export function createStartNode(

  workflow: WorkflowDocument,

  position: { x: number; y: number },

  existingIds: Iterable<string>,

): WorkflowNode {

  return cloneWorkflowNodeTemplate(workflow, START_NODE_TYPE, position, existingIds)

}



export function createEndNode(

  workflow: WorkflowDocument,

  position: { x: number; y: number },

  existingIds: Iterable<string>,

): WorkflowNode {

  return cloneWorkflowNodeTemplate(workflow, END_NODE_TYPE, position, existingIds)

}



/** Migrate legacy INPUT/OUTPUT nodes and ids to START/END. */

export function normalizeWorkflowBoundaries(doc: WorkflowDocument): WorkflowDocument {

  const idRemap = new Map<string, string>()



  const nodes = (doc.nodes ?? []).map((node) => {

    const normalizedType = normalizeNodeType(node.type)

    const nextId = legacyNodeId(normalizedType, node.id)

    if (nextId) idRemap.set(node.id, nextId)

    return {

      ...node,

      id: nextId ?? node.id,

      type: normalizedType,

    }

  })



  const edges = (doc.edges ?? []).map((edge) => ({

    ...edge,

    sourceNodeId: edge.sourceNodeId ? idRemap.get(edge.sourceNodeId) ?? edge.sourceNodeId : edge.sourceNodeId,

    targetNodeId: edge.targetNodeId ? idRemap.get(edge.targetNodeId) ?? edge.targetNodeId : edge.targetNodeId,

    source: edge.source ? idRemap.get(edge.source) ?? edge.source : edge.source,

    target: edge.target ? idRemap.get(edge.target) ?? edge.target : edge.target,

    from: edge.from ? idRemap.get(edge.from) ?? edge.from : edge.from,

    to: edge.to ? idRemap.get(edge.to) ?? edge.to : edge.to,

  }))



  return { ...doc, nodes, edges }

}



function legacyNodeId(type: string, id: string): string | null {

  if (isStartNodeType(type) && id === 'input') return 'start'

  if (isEndNodeType(type) && id === 'output') return 'end'

  return null

}

