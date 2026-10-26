/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import type { Meta, StoryObj } from '@storybook/react'
import { TenantConfigurationList } from './TenantConfigurationList'
import type { Tenant } from '../types/tenant'

const mockTenants: Tenant[] = [
  { id: '2a2a91fb-f5b4-4cf0-b917-524d242b2e3d', name: 'Acme Corp', description: 'Production tenant', configVersion: '1.0' },
  { id: 'b3c4d5e6-f7a8-4b9c-8d0e-1f2a3b4c5d6e', name: '', description: undefined, configVersion: '2.0' },
  { id: 'tenant-dev', name: 'Dev Tenant', description: 'Development', configVersion: '' },
]

const meta: Meta<typeof TenantConfigurationList> = {
  title: 'Configuration/TenantConfigurationList',
  component: TenantConfigurationList,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  argTypes: {
    onSelectTenant: { action: 'selectTenant' },
    onAddNew: { action: 'addNew' },
    onDeleteTenant: { action: 'deleteTenant' },
  },
}

export default meta

type Story = StoryObj<typeof TenantConfigurationList>

export const WithTenants: Story = {
  args: {
    tenants: mockTenants,
    loading: false,
    selectedTenantId: mockTenants[0].id,
    onSelectTenant: () => {},
    onAddNew: () => {},
    onDeleteTenant: () => {},
  },
}

export const Empty: Story = {
  args: {
    tenants: [],
    loading: false,
    selectedTenantId: null,
    onSelectTenant: () => {},
    onAddNew: () => {},
    onDeleteTenant: () => {},
  },
}

export const Loading: Story = {
  args: {
    tenants: [],
    loading: true,
    selectedTenantId: null,
    onSelectTenant: () => {},
    onAddNew: () => {},
    onDeleteTenant: () => {},
  },
}

export const NoSelection: Story = {
  args: {
    tenants: mockTenants,
    loading: false,
    selectedTenantId: null,
    onSelectTenant: () => {},
    onAddNew: () => {},
    onDeleteTenant: () => {},
  },
}
