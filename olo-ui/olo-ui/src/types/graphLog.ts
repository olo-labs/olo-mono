/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
/** Summary of a dynamic subgraph injection log file. */

export interface GraphLogSummary {
  fileName: string
  id: string
  label: string | null
  kind: string | null
  workflowId: string | null
  timestamp: string | null
  plannerNodeId: string | null
  toolLabels: string[]
}
