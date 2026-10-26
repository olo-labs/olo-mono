<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->
# Olo — UI/UX and Implementation

## Contributor and owner notes

UI contributors should preserve the product workflows while improving clarity, accessibility, empty states, and reviewability. Owners should make sure design changes are reflected in Storybook or screenshots when useful. Credit design and implementation stewardship in [MODULE_OWNERS.md](MODULE_OWNERS.md).

This document describes the current user interface, user experience, and technical implementation of Olo (frontend: olo-ui, backend: olo-be). For contributor guidance (state philosophy, naming, stores, when to use store vs local state), see **[ARCHITECTURE.md](./ARCHITECTURE.md)**.

---

## 1. Overview

- **Olo** (frontend): React + TypeScript + Vite. Runs on port 3000. Proxies `/api` to the backend.
- **olo-be**: Spring Boot REST backend. Runs on port 8082. Serves APIs and reads/writes tenant data from Redis (key configurable via env **OLO_TENANT_IDS**). Full env reference: **docs/ENVIRONMENT.md**.

The UI is a single-page application with a fixed top bar, an expandable left navigation (tenant + menu), an optional tools strip, a main content area, and an optional properties panel on the right.

### Current navigation (implemented)

Left panel sections (see `types/layout.ts`):

| Section | Sub-options (available) | Notes |
|---------|------------------------|-------|
| **Workflows** | Agents, Log, Builder | Builder has canvas + run/cancel/refresh toolbar |
| **Administration** | Tenants, **Scenarios** | Scenarios activates `olo-configuration` folders into `current-active` |
| Overview, Executions, Observability, Extensions | — | Coming soon placeholders |

**Key flows:**

- **Administration → Scenarios** — list scenario folders (excluding `current-active`); **Activate** copies into `current-active` and refreshes worker + studio (`configurationFolderStore`, `POST /api/v1/configuration/folders/{id}/activate`).
- **Workflows → Builder** — edit/run workflows; **Refresh stack** (`POST /api/v1/system/refresh`); **Cancel** on in-progress runs.
- **Workflows → Agents** — import/export/reload workflow JSON from the active folder.

Older sections below (Build / Run / Investigate / System) describe a prior layout and are kept for reference where not yet rewritten.

---

## 2. UI/UX Layout

### 2.1 High-level structure

```
┌─────────────────────────────────────────────────────────────────────────┐
│  Top bar (Olo branding)                                                  │
├──────────┬─────────┬──────────────────────────────┬─────────────────────┤
│ Left     │ Tools   │ Main content                 │ Properties          │
│ panel    │ panel   │ (section-specific view)      │ panel               │
│ (menu +  │ (hidden │                              │ (collapsed by       │
│ tenant)  │ in      │                              │ default)            │
│          │ tenant  │                              │                     │
│          │ config) │                              │                     │
└──────────┴─────────┴──────────────────────────────┴─────────────────────┘
```

- **Top bar**: Brand name “Olo” and a **theme toggle** (☀/☽) for light/dark. Theme preference is persisted in `localStorage` (`olo-theme`).
- **Left panel**: Tenant dropdown at top; below it, expandable menu categories with sub-options. Collapsible to a thin strip showing vertical “MENU”.
- **Tools panel**: Expandable left strip labeled “TOOLS”. **Hidden** when the user is on **System → Tenant Configuration**.
- **Main content**: Shows the view for the selected section and sub-option. **Navigation is URL-driven** (see §5.6): every view has a URL (e.g. `/runtime/live-runs`, `/configuration/tenant-configuration`, `/ledger/run/123/time-travel`), so links are shareable and back/forward work.
- **Properties panel**: Right-hand strip. **Collapsed by default**. Expands when the user selects a tenant or clicks “+” in Tenant configuration. Collapses again when the user selects any other section in the left menu.

### 2.2 Left panel (navigation)

- **Top**: **Tenant** dropdown. Options come from the backend (Redis key from `OLO_TENANT_IDS`). Display: tenant name if non-empty, otherwise tenant id (e.g. UUID).
- **Collapsed state**: Very thin strip (~18px) with vertical text “MENU” and letter-spacing so characters don’t overlap. No padding; centered. Click to expand.
- **Expanded state**: List of categories. Each category can be expanded/collapsed to show sub-options. Clicking a sub-option loads that view in the main content.

**Categories and sub-options**

