<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->
# Store discipline: one store per domain

## Contributor and owner notes

Store contributors should keep state scoped by domain and add loading, error, and clearError patterns for API-backed stores. Owners should review new stores carefully and credit sustained state-management stewardship in [../../../docs/MODULE_OWNERS.md](../../../docs/MODULE_OWNERS.md).

**Rule: one store per domain, not per component.**

Stores are scoped by **domain** (runtime, ledger, configuration, etc.), not by UI component. This keeps state meaningful and avoids store explosion.

- **What is a domain?** See **docs/ARCHITECTURE.md** §3 — a domain owns server data lifecycle, has its own loading/error states, spans multiple views, and evolves independently. Do not create per-screen stores (e.g. `treeStore`, `timelineStore`).
- **Store shape:** Every domain store that touches the API must have `loading`, `error`, and `clearError()`. See **docs/ARCHITECTURE.md** §3 (Store shape convention).

## Current stores

| Store | Domain | Purpose |
|-------|--------|---------|
| `ui.ts` | App shell | Panels, navigation (section/sub/run), theme, tenant dropdown |
| `tenantConfig.ts` | Configuration (tenants) | Tenant list, selection, CRUD, loading |
| `workflowConfigurationStore.ts` | Workflows (studio) | Active-folder workflow list, draft editor, save/import/export |
| `configurationFolderStore.ts` | Configuration (scenarios) | Scenario folder catalog, activate into `current-active`, refresh steps |
| `catalogStore.ts` | Studio catalog | Temporal queues, workflow types, extension catalog |
| `graphLogStore.ts` | Workflow log view | Runtime-injected graph JSON under `log/` |

## Future domains (placeholders)

Add **one store per domain** when you implement the feature:

| Store | Domain | Use when building |
|-------|--------|-------------------|
| `runtime.ts` | Runtime | Live runs, queues, metrics, run selection, run-level views |
| `ledger.ts` | Ledger | Historical runs, cost, snapshots, replay, run-level views |
| `runContext.ts` | Run context (shared) | Shared run-level state when same run is shown in Runtime vs Ledger |
| `plugins.ts` | Plugins | Executor registry, plugin metadata |
| `schema.ts` | Studio / schema | Canvas, versions, schema editing, test run results |

Do **not** create stores per component (e.g. `treeViewStore`, `timelineStore`). Prefer one `runtime.ts` or `ledger.ts` that holds run-level view state for that domain.

## Lifecycle, errors, side effects, runContext

- **Stores own data lifecycle.** Polling and stream subscriptions (e.g. runtime live runs) belong in the domain store. Expose subscribe/unsubscribe if needed; do not put streaming or polling logic in App.
- **Domain-specific API errors belong in the owning store.** Each store holds its own `error` (or similar) for API failures; components read from the store. Do not put API errors in the UI store or component state.
- **Side effects only in store actions.** API calls, subscriptions, and timers belong in store actions. Components stay declarative: they read from stores and call store actions; they do not call the API layer directly.
- **runContext** holds summary/normalized data only. Heavy raw data (execution trees, event logs, large snapshots) stays in runtime/ledger store; runContext stays small to avoid re-render and memory issues.

See **docs/ARCHITECTURE.md** (§5–§6, §6b–§6c) and **docs/DOMAIN_BOUNDARIES.md**.

## Usage

- Use Zustand `create()` and export the hook and optionally `getState()` for non-React callers.
- Keep domains meaningful: e.g. `runtime` = everything about “current runtime view” (runs list, selected run, metrics filters).
- Cross-domain data (e.g. “current run”) can live in `ui.ts` (e.g. runId in URL) or in a small `runContext.ts` if both Runtime and Ledger need the same run payload.
- **Scenario activation** (`configurationFolderStore`) may call `catalogStore` and `workflowConfigurationStore` reload actions after activate — orchestration at the action layer, not cross-store imports in components.
