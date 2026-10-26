/**
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
    sectionId: { control: 'select', options: ['chat', 'knowledge', 'documents', null] },
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
    sectionId: 'chat',
    subId: 'conversation',
    runSelected: false,
  },
}

export const Expanded: Story = {
  args: {
    ...base,
    expanded: true,
    sectionId: 'chat',
    subId: 'conversation',
    runSelected: false,
  },
}

export const Knowledge: Story = {
  args: {
    ...base,
    expanded: true,
    sectionId: 'knowledge',
    subId: 'sources',
    runSelected: false,
  },
}

export const Documents: Story = {
  args: {
    ...base,
    expanded: true,
    sectionId: 'documents',
    subId: 'upload',
    runSelected: false,
  },
}