| Category       | Sub-options (main)                                      | Run-level sub-options (when a run is selected)     |
|----------------|----------------------------------------------------------|----------------------------------------------------|
| **Build**      | Canvas, Versions, Validate, Test Run, Schema            | —                                                  |
| **Run**        | Live Runs, Queues, Metrics                              | Overview, Tree View, Timeline, Debug, Compare      |
| **Investigate**| Runs, Replay, Cost Analysis, Diff, Snapshots            | Summary, Execution Tree, Time Travel, Raw Events, Diff |
| **System**     | Tenant Configuration, Environment, Secrets, Plugins, Global Settings | —                                                  |

- **Run-level options**: For Run and Investigate, when a run is selected (via run selector in main content for “Live Runs” or “Runs”), the left panel shows the run-level sub-options instead of the main list.
- **Collapse control**: When the left panel is expanded, a small “&lt;” button at the bottom-right collapses the panel.
- **Right-click context menu**: Right-click on a category or on the menu area opens a context menu. On a **category**: **Collapse** (this category), **Expand** (this category), **Collapse all**, **Expand all**. On the **menu area** (not on a category): **Collapse all**, **Expand all** only.
- **Tooltips**: Category headers show a tooltip with the section label and subtitle (e.g. “Build: Create or modify pipelines”). Sub-options show a tooltip with a short description (e.g. “Design and edit pipelines” for Canvas); descriptions are defined in `types/layout.ts` per sub-option.

### 2.3 Tools panel

- **Collapsed**: Thin strip with vertical “TOOLS”. Expand control “&lt;” when expanded (bottom-right of strip).
- **Expanded**: Title “TOOLS” and **contextual tools** for the current view (section + sub-option). Tools are defined per sub-option via `SubOption.toolIds` in `types/layout.ts` and resolved from the **tool registry map** (`config/toolRegistry.ts`). Tool components receive only the **owning-store** slice (`storeContext`); they do not call API or other stores. Main always renders primary output; Tools panel is supplementary. See **docs/LAYOUT_CONTRACT.md** (§ Contextual tools system). When a view has no tools, the panel shows “No tools for this view”.
  - **Quick actions** — Run, pause, cancel
  - **Filters** — Scope by status, time, tags
  - **Search** — Find runs, nodes, logs
  - **Shortcuts** — Keyboard & command palette
  - **Node inspector** — Inspect selected node
  - **Logs preview** — Tail logs for selection
- Width ~220px. Visibility: not rendered when the current view is **System → Tenant Configuration**.

### 2.4 Properties panel

- **Collapsed**: Thin strip with vertical “PROPERTIES”. Expand control “&gt;” when expanded (bottom-left of strip).
- **Expanded**: Default content is “Properties content”. When the view is **System → Tenant Configuration**, the panel shows the **Tenant details form** (id, name, description, config version) and a **Save** button.
- **Default state**: Collapsed.
- **Auto-expand**: When the user clicks a tenant in the Tenant configuration list or clicks “+”, the Properties panel expands to show the form.
- **Auto-collapse**: When the user selects any other section in the left menu (e.g. Build, Run), the Properties panel collapses.

### 2.5 Main content

- Header shows current section and sub-option (e.g. “System → Tenant Configuration”).
- For **Run → Live Runs** and **Investigate → Runs**, a **Run** dropdown is shown in the header to select a run (enabling run-level sub-options in the left panel).
- Body is section- and sub-option–specific. Only **System → Tenant Configuration** has a full implementation; other sections show placeholders.

---

## 3. Tenant configuration (System → Tenant Configuration)

### 3.1 UX flow

1. User selects **System** in the left panel and expands it, then selects **Tenant Configuration**.
2. **Center (main content)**:
   - Header “Tenants” with a **+** button on the right.
   - List of tenants (from backend). Clicking a row selects that tenant.
   - Right-click on a tenant opens a context menu with **Delete tenant**.
3. **Right (Properties panel)**:
   - If nothing is selected and “+” was not clicked: message “Select a tenant from the list or click + to add a new one.”
   - If a tenant is selected: form with **ID** (read-only), **Name**, **Description**, **Config version**, and **Save**. Save updates the existing tenant.
   - If “+” was clicked: same form with empty fields. **Save** creates a new tenant.
4. Selecting a tenant or “+” expands the Properties panel so the form is visible. Selecting another section (Build, Run, Investigate) collapses Properties.

### 3.2 Data and behaviour

