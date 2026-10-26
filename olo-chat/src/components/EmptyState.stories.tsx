/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import type { Meta, StoryObj } from '@storybook/react'
import { EmptyState } from './EmptyState'

const meta: Meta<typeof EmptyState> = {
  title: 'States/EmptyState',
  component: EmptyState,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  argTypes: {
    message: { control: 'text' },
    hint: { control: 'text' },
  },
}

export default meta

type Story = StoryObj<typeof EmptyState>

export const Default: Story = {
  args: {
    message: 'No tenants yet',
    hint: 'Click + to add your first tenant.',
  },
}

export const MessageOnly: Story = {
  args: {
    message: 'No runs in this environment',
  },
}

export const WithLongHint: Story = {
  args: {
    message: 'No data to display',
    hint: 'Select a tenant and environment above, or run a pipeline to see execution history here.',
  },
}
