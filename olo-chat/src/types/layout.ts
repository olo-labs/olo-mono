/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Menu: Chat, Knowledge, Documents.
 * Section IDs are stable for URLs; labels are display-only.
 */
export type SectionId =
  | 'chat'
  | 'knowledge'
  | 'documents'

export interface SubOption {
  id: string
  label: string
  description?: string
  featureId?: keyof typeof import('../config/features').features
  toolIds?: string[]
}

export interface SectionConfig {
  id: SectionId
  label: string
  subtitle: string
  subOptions: SubOption[]
  runSelectedOptions?: SubOption[]
}

export const SECTIONS: SectionConfig[] = [
  {
    id: 'chat',
    label: 'Chat',
    subtitle: 'Conversation with Olo AI',
    subOptions: [
      { id: 'conversation', label: 'Conversation', description: 'Send messages and view run events' },
    ],
  },
  {
    id: 'knowledge',
    label: 'Knowledge',
    subtitle: 'Knowledge sources and status',
    subOptions: [
      { id: 'sources', label: 'Sources', description: 'List of knowledge source collections' },
      { id: 'create', label: 'Create new', description: 'Create new knowledge source' },
      { id: 'status', label: 'Status', description: 'Indexed, processing' },
    ],
  },
  {
    id: 'documents',
    label: 'Documents',
    subtitle: 'Document management',
    subOptions: [
      { id: 'upload', label: 'Upload / Manage Raw Files', description: 'Upload, filter, and track document processing' },
    ],
  },
]