- Tenants are loaded from the backend when entering Tenant configuration.
- Display name in list and elsewhere: **name** if non-empty, otherwise **id** (e.g. UUID).
- Add/update: POST or PUT to backend; backend writes to Redis. List is refreshed after save.
- Delete: DELETE to backend; list is refreshed and selection is cleared.

---

## 4. Backend integration

### 4.1 Startup and health

- **Backend not up**: UI shows a full-screen “Waiting for backend…” view with a spinner and **wait cursor** until `GET /api/v1/health` succeeds. Health is polled every 2 seconds. No wait logic is in the start script; only the UI waits.
- **Backend up**: Normal layout is shown.

### 4.2 API usage

- All API calls use **versioned base** `/api/v1/...`. Vite proxies `/api` to `http://localhost:8082`. See **docs/STABILITY.md** for versioning and deprecation.
- **Health**: `GET /api/v1/health` — used for readiness polling.
- **Tenants**: `GET /api/v1/tenants` — list (used for left-panel dropdown and for Tenant configuration list). Response: array of `{ id, name?, description?, configVersion? }`.
- **Tenant CRUD**: `POST /api/v1/tenants` (create/upsert), `PUT /api/v1/tenants/:id` (update), `DELETE /api/v1/tenants/:id` (delete).
- **Dropdown helpers**: `GET /api/v1/tenants/:tenantId/environments`, `GET /api/v1/tenants/:tenantId/environments/:env/runs`, `GET /api/v1/dropdown-details` (combined). Used for future dropdowns (e.g. Environment, Run ID).
- **Configuration workflows**: `GET/PUT/DELETE /api/v1/configuration/workflows` — active folder (`current-active`).
- **Scenario folders**: `GET /api/v1/configuration/folders`, `POST /api/v1/configuration/folders/{id}/activate`.
- **Stack refresh**: `POST /api/v1/system/refresh` — worker Redis + olo runtime + studio reload hint.
- **Run cancel** (olo backend): `POST /api/runs/{runId}/cancel` — proxied from builder/chat when runtime is connected.

### 4.3 Tenant data source (backend)

- **With Redis:** Tenant list is stored in Redis. Key is set by env **OLO_TENANT_IDS** (e.g. `olo:tenants`). Value is a JSON array of objects: `{ "id": "...", "name": "...", "description": "...", "configVersion": "..." }`.
- **Without Redis (fallback):** When Redis is disabled or unavailable, the backend uses an **in-memory tenant store**. Tenants can be added, updated, and deleted; data is lost on restart. This improves first-run and OSS contributor experience (no Redis required).

---

## 5. Technical implementation

### 5.1 Frontend stack

- **React 18** with **TypeScript**.
- **Vite 5** for build and dev server (port 3000).
- **Zustand** for global UI state (panels, navigation, theme, tenant config). Keeps `App.tsx` from becoming a god object as features (run selection, environment, schema, metrics, ledger, plugins) are added.

### 5.2 State management (Zustand) — one store per domain

**Rule: one store per domain, not per component.** See `store/README.md`.

- **`store/ui.ts`** — Panel expansion (left, tools, properties), navigation (sectionId, subId), runId, tenantId (dropdown), theme (light/dark). Persists theme to `localStorage`. On section change, clears tenant-config selection and collapses properties when leaving Tenant configuration.
- **`store/tenantConfig.ts`** — Tenant list, loading, selected tenant, “add new” mode; actions: loadTenants, selectTenant, startAddNew, clearSelection, saveTenant, deleteTenant (all API-backed).
- **Future domains** (placeholders): `store/runtime.ts`, `store/ledger.ts`, `store/runContext.ts`, `store/plugins.ts`, `store/schema.ts`. Do not create stores per component (e.g. no treeViewStore); keep domains meaningful.
- **App** — Holds only `backendReady` (health polling), syncs URL → store, and wires store actions to components.

### 5.3 Main components and roles

