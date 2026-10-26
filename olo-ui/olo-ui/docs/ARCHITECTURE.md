<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->
# Olo UI — Architecture

## Contributor and owner notes

Frontend architecture contributors should keep this file aligned with implemented routes, stores, builder behavior, and backend endpoints. Owners should review for state discipline, canvas performance, and contributor clarity. Credit frontend and builder ownership in [../../docs/MODULE_OWNERS.md](../../docs/MODULE_OWNERS.md).

This document describes how **olo-ui** (the React frontend) is structured, how it talks to **olo-be** (the Spring Boot API), and how it integrates with **olo-core** catalog and **olo-configuration** workflow presets.

For contributor conventions (state flow, store rules, naming), see the repository root [ARCHITECTURE.md](../../docs/ARCHITECTURE.md). This document focuses on the frontend package and its immediate boundaries.

---

## 1. Purpose

**olo-ui** is the operator console for Olo. v1 navigation:

| Section | Route | Status |
|---------|-------|--------|
| **Overview** | `/overview` | Scheduled |
| **Workflows** | `/workflows/builder`, `/workflows/agents`, `/workflows/log` | **Primary product** — graph editor, preset management, runtime graph logs |
| **Executions** | `/executions` | Scheduled |
| **Observability** | `/observability` | Scheduled |
| **Extensions** | `/extensions` | Scheduled |
| **Administration** | `/administration/tenants`, `/administration/scenarios` | Tenant CRUD; scenario activation into `current-active` |

The UI is **REST-oriented**: server data flows through `src/api/rest.ts` into Zustand domain stores. Components stay declarative.

---

