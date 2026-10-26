<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->
# Layout contract

## Contributor and owner notes

Layout contributors should protect the panel roles and interaction contract so new UI work remains predictable. Owners should review changes for accessibility, responsive behavior, and whether the change belongs in navigation, tools, main content, or properties. Credit layout and UX stewardship in [MODULE_OWNERS.md](MODULE_OWNERS.md).

> **Note:** Navigation was redesigned. Current sections are **Overview, Workflows, Executions, Observability, Extensions, Administration** (see `olo-ui/src/types/layout.ts`). The Build / Run / Investigate / System model below is **legacy** and kept for panel-ownership principles only.

The app layout is fixed. Do not repurpose panels for unrelated concerns.

---

## Panel ownership model

Each panel has a single intent. If any panel answers two of the questions below, UX becomes confusing.

| Panel | Owns | Does NOT | Answers |
|-------|------|----------|---------|
| **Left** | Tenant selection, primary intent (Build / Run / Investigate / System), sub-view selection | Control execution, render data, show tools | **Where am I working?** |
| **Tools** | Mode-specific actions: action triggers, filters, mode controls, selection helpers | Primary workflow, visualization, detail forms | **What can I do here?** |
| **Main** | Primary cognitive focus; always the dominant surface | Secondary workspace | **What am I looking at?** |
| **Properties** | Selected entity detail only | Second canvas, list, primary workflow | **Details of what I selected.** |

---

## Panel roles

| Panel | Role | Do | Do not |
|-------|------|-----|--------|
| **Left** | Primary navigation (tenant, sections, sub-options) | Section list, tenant dropdown, menu expand/collapse | Secondary workspace, ad-hoc tools |
| **Tools** | Contextual utility (quick actions, filters, search, shortcuts, node inspector, logs preview) | Contextual tools for the current view | Primary workflow, entity detail forms |
| **Main** | Primary workflow (current section/view content) | Section-specific content, lists, run selector, primary actions | Use as a secondary workspace |
| **Properties** | Entity detail (selected item’s properties / form) | Tenant form, run details, node properties | Generic workspace, secondary lists |

---

## Rules

1. **Main always renders primary output.** The main content area is the single source of truth for the current task. Do not move primary workflow into Properties or Tools.
2. **Tools panel = contextual utility.** Tools array per sub-option (`SubOption.toolIds`); tool registry map (`config/toolRegistry.ts`). Tool components talk only to the owning store (receive `storeContext`; no API, no other stores).
3. **Properties panel = entity detail.** One selected entity at a time; form or property list. Do not use it as a second canvas or list.
4. **Left panel = navigation only.** No primary content or forms; only navigation and tenant scope.

Locking this contract early keeps UX consistent as features (tree view, timeline, diff, charts) are added.

---

## Contextual tools system

- **Tools array per sub-option:** Each `SubOption` in `SECTIONS` can define `toolIds: string[]`. The Tools panel shows only those tools for the active view.
- **Tool registry map:** `config/toolRegistry.ts` — `toolRegistryMap` (id → metadata), optional `toolComponentRegistry` (id → component). `getToolsForView(sectionId, subId, runSelected)` returns the list for the current view.
- **Tool components:** Host registers components via `registerToolComponent(id, component)`. Components receive `{ context: ToolContext }`; `context.storeContext` is the owning-store slice. Tools must not import API or other domain stores.
- **Main = primary output:** Main content always renders the primary workflow; Tools panel is supplementary.

---

## Flow per mode

| Mode | User intent | Left | Tools | Main | Properties |
|------|-------------|------|-------|------|------------|
| **Build (Canvas)** | Create or modify a pipeline | Build → Canvas | Node palette, shortcuts | Canvas | Selected node properties |
| **Run (Live)** | What is currently happening? | Run → Live Runs | Filters, search | Run list | Selected run summary |
| **Run (run selected)** | Inspect a run | Run → Overview / Tree / Timeline / Debug | Debug controls, breakpoints | Execution tree | Selected node execution state |
| **Investigate** | What happened in the past? | Investigate → Runs | Filters (date, status, cost) | Run list | Selected run summary |
| **Investigate (Diff)** | Compare runs | Investigate → Diff | Run A / Run B selector | Diff canvas | Selected diff node detail |
| **System** | Configure environment | System → Tenant Configuration | (Usually none) | Tenant list | Tenant form |

---

## Golden rule

Each mode must answer clearly:

| Panel | Question |
|-------|----------|
| Left | Where am I? |
| Tools | What can I do here? |
| Main | What am I looking at? |
| Properties | What is selected? |

If any panel answers two of these, UX becomes confusing.
