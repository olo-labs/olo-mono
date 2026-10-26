/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
/**

 * Feature flags: gradual rollout, OSS vs enterprise, hidden experimental domains.

 * Toggle sections and sub-features without forking. Use with useFeature().

 */

export const features = {

  overview: true,

  workflows: true,

  executions: true,

  observability: true,

  extensions: true,

  administration: true,



  /** Sub-features */

  tenantConfiguration: true,

  workflowConfiguration: true,

  scenarioConfiguration: true,

  costAnalysis: false,

  replay: false,

} as const



export type FeatureId = keyof typeof features



export const FEATURE_FLAG_META: Partial<Record<FeatureId, { owner: string; removeBy: string }>> = {

  overview: { owner: 'platform', removeBy: 'v1.1' },

  workflows: { owner: 'platform', removeBy: 'n/a (core)' },

  executions: { owner: 'platform', removeBy: 'v1.1' },

  observability: { owner: 'platform', removeBy: 'v1.1' },

  extensions: { owner: 'platform', removeBy: 'v1.1' },

  administration: { owner: 'platform', removeBy: 'n/a (core)' },

  tenantConfiguration: { owner: 'platform', removeBy: 'n/a (core)' },

  workflowConfiguration: { owner: 'platform', removeBy: 'n/a (core)' },

  scenarioConfiguration: { owner: 'platform', removeBy: 'n/a (core)' },

  costAnalysis: { owner: 'TBD', removeBy: 'TBD' },

  replay: { owner: 'TBD', removeBy: 'TBD' },

}



export function isFeatureEnabled(id: FeatureId): boolean {

  return Boolean(features[id])

}


