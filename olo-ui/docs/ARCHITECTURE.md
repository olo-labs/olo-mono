<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->
# Olo — Contributor Architecture Guide

## Contributor and owner notes

Architecture contributors should preserve the domain boundaries described here before adding abstractions. Module owners should use this guide as the review baseline for frontend state, backend API shape, extension points, and cross-module changes. Credit substantial architecture stewardship in [MODULE_OWNERS.md](MODULE_OWNERS.md).

This document is for contributors. It explains how the codebase is structured so you can add features without fighting the architecture.

---

## 1. State philosophy

- **URL is source of truth for navigation.** Section, sub-option, run ID, tenant, and panel state (menu/tools/props) come from the URL (query: `tenant`, `menu=0|1`, `tools=0|1`, `props=0|1`; defaults: menu expanded, tools and props collapsed). The app syncs URL → store on load and on history changes; user actions (menu click, run selector) call `navigate()` so the URL updates. No “state-only” navigation.
- **Zustand holds UI and domain state.** One store per **domain** (e.g. `ui`, `tenantConfig`, `runtime`, `ledger`), not per component. See [Store creation guidelines](#3-store-creation-guidelines).
- **No god component.** `App.tsx` does not own all state. It wires URL sync, health polling, and store → components. Domain logic and lists live in stores and API layer.
- **Server state vs UI state.** Data from the backend (tenants, runs, etc.) is fetched in the app or in stores and passed down or read from stores. Transient UI (e.g. “panel open”, “selected tab”) lives in `ui` store or local component state as appropriate.

---

## 1b. State flow (visual model)

Olo follows a strict, predictable state flow. This diagram defines the only allowed data movement paths.

```
┌─────────────────────────────┐
│           URL               │
│ (section, sub, run, tenant) │
└──────────────┬──────────────┘
               │
               ▼
        ┌───────────────┐
        │      App      │
        │ (URL → Store) │
        └───────┬───────┘
                │
                ▼
        ┌───────────────┐
        │    UI Store   │
        │ (navigation)  │
        └───────────────┘


User Interaction Flow:

Component
   │
   ▼
Store Action  ───────────►  API Layer
   │                         (fetch)
   │
   ▼
Store State Update
   │
   ▼
Component Re-render
```

**Rules enforced by this model:**

- URL is the source of truth for navigation.
- Components never call the API directly.
- All side effects live inside store actions.
- Stores do not import other domain stores.
- Cross-domain sharing happens only via URL or runContext.

**This guarantees:**

- Predictable data flow
- Testable boundaries
- No hidden side effects
- No circular dependencies
- No god component

---

## 2. Naming conventions

| Kind | Convention | Example |
|------|------------|--------|
| **Components** | PascalCase, one per file | `TenantConfigurationList.tsx`, `ToolsPanel.tsx` |
| **Stores** | camelCase file, `useXxxStore` hook | `store/tenantConfig.ts` → `useTenantConfigStore` (or `tenantConfigStore` as the created store) |
| **Routes / path helpers** | camelCase functions, UPPER for constants | `parsePath()`, `buildPath()`, `DEFAULT_PATH` |
| **CSS classes** | BEM-like, kebab-case | `.tenant-config-list`, `.tenant-config-list-item`, `.main-content-header` |
| **Types** | PascalCase | `Tenant`, `SectionId`, `TenantConfigFormProps` |
| **API functions** | camelCase, verb-first | `getTenants()`, `saveTenant()`, `deleteTenant()` |
| **Event handlers (props)** | `on` + PascalCase or `handle` + PascalCase | `onSelectTenant`, `onRunIdChange`, `handleSaveTenant` |

Stories: `ComponentName.stories.tsx`, title in Storybook like `"Section/ComponentName"` (e.g. `Configuration/TenantConfigurationList`).

---

## 3. Store creation guidelines

### What is a domain?

A domain in Olo is **not a screen**. A domain is a **bounded context** that:

- **Owns server-backed data lifecycle** (fetching, polling, streaming)
- Has its **own loading and error state**
- **Spans multiple views or sub-options**
- **Evolves independently** from other areas
- **Requires isolation** to prevent architectural erosion

**Examples of valid domains:**

| Domain | Scope |
|--------|--------|
| **runtime** | Live runs, queues, metrics |
| **ledger** | Historical runs, replay, cost |
| **tenantConfig** | Tenant CRUD lifecycle |
| **schema** | Pipeline versions, validation, test runs |

**Not valid domains:**

- ❌ treeViewStore
- ❌ timelineStore
- ❌ diffStore
- ❌ panelStore
- ❌ runSelectorStore

These are views or features within a domain, not domains themselves.

**Rule:** If a feature only affects one view, does not own independent server lifecycle, or does not need cross-view state, it should use **local state** or **extend the existing domain store** — not create a new store. This prevents store explosion and cognitive fragmentation.

### 3b. Standard store shape

All domain stores must follow a consistent structure.

```ts
type DomainState<T> = {
  // Data
  data: T
  loading: boolean
  error: Error | null

  // Actions
  load(): Promise<void>
  clearError(): void
}
```

**Required fields:** Every domain store must define:

- `loading: boolean`
- `error: Error | null`
- `clearError(): void`

**Why?**

- Consistent loading UX
- Centralized error handling
- Easier test patterns
- Predictable contributor expectations

**Example (tenantConfig):**

```ts
type TenantConfigState = {
  tenants: Tenant[]
  selectedTenantId: string | null
  loading: boolean
  error: Error | null

  loadTenants(): Promise<void>
  selectTenant(id: string): void
  saveTenant(data: TenantInput): Promise<void>
  deleteTenant(id: string): Promise<void>
  clearError(): void
}
```

**Explicit rule:** No store should invent a radically different structure. If a domain needs additional flags (e.g. `isStreaming`), they must be domain-specific and justified.

### No global derived selectors (heavy computation)

Stores can accumulate derived getters (`getFilteredRuns()`, `getComputedTree()`, etc.), which lead to hidden computation, implicit dependencies, and performance unpredictability.

**Rule:** Derived or heavy computations must be **memoized in component scope** (e.g. `useMemo`, `useSelector`-style selectors) or via **selector-based subscription** — not stored as derived state inside the store. Stores stay pure: raw/source data + actions. Components (or thin selector hooks) own derivation.

For **Runtime vs Ledger** — conceptual separation (live/streaming vs immutable/historical), see **docs/DOMAIN_BOUNDARIES.md**.

### One store per domain

- **One store per domain.** Domains are: app shell (ui), tenant configuration (tenantConfig), runtime, ledger, run context, plugins, schema. Do **not** add a store per screen or per component (e.g. no `treeViewStore`, `timelineStore`).
- **When to add a new store:** When you introduce a **new domain** (e.g. “runtime” or “ledger”) with its own list/selection/loading/actions. If the feature is just a new screen under an existing domain, extend the existing store or use local state plus URL.
- **Where to put state:**  
  - **Navigation (section, sub, run, tenant in URL):** `ui` store, kept in sync with URL.  
  - **Tenant list, selection, CRUD:** `tenantConfig` store.  
  - **Runtime live runs, queues, metrics, run selection:** `runtime` store (when implemented).  
  - **Ledger runs, cost, replay, run selection:** `ledger` store (when implemented).  
  - **Shared run payload across Runtime/Ledger:** `runContext` store (optional).  
- **Placeholder stores** exist under `store/` (e.g. `runtime.ts`, `ledger.ts`) so the “one store per domain” pattern is clear. Implement them when you build those features.
- See `store/README.md` for the full list and rule.

---

## 4. When to use store vs local state

| Use store (Zustand) | Use local state (useState / useReducer) |
|---------------------|----------------------------------------|
| Data or UI state shared across multiple screens or components | State used only inside one component (e.g. form fields, modal open, dropdown open) |
| State that should survive navigation (and is reflected in URL or persisted) | Transient UI (tooltips, context menu position, “saving” flag for one form) |
| Domain data loaded from API and used in several places (e.g. tenant list) | Draft values in a form until submit |
| Panel open/closed, theme, selected tenant in dropdown | Focus, hover, local validation errors |

**Rule of thumb:** If only one component cares about it and it doesn’t need to be in the URL or shared, keep it local. If two or more components or the URL need it, put it in the right store (or in the URL and sync to store).

---

## 5. Data lifecycle: stores own it

**Stores own data lifecycle (including polling and stream subscriptions). App does not manage domain data lifecycle.**

- **Polling/streaming:** If runtime needs live runs or streaming updates, the **runtime store** owns the lifecycle: start/stop polling, subscribe/unsubscribe to streams. The store exposes `subscribe()` / `unsubscribe()` (or equivalent) if needed. App does not run `setInterval` or hold streaming logic for domain data.
- **App’s role:** App wires URL sync, health check (app-level bootstrap), and feature flags. It does not fetch, poll, or stream on behalf of domain stores. Components or stores trigger load; stores own the subscription.
- **Rule:** Domain store owns the lifecycle of its data. App stays thin.

---

## 6. Error ownership: domain store

**Domain-specific API errors belong in the owning domain store.**

- When an API call fails (e.g. fetch tenants, fetch runs), the **error** and **error state** live in the **domain store** that made the call (e.g. `tenantConfig` for tenant API errors, `runtime` for runtime API errors). Not in the UI store, not in component local state.
- **Rule:** Each domain store holds `error: Error | null` (or similar) for its own API failures. Components read error from the store and display it (or use `<ErrorState />`). This keeps error handling from scattering and gives one place to clear/retry per domain.

---

## 6b. runContext: summary only, no heavy payloads

**runContext must hold normalized or summary data only, not raw unbounded event logs.** Heavy payloads (MB-level execution trees, event streams, snapshots) cause re-render cascades and memory spikes if stored in shared context. Rule: **heavy raw data stays in runtime/ledger store; runContext stores summary only** (e.g. `runId`, run status, small shared summary). See **docs/DOMAIN_BOUNDARIES.md** for full runContext rules.

---

## 6c. Side effects: store actions only (explicit rule)

**Stores own side effects; components remain declarative.**

- **All API calls must live inside store actions.** Components only call store methods (e.g. `saveTenant(data)`); they do not import or call the API layer directly.
- **No component directly calls API** — no `getTenants()`, `fetch()`, or other API imports in `src/components/**`. Use `types/tenant` for types and `lib/tenantDisplay` for display helpers.
- **No store imports another store** — domain isolation. Cross-domain data flows via URL or `runContext` only. Enforced by ESLint (see `eslint.config.js`).
- **No cross-domain selectors in feature components** — combining `useRuntimeStore()` and `useLedgerStore()` (or any two domain stores) in one component re-couples domains at the UI level. Cross-domain state combination is allowed only in App or route-level container components; domain feature components subscribe to at most one domain store. See **docs/DOMAIN_BOUNDARIES.md**.
- This locks the architecture: single place for side effects, predictable data flow, easier testing.

---

## 7. Where things live

| What | Where |
|------|--------|
| Route parsing, path building | `src/routes.ts` |
| Panel/nav/theme/tenantId (from URL; panel state in query: menu, tools, props) | `store/ui.ts` |
| Tenant list, selection, CRUD | `store/tenantConfig.ts` |
| Workflow presets (active folder), builder draft | `store/workflowConfigurationStore.ts` |
| Scenario folder catalog + activate | `store/configurationFolderStore.ts` |
| Studio catalog (queues, types) | `store/catalogStore.ts` |
| API calls | `src/api/rest.ts`, `src/api/restConfiguration.ts`, `src/api/restConfigurationFolders.ts` |
| Section/sub-option definitions | `src/types/layout.ts` |
| Tools panel items | `src/types/tools.ts` |
| Feature CSS | `src/styles/<feature>.css` (imported from `index.css`) |
| Components | `src/components/` |
| Stories (UI in isolation) | `src/**/*.stories.tsx` |

---

## 8. UI development without backend (Storybook)

To work on UI without running the backend or Redis:

1. Run **Storybook:** `npm run storybook` (port 6006).
2. Open stories for **TenantConfigurationList**, **TenantConfigForm**, **ToolsPanel**. They use mock data and callbacks; no API or store required.
3. Add new stories under `src/**/*.stories.tsx` for new components so contributors can develop and review UI in isolation.

This reduces friction for OSS contributors who don’t have Redis or the backend running.

---

## 9. Backend: first-run and Redis

- The backend supports **in-memory fallback** for the tenant list when Redis is disabled or unavailable. No Redis is required for first-run or local contribution to the API; tenants are stored in memory and lost on restart.
- For production or persistent tenants, configure Redis and set `OLO_TENANT_IDS` (e.g. `olo:tenants`). **Full env reference (local, Docker, Compose):** see **docs/ENVIRONMENT.md**. See also `olo-be` README and `application.properties`.

---

## 10. Feature flags, observability, extensions, auth

- **Feature flags:** `config/features.ts` and `useFeature(id)`, `useVisibleSections()`. Gate sections and sub-features (e.g. experiments, costAnalysis, replay). Use for OSS vs enterprise and gradual rollout. **Sunset rule:** every flag must have an owner and a removal target version (or milestone); document in code or `FEATURE_FLAG_META` so flags are reviewed and removed. See **docs/EXTENSIBILITY.md**.
- **Observability:** `lib/observability.ts` — `logEvent(name, props)`, `logError(error, context)`. Use for navigation and errors; swap to real analytics later.
- **Extensions:** `config/extensions.ts` — `registerSidebarItem()`, `registerTool()`. Define how plugins extend UI; implement later.
- **Auth:** `auth/useAuth()` — `hasPermission('deleteTenant')` etc. Scaffold only; replace with real RBAC when needed.
- **Domain boundaries:** See **docs/DOMAIN_BOUNDARIES.md** — stores must not import other domain stores; runContext scope and size (summary only); side effects in store actions only; code review checklist.
- **Extensibility & governance:** See **docs/EXTENSIBILITY.md** — declarative extension API only, domain leakage risk, runContext not a dumping ground, feature-flag rules.
- **Layout contract:** See **docs/LAYOUT_CONTRACT.md** — Tools = utility, Properties = entity detail, Main = workflow.
- **Stability:** See **docs/STABILITY.md** — versioning and deprecation.
- **Tests:** See **docs/TEST_STRATEGY.md** — stores, forms, routing; no snapshot-heavy tests.
- **Performance:** See **docs/PERFORMANCE.md** — any list potentially exceeding 100 items must be implemented virtualized by default (hard law).

---

## 10b. Migration rule for breaking architecture

API deprecation is covered in **docs/STABILITY.md**. For **architecture** changes (e.g. splitting or merging domains, changing store boundaries):

**If a domain boundary must change, introduce an intermediate adapter layer and migrate gradually. Do not directly couple domains.**

- Prefer facades, adapters, or thin bridges so existing consumers keep working while new code uses the target shape.
- Migrate call sites incrementally; remove the adapter once migration is complete.
- This keeps refactors clean and avoids big-bang rewrites or accidental cross-domain coupling.

---

## 11. Summary

- **State:** URL for navigation; Zustand per domain; no god component. See [State flow (visual model)](#1b-state-flow-visual-model).  
- **Naming:** PascalCase components/types; camelCase stores and API; kebab-case CSS; `on*` / `handle*` for handlers.  
- **Stores:** One per domain; domain = owns server lifecycle, own loading/error, spans views, independent evolution. Domain stores: `loading`, `error`, `clearError()`. Runtime = live/streaming; Ledger = immutable/historical.  
- **Architecture changes:** Use adapter layer and gradual migration; do not directly couple domains.  
- **Data lifecycle:** Stores own polling/stream subscriptions; App does not manage domain data lifecycle.  
- **Errors:** Domain-specific API errors live in the owning domain store, not in UI store or component state.  
- **runContext:** Summary/normalized data only; heavy raw data stays in runtime/ledger.  
- **Side effects:** API and subscriptions only in store actions; components stay declarative, no direct API calls.  
- **Cross-domain selectors:** Combine domain stores only in App or route-level containers; domain feature components subscribe to at most one domain store.  
- **Derived state:** Heavy computations memoized in component scope or selectors; not stored as derived state in stores.  
- **Extensions:** May not mutate store state directly; only call public store actions.  
- **Feature flags:** Sunset rule — every flag has owner and removal target version.  
- **Local vs store:** Store for shared or URL-backed state; local for single-component transient UI.  
- **UI in isolation:** Use Storybook and mock data; no backend/Redis required for component work.  
- **Backend:** In-memory tenant store when Redis is off; API versioned at `/api/v1`.
