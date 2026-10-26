/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Register host-owned tool components. Run once at app init.
 * Tool components receive only ToolContext (storeContext = owning-store slice); they must not import API or other stores.
 */
import { registerToolComponent } from './toolRegistry'
import { SearchTool } from '../components/tools/SearchTool'

export function registerDefaultToolComponents(): void {
  registerToolComponent('search', SearchTool)
}