| Component                 | Role |
|---------------------------|------|
| `App.tsx`                 | Root. Holds only backend-ready flag and health polling. Reads from UI and tenant-config stores; passes store state/actions to children. Conditionally renders ToolsPanel. |
| `TopBar.tsx`              | Top bar with “Olo” branding and theme toggle (reads from `useUIStore`). |
| `LeftPanel.tsx`           | Tenant dropdown; expandable categories and sub-options from `SECTIONS`; collapse toggle. Right-click context menu (Collapse / Expand, Collapse all / Expand all). Tooltips on categories (label + subtitle) and sub-options (description from layout). Fetches tenants for dropdown. |
| `ToolsPanel.tsx`          | Expandable “TOOLS” strip (left). Renders **contextual tools** from `getToolsForView(sectionId, subId, runSelected)` (tool registry + `SubOption.toolIds`). Receives `storeContext` from App (owning-store slice). Rendered only when not on Tenant configuration. |
| `MainContent.tsx`         | Header (section → sub-option, run selector when applicable). Renders `TenantConfigurationList` for Tenant configuration; placeholder for other sections. Receives tenant list, selection, and handlers from App. |
| `PropertiesPanel.tsx`     | Expandable “PROPERTIES” strip (right). Accepts optional `children`; when on Tenant configuration, children are `TenantConfigForm`. |
| `TenantConfigurationList.tsx` | Center list: tenants, + button, selection, right-click context menu (Delete). |
| `TenantConfigForm.tsx`    | Form in Properties: id, name, description, config version, Save. Shows empty state when nothing selected and not adding. |

### 5.4 Layout and styling

- **CSS**: **Feature-scoped files** under `src/styles/`. **Design tokens** in `tokens.css` (radius, space, panel widths, shadows); use `var(--radius-md)`, `var(--panel-width-left)`, etc. for consistency. `index.css` imports: `tokens.css`, `base.css`, `layout.css`, `left-panel.css`, `side-panels.css`, `main-content.css`, `tenant-config.css`, `states.css`, `theme.css`.
- **Themes**: Default **dark**; **light** via top-bar toggle; `[data-theme="light"]` in `theme.css`. Theme persisted in `localStorage`.
- **Left/right panels**: Use `var(--panel-width-left)`, `var(--panel-width-left-collapsed)`, etc. from tokens. Collapsed ~18px; expanded left 260px, tools 220px, properties 260px.
- **Standard states**: `<LoadingState />`, `<ErrorState onRetry={...} />`, `<EmptyState message="..." />` in `components/` and `styles/states.css`. Use for consistent loading, error, and empty UX across features.
- **Responsiveness**: Layout is flex-based; main content has `min-width: 0` and `overflow` for scrolling.

### 5.5 URL-driven navigation (React Router)

- **Routing**: React Router drives navigation. URL is the source of truth; a sync effect in App updates the store from `location.pathname` and `searchParams` (tenant).
- **Path patterns**:
  - `/:sectionId/:subId` — e.g. `/studio/canvas`, `/runtime/live-runs`, `/configuration/tenant-configuration`
  - `/:sectionId/run/:runId/:subId` — run-level views, e.g. `/runtime/run/123/overview`, `/ledger/run/456/time-travel`
  - Query: `?tenant=...` for tenant scope (synced to store and URL when tenant dropdown changes)
- **Helpers**: `routes.ts` exports `parsePath()`, `buildPath()`, `getDefaultSubId()`, `getRunLevelDefaultSubId()`, `DEFAULT_PATH` (`/studio/canvas`). Redirect from `/` or invalid path to `DEFAULT_PATH`.
- **Deep linking, shareable links, bookmarking, back/forward** all work because navigation is URL-based.

### 5.6 Configuration and scripts

- **Vite**: `vite.config.ts` — dev server port 3000, proxy `/api` → `http://localhost:8082`.
- **Start**: `start.bat` starts olo-be and Olo (frontend) in separate windows; no backend wait.
- **Stop**: `stop.bat` stops processes on ports 8082 and 3000.

---

## 6. File structure (relevant)

