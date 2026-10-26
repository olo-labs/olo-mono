/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { Meta, StoryObj } from '@storybook/react'
import { ToolsPanel } from './ToolsPanel'

const meta: Meta<typeof ToolsPanel> = {
  title: 'Layout/ToolsPanel',
  component: ToolsPanel,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  argTypes: {
    expanded: { control: 'boolean' },
    onToggle: { action: 'toggle' },
    sectionId: {
      control: 'select',
      options: ['overview', 'workflows', 'executions', 'observability', 'extensions', 'administration', null],
    },
    subId: { control: 'text' },
    runSelected: { control: 'boolean' },
  },
}

export default meta

type Story = StoryObj<typeof ToolsPanel>

const base = {
  onToggle: () => {},
  storeContext: {},
}

export const Collapsed: Story = {
  args: {
    ...base,
    expanded: false,
    sectionId: 'workflows',
    subId: 'agents',
    runSelected: false,
  },
}

export const Expanded: Story = {
  args: {
    ...base,
    expanded: true,
    sectionId: 'workflows',
    subId: 'agents',
    runSelected: false,
  },
}

export const AdministrationTenants: Story = {
  args: {
    ...base,
    expanded: true,
    sectionId: 'administration',
    subId: 'tenants',
    runSelected: false,
    storeContext: { tenants: [], tenantsLoading: false },
  },
}

export const NoToolsForView: Story = {
  args: {
    ...base,
    expanded: true,
    sectionId: 'overview',
    subId: '',
    runSelected: false,
    storeContext: {},
  },
}
