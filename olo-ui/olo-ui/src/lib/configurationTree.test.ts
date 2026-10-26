/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { describe, expect, it } from 'vitest'
import {
  buildConfigurationTree,
  configurationRootLabel,
  layoutConfigurationTree,
  CONFIG_FILE_NODE,
  CONFIG_FOLDER_NODE,
} from './configurationTree'
import type { WorkflowSummary } from '../types/workflow'

const workflows: WorkflowSummary[] = [
  { fileName: 'default/agent.json', id: 'agent', label: 'Agent' },
  { fileName: 'default/ask.json', id: 'ask', label: 'Ask' },
  { fileName: 'current-active/architect.json', id: 'architect', label: 'Architect' },
]

describe('configurationTree', () => {
  it('derives root label from configuration path', () => {
    expect(configurationRootLabel('/data/olo-configuration')).toBe('olo-configuration')
    expect(configurationRootLabel('')).toBe('configuration')
  })

  it('builds nested folder structure with files as leaves', () => {
    const tree = buildConfigurationTree(workflows, 'olo-configuration')
    expect(tree.kind).toBe('folder')
    expect(tree.children.map((child) => child.name)).toEqual(['current-active', 'default'])

    const defaultFolder = tree.children.find((child) => child.name === 'default')
    expect(defaultFolder?.children.map((child) => child.kind)).toEqual(['file', 'file'])
    expect(defaultFolder?.children.map((child) => child.fileName)).toEqual([
      'default/agent.json',
      'default/ask.json',
    ])
  })

  it('lays out folder and file nodes with connecting edges', () => {
    const tree = buildConfigurationTree(workflows, 'olo-configuration')
    const { nodes, edges } = layoutConfigurationTree(tree)

    const folders = nodes.filter((node) => node.type === CONFIG_FOLDER_NODE)
    const files = nodes.filter((node) => node.type === CONFIG_FILE_NODE)

    expect(folders.length).toBeGreaterThan(0)
    expect(files).toHaveLength(3)
    expect(edges.length).toBe(nodes.length - 1)
    expect(edges.some((edge) => edge.target === 'file:default/agent.json')).toBe(true)
  })
})