```
olo/
├── docs/
│   ├── UI-UX-AND-IMPLEMENTATION.md   (this file)
│   ├── ARCHITECTURE.md
│   ├── DOMAIN_BOUNDARIES.md          (stores must not import other domain stores; runContext scope; checklist)
│   ├── EXTENSIBILITY.md              (declarative extensions only; governance risks; flag rules)
│   ├── LAYOUT_CONTRACT.md            (panel roles: Tools=utility, Properties=entity detail, Main=workflow)
│   ├── STABILITY.md                  (v0/v1, deprecation policy)
│   ├── TEST_STRATEGY.md
│   └── PERFORMANCE.md                (lists potentially > 100 items: implement virtualized by default)
├── start.bat
├── stop.bat
├── olo-be/                            (Spring Boot, port 8082, /api/v1)
│   └── src/main/java/com/olo/
│       ├── controller/ (DropdownController, HealthController @ /api/v1)
│       ├── service/ (DropdownDataService)
│       └── dto/ (TenantDto)
└── olo-ui/
    ├── src/
    │   ├── App.tsx
    │   ├── main.tsx
    │   ├── index.css
    │   ├── api/rest.ts                (API_BASE = /api/v1)
    │   ├── routes.ts
    │   ├── config/
    │   │   ├── features.ts            (feature flags: runtime, ledger, experiments, costAnalysis, replay, …)
    │   │   └── extensions.ts          (registerSidebarItem, registerTool — extension surface)
    │   ├── auth/
    │   │   ├── types.ts               (Role, Permission, ROLE_PERMISSIONS)
    │   │   └── useAuth.ts             (hasPermission — scaffold for future RBAC)
    │   ├── hooks/
    │   │   └── useFeature.ts          (useFeature(id), useVisibleSections())
    │   ├── lib/
    │   │   └── observability.ts       (logEvent, logError — analytics/debug scaffold)
    │   ├── store/
    │   │   ├── README.md
    │   │   ├── ui.ts
    │   │   ├── tenantConfig.ts
    │   │   ├── runtime.ts
    │   │   ├── ledger.ts
    │   │   ├── runContext.ts
    │   │   ├── plugins.ts
    │   │   └── schema.ts
    │   ├── styles/
    │   │   ├── tokens.css             (--radius-*, --space-*, --panel-width-*)
    │   │   ├── base.css
    │   │   ├── layout.css
    │   │   ├── left-panel.css
    │   │   ├── side-panels.css
    │   │   ├── main-content.css
    │   │   ├── tenant-config.css
    │   │   ├── states.css             (loading, error, empty)
    │   │   └── theme.css
    │   ├── types/
    │   │   ├── layout.ts
    │   │   └── tools.ts
    │   └── components/
    │       ├── TopBar.tsx
    │       ├── LeftPanel.tsx
    │       ├── ToolsPanel.tsx
    │       ├── MainContent.tsx
    │       ├── PropertiesPanel.tsx
    │       ├── TenantConfigurationList.tsx
    │       ├── TenantConfigForm.tsx
    │       ├── LoadingState.tsx
    │       ├── ErrorState.tsx
    │       └── EmptyState.tsx
    ├── vite.config.ts
    └── index.html
```

---

## 7. Mitigations and future-proofing

These choices reduce known “where it will hurt later” risks:

