/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { Edge, Node } from '@xyflow/react'
import type { WorkflowSummary } from '../types/workflow'

export const CONFIG_FOLDER_NODE = 'configFolder'
export const CONFIG_FILE_NODE = 'configFile'

export interface ConfigFolderNodeData {
  label: string
  path: string
}

export interface ConfigFileNodeData {
  fileName: string
  label: string
  workflowId: string | null
}

interface TreeNode {
  kind: 'folder' | 'file'
  name: string
  path: string
  fileName?: string
  workflowId?: string | null
  workflowLabel?: string | null
  children: TreeNode[]
}

const NODE_WIDTH = 196
const NODE_HEIGHT = 56
const H_GAP = 40
const V_GAP = 72

export function configurationRootLabel(configurationRoot: string): string {
  const normalized = configurationRoot.replace(/\\/g, '/').replace(/\/+$/, '')
  const parts = normalized.split('/').filter(Boolean)
  return parts[parts.length - 1] || 'configuration'
}

function displayFileLabel(node: TreeNode): string {
  if (node.workflowLabel?.trim()) return node.workflowLabel.trim()
  if (node.workflowId?.trim()) return node.workflowId.trim()
  return node.name
}

function sortTree(node: TreeNode): void {
  node.children.sort((a, b) => {
    if (a.kind !== b.kind) return a.kind === 'folder' ? -1 : 1
    return a.name.localeCompare(b.name)
  })
  node.children.forEach(sortTree)
}

export function buildConfigurationTree(
  workflows: WorkflowSummary[],
  rootLabel: string,
): TreeNode {
  const root: TreeNode = {
    kind: 'folder',
    name: rootLabel,
    path: '',
    children: [],
  }

  for (const workflow of workflows) {
    const normalized = workflow.fileName.replace(/\\/g, '/')
    const parts = normalized.split('/').filter(Boolean)
    if (parts.length === 0) continue

    let current = root
    for (let i = 0; i < parts.length; i++) {
      const part = parts[i]
      const isFile = i === parts.length - 1
      if (isFile) {
        current.children.push({
          kind: 'file',
          name: part,
          path: normalized,
          fileName: normalized,
          workflowId: workflow.id,
          workflowLabel: workflow.label,
          children: [],
        })
        continue
      }

      const folderPath = parts.slice(0, i + 1).join('/')
      let folder = current.children.find((child) => child.kind === 'folder' && child.path === folderPath)
      if (!folder) {
        folder = { kind: 'folder', name: part, path: folderPath, children: [] }
        current.children.push(folder)
      }
      current = folder
    }
  }

  sortTree(root)
  return root
}

function folderNodeId(path: string): string {
  return `dir:${path || '__root__'}`
}

function fileNodeId(fileName: string): string {
  return `file:${fileName}`
}

export function layoutConfigurationTree(tree: TreeNode): { nodes: Node[]; edges: Edge[] } {
  const nodes: Node[] = []
  const edges: Edge[] = []

  function layoutSubtree(
    node: TreeNode,
    depth: number,
    xStart: number,
  ): { centerX: number; nextX: number } {
    if (node.kind === 'file') {
      const x = xStart
      const y = depth * (NODE_HEIGHT + V_GAP)
      nodes.push({
        id: fileNodeId(node.fileName!),
        type: CONFIG_FILE_NODE,
        position: { x, y },
        data: {
          fileName: node.fileName!,
          label: displayFileLabel(node),
          workflowId: node.workflowId ?? null,
        } satisfies ConfigFileNodeData,
        draggable: false,
        selectable: true,
      })
      return { centerX: x + NODE_WIDTH / 2, nextX: x + NODE_WIDTH + H_GAP }
    }

    if (node.children.length === 0) {
      const x = xStart
      const y = depth * (NODE_HEIGHT + V_GAP)
      nodes.push({
        id: folderNodeId(node.path),
        type: CONFIG_FOLDER_NODE,
        position: { x, y },
        data: { label: node.name, path: node.path } satisfies ConfigFolderNodeData,
        draggable: false,
        selectable: false,
      })
      return { centerX: x + NODE_WIDTH / 2, nextX: x + NODE_WIDTH + H_GAP }
    }

    let cursor = xStart
    const childCenters: number[] = []
    const parentId = folderNodeId(node.path)

    for (const child of node.children) {
      const result = layoutSubtree(child, depth + 1, cursor)
      childCenters.push(result.centerX)
      cursor = result.nextX
      const childId = child.kind === 'file' ? fileNodeId(child.fileName!) : folderNodeId(child.path)
      edges.push({
        id: `${parentId}->${childId}`,
        source: parentId,
        target: childId,
        type: 'smoothstep',
        selectable: false,
      })
    }

    const centerX = (childCenters[0] + childCenters[childCenters.length - 1]) / 2
    const x = centerX - NODE_WIDTH / 2
    const y = depth * (NODE_HEIGHT + V_GAP)
    nodes.push({
      id: parentId,
      type: CONFIG_FOLDER_NODE,
      position: { x, y },
      data: { label: node.name, path: node.path } satisfies ConfigFolderNodeData,
      draggable: false,
      selectable: false,
    })

    return { centerX, nextX: cursor }
  }

  layoutSubtree(tree, 0, 0)
  return { nodes, edges }
}

export function workflowsToDirectoryGraph(
  workflows: WorkflowSummary[],
  configurationRoot: string,
): { nodes: Node[]; edges: Edge[] } {
  const rootLabel = configurationRootLabel(configurationRoot)
  const tree = buildConfigurationTree(workflows, rootLabel)
  return layoutConfigurationTree(tree)
}
