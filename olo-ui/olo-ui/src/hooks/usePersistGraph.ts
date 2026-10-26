/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { useCallback } from 'react'
import type { Edge, Node } from '@xyflow/react'
import { flowToWorkflow, type CatalogFlowNodeData } from '../lib/workflowGraph'
import type { WorkflowDocument } from '../types/workflow'
import type { StudioCatalog } from '../types/catalog'
import { workflowConfigurationStore } from '../store/workflowConfigurationStore'

export function usePersistGraph(
  draft: WorkflowDocument | null,
  catalog: StudioCatalog | null,
  readOnly: boolean,
  syncingRef: React.MutableRefObject<boolean>,
) {
  const updateDraft = workflowConfigurationStore((s) => s.updateDraft)
  return useCallback(
    (nextNodes: Node<CatalogFlowNodeData>[], nextEdges: Edge[]) => {
      if (!draft || syncingRef.current || readOnly) return
      updateDraft(flowToWorkflow(nextNodes, nextEdges, draft, catalog))
    },
    [catalog, draft, readOnly, syncingRef, updateDraft],
  )
}
