/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
/**

 * v1 product navigation — stable section IDs for URLs; labels are display-only.

 */

export type SectionId =

  | 'overview'

  | 'workflows'

  | 'executions'

  | 'observability'

  | 'extensions'

  | 'administration'



export type SectionStatus = 'available' | 'coming-soon' | 'partial'



export interface SubOption {

  id: string

  label: string

  description?: string

  status?: 'available' | 'coming-soon'

  featureId?: keyof typeof import('../config/features').features

  /** Show catalog Components panel (builder). */

  componentsPanel?: boolean

  /** Badge text for coming-soon sub-options (default: Scheduled). */

  comingSoonLabel?: string

  /** Legacy contextual tool IDs (non-builder views). */

  toolIds?: string[]

}



export interface SectionConfig {

  id: SectionId

  label: string

  emoji: string

  status: SectionStatus

  subtitle: string

  /** Badge text when status is coming-soon (default: Scheduled). */

  comingSoonLabel?: string

  subOptions: SubOption[]

  /** Future: run-level tabs when a run is selected (Executions, Observability). */

  runSelectedOptions?: SubOption[]

  /** Default sub-route when the section path omits a sub-segment. */

  defaultSubId?: string

}



export const SECTIONS: SectionConfig[] = [

  {

    id: 'overview',

    label: 'Overview',

    emoji: '🏠',

    status: 'coming-soon',

    comingSoonLabel: 'SCHEDULED-V1',

    subtitle: 'Product dashboard',

    subOptions: [],

  },

  {

    id: 'workflows',

    label: 'Workflows',

    emoji: '📋',

    status: 'available',

    subtitle: 'Design and manage workflows',

    defaultSubId: 'builder',

    subOptions: [

      {

        id: 'agents',

        label: 'Agents',

        description: 'Import, export, and manage workflow JSON',

        status: 'available',

        featureId: 'workflowConfiguration',

      },

      {

        id: 'log',

        label: 'Log',

        description: 'View runtime-injected workflow graphs (read-only)',

        status: 'available',

        featureId: 'workflowConfiguration',

      },

      {

        id: 'builder',

        label: 'Builder',

        description: 'Visual workflow editor',

        status: 'available',

        componentsPanel: true,

      },

      {

        id: 'debugger',

        label: 'Debugger',

        description: 'Step through workflow runs and inspect state',

        status: 'coming-soon',

        comingSoonLabel: 'SCHEDULED-V3',

      },

      {

        id: 'templates',

        label: 'Templates',

        description: 'Workflow templates',

        status: 'coming-soon',

        comingSoonLabel: 'SCHEDULED-V5',

      },

      {

        id: 'versions',

        label: 'Versions',

        description: 'Workflow version history',

        status: 'coming-soon',

        comingSoonLabel: 'SCHEDULED-V6',

      },

    ],

  },

  {

    id: 'executions',

    label: 'Executions',

    emoji: '⚡',

    status: 'coming-soon',

    comingSoonLabel: 'SCHEDULED-V4',

    subtitle: 'Live and recent runs',

    subOptions: [],

  },

  {

    id: 'observability',

    label: 'Observability',

    emoji: '📊',

    status: 'coming-soon',

    comingSoonLabel: 'SCHEDULED-V4',

    subtitle: 'Metrics, logs, and traces',

    subOptions: [],

  },

  {

    id: 'extensions',

    label: 'Extensions',

    emoji: '🔌',

    status: 'coming-soon',

    comingSoonLabel: 'SCHEDULED-V5',

    subtitle: 'Plugins and integrations',

    subOptions: [],

  },

  {

    id: 'administration',

    label: 'Administration',

    emoji: '⚙️',

    status: 'partial',

    subtitle: 'Tenants and platform settings',

    subOptions: [

      {

        id: 'tenants',

        label: 'Tenants',

        description: 'Manage tenants',

        status: 'available',

        featureId: 'tenantConfiguration',

      },

      {

        id: 'scenarios',

        label: 'Scenarios',

        description: 'Activate workflow configuration folders into current-active',

        status: 'available',

        featureId: 'scenarioConfiguration',

      },

    ],

  },

]



export function getSection(sectionId: SectionId): SectionConfig | undefined {

  return SECTIONS.find((s) => s.id === sectionId)

}



export function getSubOption(sectionId: SectionId, subId: string): SubOption | undefined {

  const section = getSection(sectionId)

  if (!section) return undefined

  return section.subOptions.find((s) => s.id === subId)

}



export function getSectionDefaultSubId(sectionId: SectionId): string {

  const section = getSection(sectionId)

  if (!section) return ''

  if (section.defaultSubId) return section.defaultSubId

  return section.subOptions[0]?.id ?? ''

}



export function resolveSubId(sectionId: SectionId, subId: string): string {

  if (sectionId === 'workflows' && subId === 'import-export') {

    return 'agents'

  }

  return subId

}



export function sectionIsComingSoon(section: SectionConfig): boolean {

  return section.status === 'coming-soon'

}



export function subOptionIsComingSoon(sub: SubOption): boolean {

  return sub.status === 'coming-soon'

}


