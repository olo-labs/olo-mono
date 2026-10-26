/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Main content for Knowledge section. Sub-options: sources, create, status.
 */
import { KnowledgeCreateView } from './KnowledgeCreateView'
import { KnowledgeIngestRunTracker } from './KnowledgeIngestRunTracker'
import { KnowledgeSourcesList } from './KnowledgeSourcesList'
import { KnowledgeStatusView } from './KnowledgeStatusView'
import { knowledgeIngestStore } from '../store/knowledgeIngestStore'

export interface KnowledgeViewProps {
  subId: string
}

export function KnowledgeView({ subId }: KnowledgeViewProps) {
  const runs = knowledgeIngestStore((s) => s.runs)
  const activeRuns = runs.filter(
    (r) => r.runId && (r.status === 'in_progress' || r.status === 'pending')
  )

  const trackers = activeRuns.map((run) => <KnowledgeIngestRunTracker key={run.id} run={run} />)

  if (subId === 'create') {
    return (
      <>
        <KnowledgeCreateView />
        {trackers}
      </>
    )
  }
  if (subId === 'status') {
    return (
      <>
        <KnowledgeStatusView />
        {trackers}
      </>
    )
  }
  return (
    <div className="knowledge-view knowledge-view-sources">
      <p className="knowledge-view-description">
        Review every knowledge source collection available to chat. File collections come from
        uploaded document folders, and additional source types can appear here as they are added.
      </p>
      <KnowledgeSourcesList />
      {trackers}
    </div>
  )
}
