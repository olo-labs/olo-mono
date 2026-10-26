/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { CatalogComponentBase, StudioCatalog } from '../types/catalog'



export interface CatalogComponentGroup {

  id: 'nodes' | 'hooks'

  label: string

  items: CatalogComponentBase[]

}



export function catalogComponentGroups(catalog: StudioCatalog | null): CatalogComponentGroup[] {

  if (!catalog) return []



  const groups: CatalogComponentGroup[] = []



  if (catalog.nodes?.length) {

    groups.push({ id: 'nodes', label: 'Nodes', items: catalog.nodes })

  }



  if (catalog.hooks?.length) {

    groups.push({ id: 'hooks', label: 'Hooks', items: catalog.hooks })

  }



  return groups

}
