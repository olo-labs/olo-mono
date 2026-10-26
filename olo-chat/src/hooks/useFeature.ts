/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { useMemo } from 'react'
import { isFeatureEnabled, type FeatureId } from '../config/features'
import { SECTIONS, type SectionConfig } from '../types/layout'

/**
 * Hook to gate UI by feature flag. Enables gradual rollout, OSS vs enterprise builds, hidden experimental domains.
 */
export function useFeature(id: FeatureId): boolean {
  return useMemo(() => isFeatureEnabled(id), [id])
}

/** Sections visible in the left panel based on feature flags. */
export function useVisibleSections(): SectionConfig[] {
  return useMemo(
    () => SECTIONS.filter((s) => isFeatureEnabled(s.id as FeatureId)),
    []
  )
}
