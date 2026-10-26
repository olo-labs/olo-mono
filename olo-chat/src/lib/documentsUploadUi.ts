/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import type { DocumentUploadRow } from '../store/documentUploadsStore'

export function fileIcon(name: string): string {
  const lower = name.toLowerCase()
  if (lower.endsWith('.pdf')) return '📕'
  if (lower.endsWith('.md')) return '📝'
  if (lower.endsWith('.txt')) return '📃'
  return '📄'
}

export function statusDisplay(status: DocumentUploadRow['status']): { label: string; icon: string } {
  switch (status) {
    case 'uploading':
      return { label: 'Uploading', icon: '⏳' }
    case 'uploaded':
      return { label: 'Uploaded', icon: '📁' }
    case 'processing':
      return { label: 'Processing', icon: '⚙️' }
    case 'ready':
      return { label: 'Ready', icon: '✅' }
    case 'failed':
      return { label: 'Failed', icon: '❌' }
    default:
      return { label: status, icon: '•' }
  }
}
