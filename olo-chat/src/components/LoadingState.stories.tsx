/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import type { Meta, StoryObj } from '@storybook/react'
import { LoadingState } from './LoadingState'

const meta: Meta<typeof LoadingState> = {
  title: 'States/LoadingState',
  component: LoadingState,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  argTypes: {
    message: { control: 'text' },
  },
}

export default meta

type Story = StoryObj<typeof LoadingState>

export const Default: Story = {
  args: {
    message: 'Loading…',
  },
}

export const CustomMessage: Story = {
  args: {
    message: 'Loading tenants…',
  },
}

export const NoMessage: Story = {
  args: {
    message: '',
  },
}
