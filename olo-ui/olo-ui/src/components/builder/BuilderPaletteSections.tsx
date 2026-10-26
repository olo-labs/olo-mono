/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { CatalogComponentBase } from '../../types/catalog'
import type { WorkflowDocument, WorkflowSummary } from '../../types/workflow'
import { isPlannerAgentEnabled } from '../../lib/plannerContext'
import { workflowHooks } from '../../lib/workflowResources'
import { ExpandableSection } from './ExpandableSection'
import {
  BuilderPaletteItem,
  handleAgentDragStart,
  handleHookDragStart,
} from './BuilderPaletteItem'

export function BuilderAgentsSection({
  otherWorkflows,
  workflows,
  draft,
  disabled,
  expanded,
  onToggle,
}: {
  otherWorkflows: WorkflowSummary[]
  workflows: WorkflowSummary[]
  draft: WorkflowDocument | null
  disabled: boolean
  expanded: boolean
  onToggle: () => void
}) {
  const workflowLabel = (id: string) => workflows.find((w) => w.id === id)?.label ?? id
  return (
    <ExpandableSection
      title="Agents"
      count={otherWorkflows.length}
      expanded={expanded}
      onToggle={onToggle}
      hint="Drag agent presets onto the canvas to delegate to other workflows."
    >
      {otherWorkflows.length === 0 ? (
        <p className="builder-empty">No other agent presets available.</p>
      ) : (
        <ul className="builder-node-list">
          {otherWorkflows.map((workflow) => (
            <BuilderPaletteItem
              key={workflow.id}
              title={workflow.label ?? workflowLabel(workflow.id!)}
              emoji="🤖"
              description={workflow.description ?? workflow.id ?? undefined}
              disabled={disabled}
              active={draft ? isPlannerAgentEnabled(draft, workflow.id!) : false}
              onDragStart={(e) =>
                handleAgentDragStart(e, workflow.id!, workflow.label ?? workflowLabel(workflow.id!))
              }
            />
          ))}
        </ul>
      )}
    </ExpandableSection>
  )
}

export function BuilderHooksSection({
  catalogHooks,
  draft,
  disabled,
  expanded,
  onToggle,
}: {
  catalogHooks: CatalogComponentBase[]
  draft: WorkflowDocument | null
  disabled: boolean
  expanded: boolean
  onToggle: () => void
}) {
  return (
    <ExpandableSection
      title="Hooks"
      count={draft ? workflowHooks(draft).length : 0}
      expanded={expanded}
      onToggle={onToggle}
      hint="Drag observability hooks onto the canvas. Wire agent definition ports into an Agent node."
    >
      {catalogHooks.length === 0 ? (
        <p className="builder-empty">No catalog hooks loaded.</p>
      ) : (
        <ul className="builder-node-list">
          {catalogHooks.map((hook) => (
            <BuilderPaletteItem
              key={hook.id}
              title={hook.name ?? hook.id}
              emoji={hook.emoji ?? '🪝'}
              description={hook.description ?? hook.id}
              disabled={disabled}
              onDragStart={(e) => handleHookDragStart(e, hook)}
            />
          ))}
        </ul>
      )}
    </ExpandableSection>
  )
}
