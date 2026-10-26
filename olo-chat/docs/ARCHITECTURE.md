<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->

# Architecture

Technical architecture of the olo-chat frontend: stack, routing, state, API layer, and data flows.

For technology choices, UI design, and backend communication, see **[TECHNOLOGY_ARCHITECTURE.md](./TECHNOLOGY_ARCHITECTURE.md)**.

---

## High-level stack

| Layer | Technology |
|-------|------------|
| **Runtime** | Browser (React 18). |
| **Build** | Vite 5, TypeScript. |
| **UI** | React 18, React Router 7. |
| **State** | Zustand. |
| **Backend** | olo backend at `VITE_API_BASE` (default `http://localhost:7080`). |

SPA; backend is source of truth for sessions, messages, runs, and execution events.

---

## Routing

- **Path** — `/:sectionId/:subId` (e.g. `/chat/conversation`). Default: `/chat/conversation`.
- **Query** — `menu`, `tools`, `props` (panel expanded state). Optional legacy `tenant` query is not used for API calls.
- **Tenant** — Resolved by backend via `GET /api/ui/context` (JWT or config region folder).

---

## State (Zustand stores)

| Store | Export | Purpose |
|-------|--------|---------|
| **ui** | `useUIStore` | Panels, widths, theme, navigation, `runEventsBellUnread`. |
| **chatSessions** | `chatSessionsStore` | Session list and selected session ID. |
| **runEvents** | `runEventsStore` | Current run events (SSE/WebSocket). **ChatView** reads `s.events` as the single timeline source. Persist last 200 non-liveness events; dedupe on replay; hydrate from `localStorage`. |
| **conversationPanel** | `conversationPanelStore` | **selectedProfileId**, **selectedQueueId**, **selectedPipelineId** from the active chat preset. |
| **sessionDisplay** | `sessionDisplayStore` | Custom titles and first-message previews (persisted, max 80). |
| **knowledgeIngest** | `knowledgeIngestStore` | RAG ingest job state (Knowledge section). |
| **documentUploads** | `documentUploadsStore` | Documents upload queue and status. |
| **tenantConfig** | `tenantConfigStore` | Tenant list from `GET /api/tenants`. Config form exists but nav is disabled. |

---

## API layer

| Module | Purpose |
|--------|---------|
| **chatApi** | Re-exports from split modules below. |
| **chatContextApi** | `GET /api/health`, `GET /api/ui/context` (`chatProfiles`). |
| **chatSessionsApi** | Sessions, messages (optional `capabilitySource` on send). |
| **chatRunsApi** | Runs: SSE events, response, status, cancel, human-input. |
| **chatSseApi** | SSE stream helpers. |
| **ragIngestApi** | `POST /api/rag/ingest`, `GET /api/knowledge/sources`, `GET /api/documents`. |
| **documentsUploadApi** | `POST /api/resource/upload`. |
| **rest** | Tenant CRUD at `/api/v1/tenants` (orphaned from main chat flow). |

### ChatProfileDto (from UI context)

| API field | Workflow JSON source |
|-----------|---------------------|
| `id` | `id` |
| `displayName` | `role` (fallback: `name`, `id`) |
| `displaySummary` | `shortDescription` |
| `emoji` | `emoji` |
| `queue` | `queue` (typically same as `id`, e.g. `ask`) |
| `pipeline` | `id` |
| `runAgain` | `runAgain` |

Profiles are built by the backend from **`olo.configuration.dir/<region>/*.json`**, not from Redis or static frontend JSON.

---

## Lib and hooks

| Module | Purpose |
|--------|---------|
| **queueDisplayName** | Strips `:version` suffix for API params. |
| **chatProfileUi** | `emojiForProfile`, `formatProfileOptionLabel`. |
| **wsUrl** / **wsSingleton** | WebSocket URL; shared socket; `SUBSCRIBE_RUN`. |
| **useWebSocketLiveness** | PING/PONG + `RUN_EVENT` → `runEventsStore`. |
| **useBackendReachable** | Polls health; gates Chat disconnected UI. |

---

## Component tree (simplified)

```
App
├── TopBar
└── app-body
    ├── LeftPanel (Chat → Conversation)
    ├── ToolsPanel (New chat + sessions; no queue/pipeline dropdowns)
    ├── MainContent
    │   ├── ChatView (preset select, messages, composer, progress strip)
    │   ├── KnowledgeView (Sources / Create / Status + RAG sources list)
    │   └── DocumentsUploadView
    └── PropertiesPanel
        └── EventsList
```

- **ChatView** — Requires non-empty `chatProfiles`. Preset pill beside composer. Config pills per `runId`. Run again menu for presets with `runAgain`. Human-input from `input.options`. Worker progress strip below composer.
- **ToolsPanel** — Sessions list (tenant-wide). Delete-all passes selected preset `queue` + `pipeline` as query params.

---

## Data flow (Chat)

1. **Load** — `GET /api/ui/context` → `tenantId`, `chatProfiles`. First profile initializes `conversationPanelStore`. `listSessions(tenantId)` without queue filter.
2. **New chat** — `createSession(tenantId, {})` — body is `{ tenantId }` only.
3. **Send** — `sendMessage` with `taskQueue` from `selectedQueueId` (`queueDisplayName`). Subscribe SSE or WebSocket `SUBSCRIBE_RUN`. Human steps via `POST .../human-input`.
4. **Delete all** — `deleteAllSessions(tenantId, { queue, pipeline })` from current preset.

---

## Environment and build

Build-time: `VITE_API_BASE`, `VITE_WS_ACCESS_TOKEN`, `VITE_WS_PING_INTERVAL_SEC`, upload-related `VITE_*`. See [DOCKER.md](./DOCKER.md).

---

## Related docs

- [TECHNOLOGY_ARCHITECTURE.md](./TECHNOLOGY_ARCHITECTURE.md)
- [UI_FEATURES.md](./UI_FEATURES.md)
- [CHAT_UI.md](./CHAT_UI.md)
- [README.md](./README.md)
- [DOCKER.md](./DOCKER.md)
