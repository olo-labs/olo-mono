<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->
# Domain boundaries

## Contributor and owner notes

Use this document when a change touches more than one domain store, API surface, or runtime concept. Contributors should explain the boundary they are changing in the PR; owners should review for coupling, migration safety, and contributor learnability. Ownership credit lives in [MODULE_OWNERS.md](MODULE_OWNERS.md).

Domains are implemented as Zustand stores and own specific UI and data. To avoid circular logic and cognitive collapse, **domains may depend only on**:

- **API layer** (`api/rest.ts` or equivalent)
- **Shared types** (`types/*`)
- **runContext** (if explicitly shared between Runtime and Ledger)

**Domains should not import each other's stores.** Orchestration at the action layer is acceptable (e.g. `configurationFolderStore` reloads catalog and workflow configuration after activation). Examples of what to avoid:

- ❌ `runtimeStore` importing `ledgerStore`
- ❌ `ledgerStore` importing `runtimeStore`
- ❌ `tenantConfigStore` importing `runtimeStore`

---

## Runtime vs Ledger — conceptual separation

Although Runtime and Ledger both deal with runs, they are philosophically different systems.

**Runtime**

- Live, mutable, streaming system.
- **Focus:** What is happening now?
- **Data:** Active runs, in-progress execution trees, live metrics
- **Nature:** Dynamic, real-time, may change second-to-second
- **Lifecycle:** Polling, streaming, subscription-based
- **Ownership:** runtime store owns streaming lifecycle
- Runtime is concerned with **operational state**.

**Ledger**

- Historical, immutable, replayable system.
- **Focus:** What happened?
- **Data:** Completed runs, cost summaries, snapshots, diff comparisons
- **Nature:** Immutable after completion
- **Lifecycle:** Fetch-on-demand, paginated queries
- **Ownership:** ledger store owns historical data lifecycle
- Ledger is concerned with **audit and investigation**.

**Why separation matters**

Without this separation:

- Streaming logic bleeds into historical queries
- Replay logic pollutes live run handling
- Cost analysis invades operational views
- Store coupling emerges

**The rule is strict:**

- Runtime never imports Ledger.
- Ledger never imports Runtime.
- Shared identity flows only via **URL** (`runId`) or **runContext** (summary only).

This prevents conceptual and architectural collapse.

---

## Domain responsibility and access

| Domain (store) | Owns | May use | Must not use |
|----------------|------|--------|--------------|
| **ui** | Panels, navigation (section/sub/run), theme, tenant dropdown | — | Other domain stores |
| **tenantConfig** | Tenant list, selection, loading, CRUD actions | `api/rest` | runtime, ledger, plugins, schema |
| **workflowConfiguration** | Workflow preset draft, save/reload | `api/rest`, `catalogStore` (read-only metadata) | runtime, ledger |
| **configurationFolder** | Scenario folder list and activation | `api/rest`; orchestrates reload via `catalogStore` / `workflowConfigurationStore` actions | — |
| **catalog** | Merged extension catalog from olo-be | `api/rest` | other domain stores |
| **graphLog** | Read-only runtime graph injection logs | `api/rest` | other domain stores |
| **runtime** | Live runs, queues, metrics, run-level view state | `api/rest`, `runContext` (optional) | ledger, tenantConfig, plugins, schema |
| **ledger** | Historical runs, cost, snapshots, replay, run-level view state | `api/rest`, `runContext` (optional) | runtime, tenantConfig, plugins, schema |
| **runContext** | Shared run payload for Runtime/Ledger | `api/rest` | runtime, ledger (no store imports) |
| **plugins** | Executor registry, plugin metadata | `api/rest` | runtime, ledger, tenantConfig, schema |
| **schema** | Canvas, versions, schema, test run results | `api/rest` | runtime, ledger, tenantConfig, plugins |

Cross-domain data (e.g. “current run”) flows via **URL** (e.g. `runId` in path) or **runContext** (shared run payload), not by one store importing another.

---

## No cross-domain selectors in feature components

Store imports are already blocked between domains. A separate risk is **combining multiple domain stores in one component**:

```ts
// Avoid in domain feature components (e.g. TenantConfigurationList, RunTreeView):
const runtimeState = useRuntimeStore()
const ledgerState = useLedgerStore()
// ... mixing logic from both
```

That re-couples domains at the UI level over time.

**Rule:** Cross-domain state combination must happen only in **App** or **route-level container components**, not inside domain feature components. Domain feature components should subscribe to at most one domain store (plus `ui` if needed for layout). If a view truly needs data from two domains, the container (e.g. App or a route wrapper) should read both and pass down only what the child needs as props.

