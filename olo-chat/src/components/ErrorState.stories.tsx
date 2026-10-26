/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import type { Meta, StoryObj } from '@storybook/react'
import { ErrorState } from './ErrorState'

const meta: Meta<typeof ErrorState> = {
  title: 'States/ErrorState',
  component: ErrorState,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  argTypes: {
    message: { control: 'text' },
    onRetry: { action: 'retry' },
  },
}

export default meta

type Story = StoryObj<typeof ErrorState>

export const Default: Story = {
  args: {
    message: 'Something went wrong.',
  },
}

export const WithRetry: Story = {
  args: {
    message: 'Failed to load tenants. Check your connection and try again.',
    onRetry: () => {},
  },
}

export const CustomMessage: Story = {
  args: {
    message: 'Unable to save tenant. The name may already be in use.',
    onRetry: () => {},
  },
}
