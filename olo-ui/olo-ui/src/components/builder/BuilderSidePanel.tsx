/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { useState } from 'react'
import { catalogStore } from '../../store/catalogStore'
import { workflowConfigurationStore } from '../../store/workflowConfigurationStore'
import { catalogComponentGroups } from '../../lib/catalogComponents'
import { workflowPaletteNodes } from '../../lib/workflowNodeTemplates'
import { ExpandableSection } from './ExpandableSection'
import { ModelProvidersSection } from './ModelProvidersSection'
import { VariablesSection } from './VariablesSection'
import { workflowModelProviders } from '../../lib/workflowModelProviders'
import { BuilderPaletteItem, handleNodeDragStart } from './BuilderPaletteItem'
import {
  BuilderAgentsSection,
  BuilderCapabilitiesSection,
  BuilderHooksSection,
} from './BuilderPaletteSections'

export interface BuilderSidePanelProps {
  expanded: boolean
  onToggle: () => void
}

type SectionId =
  | 'components'
  | 'workflowVariables'
  | 'modelProviders'
  | 'capabilities'
  | 'agents'
  | 'hooks'

const DEFAULT_EXPANDED = new Set<SectionId>([
  'components',
  'workflowVariables',
  'modelProviders',
  'capabilities',
  'agents',
  'hooks',
])

export function BuilderSidePanel({ expanded, onToggle }: BuilderSidePanelProps) {
  const catalog = catalogStore((s) => s.catalog)
  const catalogLoading = catalogStore((s) => s.loading)
  const catalogError = catalogStore((s) => s.error)
  const draft = workflowConfigurationStore((s) => s.draft)
  const workflows = workflowConfigurationStore((s) => s.workflows)
  const nodeGroup = catalogComponentGroups(catalog).find((g) => g.id === 'nodes')
  const boundaryNodes = workflowPaletteNodes(draft)
  const catalogTools = catalog?.tools ?? []
  const catalogHooks = catalog?.hooks ?? []
  const [expandedSections, setExpandedSections] = useState<Set<SectionId>>(() => new Set(DEFAULT_EXPANDED))
  const [expandedNodeGroups, setExpandedNodeGroups] = useState(true)
  const otherWorkflows = workflows.filter((w) => w.id && w.id !== draft?.id)
  const disabled = !draft

  const toggleSection = (id: SectionId) => {
    setExpandedSections((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  return (
    <aside className={`builder-side-panel tools-panel side-panel ${expanded ? 'expanded' : 'collapsed'}`}>
      {expanded && (
        <div className="side-panel-inner">
          <div className="side-panel-title">Builder</div>
          {disabled ? <p className="builder-panel-message">Open a workflow under Agents first.</p> : null}
          {catalogLoading ? <p className="builder-panel-message">Loading catalog…</p> : null}
          {catalogError ? <p className="builder-panel-error">{catalogError}</p> : null}

          <ExpandableSection
            title="Components"
            count={(nodeGroup?.items.length ?? 0) + boundaryNodes.length}
            expanded={expandedSections.has('components')}
            onToggle={() => toggleSection('components')}
            hint="Drag nodes onto the canvas."
          >
            <section className={`builder-nodes-group ${expandedNodeGroups ? 'expanded' : 'collapsed'}`}>
              <button type="button" className="builder-nodes-header" onClick={() => setExpandedNodeGroups((v) => !v)}>
                <span className="builder-section-chevron">{expandedNodeGroups ? '▼' : '▶'}</span>
                <span>Nodes</span>
              </button>
              {expandedNodeGroups ? (
                <ul className="builder-node-list">
                  {boundaryNodes.map((item) => (
                    <BuilderPaletteItem
                      key={item.id}
                      title={item.name ?? item.id}
                      emoji={item.emoji}
                      description={item.description ?? item.id}
                      disabled={disabled}
                      onDragStart={(e) => handleNodeDragStart(e, item)}
                    />
                  ))}
                  {(nodeGroup?.items ?? []).map((item) => (
                    <BuilderPaletteItem
                      key={item.id}
                      title={item.name ?? item.id}
                      emoji={item.emoji}
                      description={item.description ?? item.id}
                      disabled={disabled}
                      onDragStart={(e) => handleNodeDragStart(e, item)}
                    />
                  ))}
                </ul>
              ) : null}
            </section>
          </ExpandableSection>

          <ExpandableSection
            title="Workflow variables"
            count={draft?.variables?.length ?? 0}
            expanded={expandedSections.has('workflowVariables')}
            onToggle={() => toggleSection('workflowVariables')}
            hint="Define workflow-scoped variables with type and scope."
          >
            <VariablesSection />
          </ExpandableSection>

          <ExpandableSection
            title="Model providers"
            count={draft ? workflowModelProviders(draft).length : 0}
            expanded={expandedSections.has('modelProviders')}
            onToggle={() => toggleSection('modelProviders')}
            hint="LLM endpoints (local, URL, OpenAI). Assign to Agent and Model nodes on the canvas."
          >
            <ModelProvidersSection />
          </ExpandableSection>

          <BuilderCapabilitiesSection
            catalogTools={catalogTools}
            draft={draft}
            disabled={disabled}
            expanded={expandedSections.has('capabilities')}
            onToggle={() => toggleSection('capabilities')}
          />
          <BuilderAgentsSection
            otherWorkflows={otherWorkflows}
            workflows={workflows}
            draft={draft}
            disabled={disabled}
            expanded={expandedSections.has('agents')}
            onToggle={() => toggleSection('agents')}
          />
          <BuilderHooksSection
            catalogHooks={catalogHooks}
            draft={draft}
            disabled={disabled}
            expanded={expandedSections.has('hooks')}
            onToggle={() => toggleSection('hooks')}
          />
        </div>
      )}
      <button
        type="button"
        className="side-panel-toggle"
        onClick={onToggle}
        title={expanded ? 'Collapse' : 'Expand'}
        aria-label={expanded ? 'Collapse builder panel' : 'Expand builder panel'}
      >
        {expanded ? '<' : <span className="side-panel-collapsed-label">Builder</span>}
      </button>
    </aside>
  )
}
