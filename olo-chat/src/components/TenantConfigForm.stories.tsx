/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import type { Meta, StoryObj } from '@storybook/react'
import { TenantConfigForm } from './TenantConfigForm'
import type { Tenant } from '../types/tenant'

const mockTenant: Tenant = {
  id: '2a2a91fb-f5b4-4cf0-b917-524d242b2e3d',
  name: 'Acme Corp',
  description: 'Production tenant',
  configVersion: '1.0',
}

const meta: Meta<typeof TenantConfigForm> = {
  title: 'Configuration/TenantConfigForm',
  component: TenantConfigForm,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  argTypes: {
    onSave: { action: 'save' },
  },
}

export default meta

type Story = StoryObj<typeof TenantConfigForm>

export const EmptyState: Story = {
  args: {
    tenant: null,
    isAddingNew: false,
    onSave: async () => {},
  },
}

export const AddNew: Story = {
  args: {
    tenant: null,
    isAddingNew: true,
    onSave: async (tenant) => {
      console.log('Save new tenant', tenant)
    },
  },
}

export const EditExisting: Story = {
  args: {
    tenant: mockTenant,
    isAddingNew: false,
    onSave: async (tenant) => {
      console.log('Update tenant', tenant)
    },
  },
}

export const EditMinimalTenant: Story = {
  args: {
    tenant: { id: 'uuid-only', name: '' },
    isAddingNew: false,
    onSave: async () => {},
  },
}
