/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { useMemo, type SyntheticEvent } from 'react'
import { resolveNodePresentation } from '../../lib/nodePresentation'
import type { WorkflowDocument, WorkflowNode } from '../../types/workflow'
import type { StudioCatalog } from '../../types/catalog'
import { InlinePropertyBlock } from './InlinePropertyBlock'

export interface NodeInlinePropertiesProps {
  workflow: WorkflowDocument
  node: WorkflowNode
  catalog: StudioCatalog | null
  readOnly?: boolean
  onChange: (workflow: WorkflowDocument) => void
}

export function NodeInlineProperties({
  workflow,
  node,
  catalog,
  readOnly = false,
  onChange,
}: NodeInlinePropertiesProps) {
  const presentation = useMemo(
    () => resolveNodePresentation(workflow, node, catalog),
    [catalog, node, workflow],
  )

  if (readOnly || presentation.inlineProperties.length === 0) return null

  const stopDrag = (event: SyntheticEvent) => {
    event.stopPropagation()
  }

  return (
    <div className="catalog-flow-node-properties nodrag nopan" onPointerDown={stopDrag}>
      {presentation.inlineProperties.map((property) => (
        <InlinePropertyBlock
          key={property.id}
          property={property}
          workflow={workflow}
          node={node}
          catalog={catalog}
          onChange={onChange}
        />
      ))}
    </div>
  )
}
