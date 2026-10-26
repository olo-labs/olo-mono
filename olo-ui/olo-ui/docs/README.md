<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->
# olo-ui docs

## Contributor and owner notes

This folder is the frontend contributor landing area. Add docs here when a UI feature needs local architecture, review, or workflow context; link back to root docs for platform rules. Credit owners in [../../docs/MODULE_OWNERS.md](../../docs/MODULE_OWNERS.md).

Frontend-specific documentation for the **olo-ui** package (`olo-ui/olo-ui/`). Cross-cutting contributor guides live in the repository root [docs/](../../docs/) folder.

---

## In this folder

| Document | Description |
|----------|-------------|
| [ARCHITECTURE.md](ARCHITECTURE.md) | System design: v1 navigation, four-panel builder, catalog, canvas, workflow JSON, olo-be API |
| [DOCKERHUB-PAGE.md](DOCKERHUB-PAGE.md) | Copy-paste Docker Hub description for the combined image |

---

## Quick reference

### What ships in v1

| Area | Route | Capability |
|------|-------|------------|
| Workflows → Builder | `/workflows/builder` | Drag-and-drop graph canvas, variables, tools, hooks, child workflows, available agents |
| Workflows → Agents | `/workflows/agents` | Tree view of presets; import, export, save `olo-configuration` JSON |
| Workflows → Log | `/workflows/log` | Read-only canvas of runtime-injected graphs from `olo-configuration/log/`; drag nodes to rearrange (session-only) |
| Administration → Tenants | `/administration/tenants` | Tenant CRUD |
| Administration → Scenarios | `/administration/scenarios` | Activate `olo-configuration` scenario folders into `current-active` |
| Overview, Executions, Observability, Extensions | `/overview`, … | Scheduled placeholders |

### Local run

```powershell
# Backend (port 8082)
cd olo-ui\olo-be
.\gradlew.bat bootRun

# Frontend (port 3000, proxies /api → :8082 and /runtime-api → :7080)
cd olo-ui\olo-ui
npm install
npm run dev
```

Open `http://localhost:3000` → defaults to **Workflows → Builder**.

### Workflow editor workflow

1. **Agents** — select or import `agent.json` (or any preset from `olo.configuration.directory`).
2. **Builder** — drag catalog nodes, connect edges, enable tools/hooks/agents in the Builder panel.
3. **Properties** — edit catalog-matched workflow parameters (temperature, model, etc.).
4. **Save** — writes back to the configuration folder (Drive-synced if configured).

### Runtime graph logs

After a workflow run injects a dynamic subgraph, the kernel writes JSON under **`olo-configuration/log/`**. Open **Workflows → Log** to inspect the merged graph in a read-only canvas. Use the toolbar dropdown to switch logs; drag nodes to improve layout (not persisted).

### Key dependencies

- **React 18** + **Vite** — UI shell
- **@xyflow/react** — workflow graph canvas
- **Zustand** — domain stores
- **react-router-dom** — URL navigation

### Tests

```powershell
cd olo-ui\olo-ui
npm run test
```

---

## Root docs (shared)

From [docs/](../../docs/):

| Document | Topic |
|----------|-------|
| [ARCHITECTURE.md](../../docs/ARCHITECTURE.md) | Store rules, naming, state flow |
| [ENVIRONMENT.md](../../docs/ENVIRONMENT.md) | Env vars (local, Docker, Compose) |
| [LAYOUT_CONTRACT.md](../../docs/LAYOUT_CONTRACT.md) | Panel roles (Nav, Builder, Main, Properties) |
| [DOMAIN_BOUNDARIES.md](../../docs/DOMAIN_BOUNDARIES.md) | Cross-domain rules, runContext |
| [EXTENSIBILITY.md](../../docs/EXTENSIBILITY.md) | Feature flags, extension API |
| [TEST_STRATEGY.md](../../docs/TEST_STRATEGY.md) | Testing conventions |
| [STABILITY.md](../../docs/STABILITY.md) | API versioning |
| [DOCKERHUB-PAGE.md](../../docs/DOCKERHUB-PAGE.md) | Canonical Docker Hub text (repo root) |

---

## Related paths (monorepo)

| Path | Role |
|------|------|
| `olo-mono/olo-core/dist/catalog/` | Studio catalog JSON (nodes, tools, hooks, presets) |
| `olo-mono/olo-definition/olo-configuration/` | Default workflow preset JSON and runtime graph logs (`log/`) |
| `olo-ui/olo-be/` | Spring Boot API serving catalog + configuration folder |

See [olo-ui/README.md](../README.md) for Gradle build and Storybook.