## 2. System context

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              Browser                                    │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │  olo-ui (React + Vite, port 3000 dev)                             │  │
│  │  • URL-driven navigation                                          │  │
│  │  • Zustand stores per domain                                      │  │
│  │  • Catalog-driven editors + React Flow canvas                     │  │
│  └────────────────────────────┬──────────────────────────────────────┘  │
└───────────────────────────────┼─────────────────────────────────────────┘
                                │  /api/v1/*  (olo-be, proxied in dev & Docker)
                                │  /runtime-api/*  (workflow execution → :7080)
                                ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  olo-be (Spring Boot, port 8082)                                        │
│  • Tenants, dropdowns, health                                         │
│  • Extension catalog (static JSON from olo-core)                        │
│  • Workflow configuration files (olo-configuration folder)            │
│  • Dynamic subgraph injection logs (olo-configuration/log)            │
└───────────────┬─────────────────────────────┬───────────────────────────┘
                │                             │
                ▼                             ▼
         Redis (optional)              Filesystem / Drive-synced folder
         tenant list                   olo-configuration/*.json
                                      olo-configuration/log/*.json
```

**olo-be intentionally has no `org.olo` Java dependencies.** Catalog and workflow documents are served as JSON. Editor metadata comes from **olo-core**; workflow definitions come from **olo-definition/olo-configuration**.

---

## 3. Repository layout

```
olo-ui/                          # Repository root
├── olo-ui/                      # Frontend package (this document)
│   ├── src/
│   │   ├── api/                 # REST client (`/api/v1`)
│   │   ├── components/
│   │   │   ├── builder/         # BuilderSidePanel, VariablesSection, …
│   │   │   ├── canvas/          # WorkflowCanvas, CatalogFlowNode
│   │   │   └── …                # Shell, lists, editors
│   │   ├── config/              # Feature flags, tool registry
│   │   ├── lib/
│   │   │   ├── workflowGraph.ts # Workflow JSON ↔ React Flow
│   │   │   ├── workflowResources.ts  # Variables, tools, hooks, agents
│   │   │   ├── catalogLookup.ts
│   │   │   ├── canvasDrag.ts
│   │   │   └── workflowConfiguration.ts
│   │   ├── routes.ts
│   │   ├── store/
│   │   │   ├── workflowConfigurationStore.ts
│   │   │   └── graphLogStore.ts     # Read-only runtime graph logs
│   │   ├── styles/
│   │   └── types/               # catalog.ts, workflow.ts, layout.ts
│   ├── docs/
│   └── vite.config.ts           # Dev proxy `/api` → :8082
├── olo-be/
└── docs/                        # Shared contributor docs
```

---

## 4. Application shell

`App.tsx` wires URL sync, health gate, panel layout, and store subscriptions. It does **not** own domain logic.

### Workflows layout

The **Builder panel** is resizable (drag handle between Builder and Canvas). Query param `tools=1` controls its visibility (`toolsPanelExpanded` in `ui` store). It appears **only** on **Workflows → Builder** — not on Agents or Log.

**List + form views** (Agents, Administration → Tenants) hide the Builder panel — main list + Properties form only.

**Log view** (Workflows → Log) uses the same React Flow canvas as Builder in **read-only** mode: no Builder panel, no save/run, no connect/add/delete. Users may **drag nodes** to rearrange the layout for visibility (session-only; not written to disk).

```
Workflows → Builder (four panels)
┌──────────┬─────────────┬──────────────────────┬──────────────┐
│  Nav     │  Builder    │  Canvas              │  Properties  │
│          │  panel      │  (React Flow)        │              │
│ Overview │ Components  │  Drag/connect nodes  │  Workflow    │
│ Workflows│ Variables   │  Delete / move       │  parameters  │
│ …        │ Tools       │                      │  Save        │
│          │ Hooks       │                      │              │
│          │ Child WFs   │                      │              │
│          │ Agents      │                      │              │
└──────────┴─────────────┴──────────────────────┴──────────────┘

Workflows → Log (three panels — Builder panel hidden)
┌──────────┬────────────────────────────────────┬──────────────┐
│  Nav     │  Canvas (read-only)               │  Properties  │
│          │  Select log from toolbar dropdown │  (empty)     │
│          │  Drag nodes to rearrange (local)  │              │
└──────────┴────────────────────────────────────┴──────────────┘
```

See [LAYOUT_CONTRACT.md](../../docs/LAYOUT_CONTRACT.md) for panel intent rules.

---

## 5. Navigation and URL

| Path pattern | Example | Meaning |
|--------------|---------|---------|
| `/:section` | `/overview` | Section without sub-options |
| `/:section/:sub` | `/workflows/builder` | Section + sub-option |
| `/:section/run/:runId/:sub` | (future Executions) | Run-level view |
| Query `tenant`, `menu`, `tools`, `props` | `?props=1` | Tenant + panel visibility |

Helpers: `src/routes.ts` — `parsePath`, `buildPath`, `buildQuery`, `parseQuery`.

Definitions: `src/types/layout.ts` (`SECTIONS`). Feature gating: `src/config/features.ts`.

**Default path:** `/workflows/builder`.

---

## 6. State management

One **Zustand store per domain** (not per component).

| Store | Responsibility |
|-------|----------------|
| `ui.ts` | Panel widths/expansion, navigation mirror, theme |
| `tenantConfig.ts` | Tenant list, selection, CRUD |
| `workflowConfigurationStore.ts` | Workflow list, draft, dirty, import/export, `selectedCanvasNodeId` |
| `graphLogStore.ts` | Runtime graph log list, read-only draft for Log view |
| `catalogStore.ts` | Extension catalog from olo-core |
| `runtime.ts`, `ledger.ts`, … | Placeholders for future sections |

**Data flow:**

```
URL → App → Store action → API (rest.ts) → Store state → Component
```

Components never call `fetch` directly. See `src/store/README.md` and root [ARCHITECTURE.md](../../docs/ARCHITECTURE.md).

---

## 7. API layer

All HTTP via `src/api/rest.ts` under **`/api/v1`** (olo-be). Workflow execution uses **`src/api/oloRuntime.ts`** under **`/runtime-api`** (proxied to olo chat backend on port 7080):

| Endpoint | Purpose |
|----------|---------|
| `GET /health` | Backend readiness |
| `GET/POST/PUT/DELETE /tenants` | Tenant CRUD |
| `GET /dropdown-details` | Tenants, environments, run IDs |
| `GET /catalog` | Merged `catalog.json` |
| `GET /configuration/workflows` | List workflow `*.json` files |
| `GET/PUT/DELETE /configuration/workflows/{file}` | Read/write/delete preset |
| `GET /configuration/workflows/meta/root` | Configured folder path |
| `GET /configuration/folders` | List scenario folders (Scenarios tab) |
| `POST /configuration/folders/{id}/activate` | Activate scenario into `current-active` |
| `POST /system/refresh` | Signal worker refresh via Redis |
| `POST /model-providers/test` | Test LLM provider connectivity |
| `GET /configuration/logs` | List dynamic subgraph injection logs (`*.json`) |
| `GET /configuration/logs/{file}` | Read `mergedGraph` from a log file (read-only) |
| `GET /configuration/logs/meta/root` | Log directory path |

**Runtime API** (`oloRuntime.ts`, `/runtime-api` → `:7080`): `GET /health`, `POST /sessions`, `POST /sessions/{id}/messages`, `GET /runs/{id}/events`, `POST /runs/{id}/cancel`, `POST /runs/{id}/human-input`, etc.

Dev: Vite proxies `/api` → `localhost:8082`, `/runtime-api` → `localhost:7080`. Docker: nginx proxies to embedded Spring Boot.

---

## 8. Feature flags

`config/features.ts` gates sections at composition time (`LeftPanel`):

`overview`, `workflows`, `executions`, `observability`, `extensions`, `administration`, plus sub-flags `tenantConfiguration`, `workflowConfiguration`, `scenarioConfiguration`.

Flags must not branch business logic inside domain stores.

---

## 9. Catalog integration

```
olo-mono/olo-core/dist/catalog/catalog.json
        │  syncExtensionCatalog (olo-be build)
        ▼
GET /api/v1/catalog  →  catalogStore  →  editors / builder / canvas
```

`catalog.json` contains **nodes**, **tools**, **hooks**, **workflowPresets**, and **defaults** (connection rules, designer sizing).

| Helper | Use |
|--------|-----|
| `findWorkflowPreset` | Match `workflow.id` → preset parameters |
| `findCatalogNode` | Resolve node `type` → palette metadata |
| `presetParametersForWorkflow` | Properties panel parameter fields |
| `catalogComponentGroups` | Group nodes/tools/hooks for Builder panel |

Regenerate after olo-core changes:

```powershell
cd olo-mono\olo-core
.\gradlew.bat exportStudioCatalog
cd olo-ui\olo-be
.\gradlew.bat build
```

---

## 10. Workflow configuration (olo-configuration)

Workflow presets are **WorkflowDefinition** JSON — graph, parameters, runtime resources — stored as `*.json`.

### Catalog vs configuration

| Artifact | Source | UI role |
|----------|--------|---------|
| **Catalog** | olo-core `dist/catalog` | Widgets, palette, tool/hook metadata |
| **olo-configuration** | Backend folder (or Drive-synced path) | Persisted workflow documents |

### Import / Export flow (Workflows → Agents)

1. **Workflows → Agents** — tree/list of presets from `olo.configuration.directory`.
2. **Import** — file picker; **Export** — browser download.
3. **Select** — loads `workflowConfigurationStore.draft`.
4. **Properties** — `WorkflowConfigurationEditor` edits catalog-matched **parameters**.
5. **Save** — `PUT /api/v1/configuration/workflows/{file}`.

### Log flow (Workflows → Log)

When the Olo runtime injects a dynamic subgraph (tool-call expansion, dynamic-graph expansion), the kernel writes an audit JSON file under **`olo-configuration/log/`**. Each file contains metadata (`kind`, `workflowId`, `timestamp`, …) and a **`mergedGraph`** — the full workflow graph after injection.

1. **Workflows → Log** — toolbar dropdown lists log files (newest first).
2. **Select** — `graphLogStore` loads `mergedGraph` via `GET /api/v1/configuration/logs/{file}`.
3. **Canvas** — same `WorkflowCanvas` as Builder with `mode="log"` and `readOnly`: pan/zoom, drag nodes for layout only.
4. **No save** — layout changes stay in the browser session; log files on disk are never modified.

Log directory resolution (backend):

- `OLO_LOG_DIRECTORY` / `olo.configuration.log-directory` when set
- otherwise `{configuration}/log` if it exists
- otherwise sibling `{parent-of-configuration}/log` (typical: `olo-configuration/log` next to `current-active`)

Local default when running olo-be from the repo: `olo-mono/olo-definition/olo-configuration/log/`.

### Builder flow

1. Open a workflow (step above).
2. **Workflows → Builder**.
3. **Builder panel** — configure runtime resources (see §11).
4. **Canvas** — drag nodes from Components; connect ports; move/delete nodes.
5. **Save** — persists graph + resources to the same JSON file.

### Drive / shared folder

No Google Drive API in v1. Point the backend at a synced folder:

```properties
# olo-be application.properties
olo.configuration.directory=C:/Users/you/Google Drive/olo-configuration/current-active
olo.configuration.log-directory=C:/Users/you/Google Drive/olo-configuration/log
```

Or via environment: `OLO_CONFIGURATION_DIRECTORY`, `OLO_LOG_DIRECTORY`.

---

## 11. Workflow builder

### Canvas (`@xyflow/react`)

| File | Role |
|------|------|
| `components/canvas/WorkflowCanvas.tsx` | React Flow host, drop target, connect/delete |
| `components/canvas/CatalogFlowNode.tsx` | Custom node with catalog port handles |
| `lib/workflowGraph.ts` | `workflowToFlow` / `flowToWorkflow` conversion |
| `lib/canvasDrag.ts` | Drag payload from Builder panel → canvas |
| `lib/portConnection.ts` | Port schema compatibility (future connect validation) |

- Node positions stored in `node.configuration.designer.position`.
- Edges use olo-definition shape: `sourceNodeId`, `targetNodeId`, optional `sourcePortId` / `targetPortId`.
- Only catalog **nodes** are draggable onto the canvas (not tools/hooks).

### Builder side panel

`components/builder/BuilderSidePanel.tsx` — expandable sections:

| Section | Writes to workflow JSON |
|---------|-------------------------|
| **Components → Nodes** | `nodes[]` (via canvas drag) |
| **Variables** | `variables[]` — name, type, description, required, metadata |
| **Tools** | `tools[]` — catalog tool + `runtimeBinding.implementationId` |
| **Hooks** | `hooks[]` — catalog hook, pattern `**`, `pre` binding |
| **Child workflows** | `childWorkflows[]` — `{ workflowId, workflowVersion }` |
| **Available agents** | `availableAgents[]` — `{ id }` planner delegation hints |

Logic: `lib/workflowResources.ts` (`toggleCatalogTool`, `toggleCatalogHook`, `upsertVariable`, etc.).

Child workflows and available agents are chosen from other presets in the same configuration folder (excluding the current workflow).

### Key workflow files

| File | Role |
|------|------|
| `store/workflowConfigurationStore.ts` | Draft lifecycle, dirty flag, CRUD |
| `components/WorkflowConfigurationList.tsx` | Import/export list |
| `components/WorkflowConfigurationEditor.tsx` | Catalog parameter fields |
| `components/StudioCanvas.tsx` | Builder canvas entry (`mode="builder"` \| `"log"`) |
| `components/builder/VariablesSection.tsx` | Variable CRUD UI |
| `types/workflow.ts` | TypeScript contracts for workflow document slices |
| `types/graphLog.ts` | Graph log summary types |
| `lib/graphLog.ts` | Log list labels / timestamps |

---

## 12. Backend boundary (olo-be)

| Controller | Responsibility |
|------------|----------------|
| `HealthController` | Liveness |
| `DropdownController` | Tenant/environment/run dropdowns |
| `ExtensionCatalogController` | Classpath `catalog.json` |
| `WorkflowConfigurationController` | CRUD on `olo.configuration.directory` |
| `GraphLogController` | Read-only list/read of `olo.configuration.log-directory` |

`WorkflowConfigurationService` and `GraphLogService` sanitize file names and prevent path traversal.

See [olo-be/docs/README.md](../../olo-be/docs/README.md) and [ENVIRONMENT.md](../../docs/ENVIRONMENT.md).

---

## 13. Styling

Feature-scoped CSS imported from `src/index.css`:

| File | Scope |
|------|-------|
| `tokens.css`, `layout.css`, `side-panels.css` | App shell |
| `tenant-config.css` | Shared form patterns |
| `workflow-config.css` | Workflow list/editor |
| `builder-side-panel.css` | Builder panel sections |
| `workflow-canvas.css` | React Flow canvas + nodes |
| `components-panel.css` | Legacy component list styles (subset reused) |

Panel widths use CSS variables (`--panel-width-left`, `--panel-width-tools`, `--panel-width-properties`) persisted in `localStorage`.

---

## 14. Testing

| Layer | Tool | Examples |
|-------|------|----------|
| Routes | Vitest | `routes.test.ts` |
| Lib | Vitest | `workflowGraph.test.ts`, `workflowResources.test.ts`, `workflowConfiguration.test.ts` |
| Stores | Vitest | `ui.test.ts` |
| Integration | Vitest + RTL | `App.routing.test.tsx` |
| Components | Storybook | `*.stories.tsx` |

```bash
cd olo-ui/olo-ui
npm run test
```

See [TEST_STRATEGY.md](../../docs/TEST_STRATEGY.md).

---

## 15. Build and deployment

### Local development

```powershell
# Terminal 1 — backend
cd olo-ui\olo-be
.\gradlew.bat bootRun

# Terminal 2 — frontend
cd olo-ui\olo-ui
npm run dev
```

Open `http://localhost:3000` (API proxied to `:8082`).

Ensure `olo-mono/olo-core/dist/catalog` exists (run `exportStudioCatalog`) before building olo-be so `/api/v1/catalog` is populated.

### Production

```powershell
cd olo-ui\olo-ui
npm run build

cd olo-ui\olo-be
.\gradlew.bat build
```

### Docker

Combined image from **olo-ui repository root**. See [DOCKERHUB-PAGE.md](DOCKERHUB-PAGE.md).

---

## 16. Extension points

| Mechanism | Declares | Host owns |
|-----------|----------|-----------|
| `SECTIONS` / `layout.ts` | Nav structure | LeftPanel, MainContent |
| `features.ts` | Capability toggles | Section visibility |
| Catalog JSON | Node/tool/hook/preset metadata | Builder panel, canvas, Properties |
| `toolRegistry.ts` | Contextual tools (non-builder views) | ToolsPanel |

Extensions register **metadata only**. See [EXTENSIBILITY.md](../../docs/EXTENSIBILITY.md).

---

## 17. Related documentation

| Document | Location |
|----------|----------|
| Contributor architecture | [docs/ARCHITECTURE.md](../../docs/ARCHITECTURE.md) |
| Panel layout contract | [docs/LAYOUT_CONTRACT.md](../../docs/LAYOUT_CONTRACT.md) |
| Environment variables | [docs/ENVIRONMENT.md](../../docs/ENVIRONMENT.md) |
| Store discipline | [src/store/README.md](../src/store/README.md) |
| Backend API | [olo-be/docs/README.md](../../olo-be/docs/README.md) |
| olo-definition workflow schema | `olo-mono/olo-definition/doc/ARCHITECTURE.md` |
| Docker Hub copy | [DOCKERHUB-PAGE.md](DOCKERHUB-PAGE.md) |

---

## 18. Evolution notes

| Area | Next increments |
|------|-----------------|
| **Canvas** | Port schema validation on connect; per-node Properties editor |
| **Hooks** | Edit pattern / phases in UI (today: default `**` + `pre`) |
| **Executions / Observability** | Populate stores; run-level routes in `routes.ts` |
| **Auth** | Gate `rest.ts` or add interceptors |
| **Catalog** | Hot-reload in dev (today: olo-be rebuild) |

When adding features, extend the **owning domain store** — avoid per-view stores.