| Risk | Mitigation |
|------|------------|
| **App.tsx as god object** | **Zustand** stores (`store/ui.ts`, `store/tenantConfig.ts`) hold panel state, navigation, theme, and tenant-config state/actions. App only does health polling and wiring. New domains (run selection, environment, schema, metrics, ledger, plugins) should get dedicated stores or slices so App stays thin. |
| **Tools panel undefined** | **Contextual tools system**: tools array per sub-option (`toolIds`), tool registry map (`config/toolRegistry.ts`), tool components talk only to owning store. Main always renders primary output. See LAYOUT_CONTRACT.md. |
| **Configuration isolated** | **System** section in the left menu: Tenant Configuration, Environment, Secrets, Plugins, Global Settings. Only Tenant Configuration is implemented; the rest are placeholders. See **docs/LAYOUT_CONTRACT.md** for panel ownership and flow per mode. |
| **Dark-only theme** | **Theme toggle** in the top bar (☀/☽). Light theme implemented via `[data-theme="light"]` overrides in CSS. Preference persisted in `localStorage` (`olo-theme`). Default remains dark; enterprise users can switch to light. |
| **CSS as single index.css** | **Feature-scoped CSS**: `index.css` only imports `styles/base.css`, `layout.css`, `left-panel.css`, `side-panels.css`, `main-content.css`, `tenant-config.css`, `theme.css`. Add `runtime.css`, `ledger.css`, etc. as features are built so the codebase doesn’t hit 1500+ lines in one file. |
| **No routing** | **React Router** with URL-driven navigation. Paths: `/:sectionId/:subId` and `/:sectionId/run/:runId/:subId`; query `?tenant=`. Enables deep linking, shareable debugging links, bookmarking, and browser back/forward. Add routing before adding Tree View / Time Travel. |
| **Store explosion** | **One store per domain** (see `store/README.md`). Placeholder stores: `runtime`, `ledger`, `runContext`, `plugins`, `schema`. Avoid micro-slicing (e.g. no store per component); keep domains meaningful. |
| **No Storybook / UI isolation** | **Storybook** added. Run `npm run storybook` (port 6006). Stories for TenantConfigurationList, TenantConfigForm, ToolsPanel with mock data. Contributors can develop and review UI without running backend or Redis. |
| **No contributor architecture guide** | **ARCHITECTURE.md** in `docs/` covers state philosophy, naming conventions, store creation guidelines, when to use store vs local state. |
| **Redis required for tenant list** | **In-memory fallback** in olo-be when Redis is disabled: tenant CRUD works in memory (data lost on restart). First-run works without Redis. |
| **No feature flags** | **Feature flags** in `config/features.ts` (studio, runtime, ledger, configuration, costAnalysis, replay, tenantConfiguration). `useFeature(id)`, `useVisibleSections()`. Left panel and sub-options gated by flags; OSS vs enterprise, gradual rollout. Menu is user-centric: **Build**, **Run**, **Investigate**, **System**. |
| **Domain coupling** | **DOMAIN_BOUNDARIES.md**: stores may depend only on api, types, runContext; not on each other. Prevents circular logic. |
| **Inconsistent loading/error/empty states** | **LoadingState**, **ErrorState**, **EmptyState** components; use across Live Runs, Ledger, Cost, Replay for consistent UX. |
| **No observability** | **logEvent**, **logError** in `lib/observability.ts`. Navigation and errors logged; optional `window.__oloAnalytics` for analytics. |
| **No extension surface** | **config/extensions.ts**: `registerSidebarItem()`, `registerTool()`; define plugin extension API now so future plugin UI is additive. |
| **Layout drift** | **LAYOUT_CONTRACT.md**: Tools = contextual utility, Properties = entity detail, Main = primary workflow. Do not repurpose panels. |
| **CSS drift** | **Design tokens** in `tokens.css`: `--radius-*`, `--space-*`, `--panel-width-*`, `--shadow-*`. Use for Tree, Timeline, Diff, Charts. |
| **No permission/role scaffolding** | **auth/types.ts**, **useAuth()**: Role, Permission, `hasPermission('deleteTenant')`. Scaffold now; replace with real auth later. |
| **Unversioned API** | **Versioned API** `/api/v1/...` in backend and frontend. See STABILITY.md for deprecation. |
| **Silent breaking changes** | **STABILITY.md**: v0.x unstable, v1.x stable; deprecation policy (announce before removal). |
| **Fragile test patterns** | **TEST_STRATEGY.md**: unit tests for stores, component tests for forms, routing tests for path parsing; no snapshot-heavy tests. |
| **Rendering bottlenecks** | **PERFORMANCE.md**: any list potentially exceeding 100 items must be implemented virtualized by default (hard law). |

---

## 8. UI development without backend (Storybook)

- Run **Storybook:** `npm run storybook` in the Olo frontend project (port 6006).
- Stories use **mock data and callbacks**; no backend, Redis, or tenants required.
- Available stories: **Configuration/TenantConfigurationList** (with tenants, empty, loading), **Configuration/TenantConfigForm** (empty state, add new, edit existing), **Layout/ToolsPanel** (collapsed, expanded).
- Add stories for new components under `src/**/*.stories.tsx` so UI can be developed in isolation.

---

## 9. Summary

- **UI**: Top bar + left (tenant + expandable menu) + optional tools + main content + optional properties. Left and side panels collapse to thin strips with vertical labels.
- **UX**: Menu is **Build**, **Run**, **Investigate**, **System**. Tenant Configuration (System) is the only fully implemented flow: list in center, add/edit/delete via Properties and context menu; Properties expands on tenant/+ and collapses when leaving the section. Panel ownership: Left = “Where am I?”; Tools = “What can I do here?”; Main = “What am I looking at?”; Properties = “What is selected?” (see **docs/LAYOUT_CONTRACT.md**).
- **Implementation**: React + TypeScript + Vite; **Zustand** (one store per domain); **React Router** for URL-driven navigation; **feature-scoped CSS** under `styles/`; **Storybook** for UI isolation. REST to olo-be; tenants from Redis or **in-memory fallback** when Redis is disabled. **ARCHITECTURE.md** for contributor guidance (state, naming, stores). UI shows wait cursor until backend health succeeds; start script does not wait for backend.