---

## runContext: narrow responsibility and size

**runContext** is the only shared domain. It must contain only:

- **Run identity** (`runId`) and **shared run payload** (e.g. run summary) needed by both Runtime and Ledger when viewing the same run.

**runContext must hold normalized or summary data only, not raw unbounded event logs.** When execution trees and snapshots grow large (MB-level payloads), putting them in runContext risks React re-render cascades and memory spikes. Rule:

- **Heavy raw data** (full execution trees, event streams, MB-level snapshots) stays in the **runtime** or **ledger** store.
- **runContext** stores **summary only** (e.g. `runId`, run status, small shared summary object). Each domain store fetches and holds its own full payload when needed.

**Do not put in runContext:**

- Feature flags, generic UI state, or domain-specific state. Those belong in `ui` or the owning domain store.
- Raw unbounded data (full event logs, full execution trees, large snapshots). Those belong in runtime/ledger; runContext stays small.

---

## Code review checklist

Before merging code that touches stores or domains:

- ❓ **Does this store import another store?** If yes, it violates domain boundaries unless it is the allowed shared store (runContext).
- ❓ **Does this violate DOMAIN_BOUNDARIES.md?** Cross-domain data must flow via URL or runContext only.
- ❓ **Does runtime (or any domain) store own its streaming lifecycle?** Polling/streaming should live in the domain store with subscribe/unsubscribe; not in App.
- ❓ **Are API errors owned by the domain store that made the call?** Domain-specific errors belong in the owning store, not in UI store or component state.
- ❓ **Is runContext kept small?** Only summary/normalized data; heavy raw data stays in runtime/ledger.
- ❓ **Are side effects (API, subscriptions) only in store actions?** Components should not call the API layer directly.
- ❓ **Does a domain feature component subscribe to more than one domain store?** Cross-domain combination belongs in App or route-level containers only.

**Optional lint (ESLint):** To enforce “no store imports another store”, use `no-restricted-imports` and restrict imports from `../store/*` (or similar) inside `store/` except for `runContext`. Example (adjust paths to your layout):

```json
{
  "rules": {
    "no-restricted-imports": ["error", {
      "patterns": [{
        "group": ["../runtime", "../ledger", "../tenantConfig", "../plugins", "../schema"],
        "message": "Domain stores must not import other domain stores. Use URL or runContext for cross-domain data."
      }]
    }
  }
}
```

---

## Data lifecycle and error ownership

- **Stores own data lifecycle.** Polling and stream subscriptions (e.g. runtime live runs) are owned by the domain store. The store exposes subscribe/unsubscribe if needed. App does not manage domain data lifecycle — no `setInterval` or streaming logic in App for domain data. Keeps App thin.
- **Domain-specific API errors belong in the owning domain store.** When an API call fails, error state lives in the store that made the call (e.g. `tenantConfig` for tenant errors, `runtime` for runtime errors). Not in the UI store, not in component local state. One place per domain to clear/retry.

---

## Side effects: store actions only (explicit rule)

**Stores own side effects; components remain declarative.**

- **Do:** Components read from stores and call store actions (e.g. `tenantConfigStore.loadTenants()`). Store actions perform `fetch` / API calls and update store state.
- **Do not:** Call the API layer directly from components (e.g. no `getTenants()` inside `useEffect`). No imports from `api/rest` in `src/components/**` — use `types/tenant` and `lib/tenantDisplay` instead. No `setInterval` or WebSocket setup in components for domain data.
- **No store imports another store** — domain isolation. Enforced by ESLint (`eslint.config.js` overrides for `src/components/**` and `src/store/**`). Run `npx eslint src/components src/store`.
- **Why:** Locks the architecture: single place for side effects, predictable data flow, easier testing.

---

## Rule summary

1. **Stores do not import other domain stores.**
2. **Stores may import:** `api` layer, `types`, and optionally `runContext` for shared run state.
3. **App / routes** wire stores to components; they may read from multiple stores. App does not own domain data lifecycle (polling/streaming).
4. **Domain stores own their data lifecycle and their API errors.**
5. **runContext** holds summary/normalized data only; heavy raw data stays in runtime/ledger.
6. **Side effects** (API, subscriptions, timers) live in store actions only; components do not call the API directly.
7. **New domains** get their own store and follow the same rule.

This keeps the architecture from eroding as the codebase grows.
