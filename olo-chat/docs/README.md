<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->

# olo-chat

Frontend for the **Olo** chat flow. It provides a chat UI that talks to the **olo** backend (REST + SSE + WebSocket) for sessions, messages, runs, and live execution events.

---

## Documentation index

| Doc | Description |
|-----|-------------|
| **[TECHNOLOGY_ARCHITECTURE.md](./TECHNOLOGY_ARCHITECTURE.md)** | Technology stack, logical architecture, UI design, and communication (REST, SSE, WebSocket, event model). |
| **[UI_FEATURES.md](./UI_FEATURES.md)** | User-facing features: panels, preset pills, config pills, thinking line, Run again, persisted run events, worker progress, Events bell, human-input, health polling. |
| **[ARCHITECTURE.md](./ARCHITECTURE.md)** | Stack, routing, stores, API layer, component tree, Chat data flow. |
| **[CHAT_UI.md](./CHAT_UI.md)** | Chat APIs, profiles, session/message/run flow, execution model. |
| **[DOCKER.md](./DOCKER.md)** | Docker build/run, env vars, Docker Compose, GitHub Actions. |
| **[DOCKER_HUB_DESCRIPTION.md](./DOCKER_HUB_DESCRIPTION.md)** | Copy-paste description for the Docker Hub image page. |

---

## Overview

### Sections

- **Chat** — **Conversation** only (`/chat/conversation`). Create a session, pick a **preset** beside the composer, send messages, and see run events (PLANNER, MODEL, TOOL, HUMAN, SYSTEM, plus WebSocket PING/PONG liveness). **Tools panel:** **New chat** + sessions list. APIs: `POST /api/sessions`, `POST /api/sessions/{sessionId}/messages`, `GET /api/runs/{runId}/events` (SSE), optional WebSocket `/ws`, `POST /api/runs/{runId}/human-input`.
- **Knowledge** — **Sources**, **Create new**, **Status**. Wired to `GET /api/knowledge/sources`, `POST /api/rag/ingest`, and `GET /api/documents`. See [CHAT_UI.md](./CHAT_UI.md#knowledge--rag).
- **Documents** — **Upload / Manage Raw Files**: capability source dropdown (`VITE_CAPABILITY_SOURCE_OPTIONS`), upload to backend shared storage via `POST /api/resource/upload`.

### Chat profiles (required)

The chat UI **requires** `chatProfiles` from **`GET /api/ui/context`**. There is no legacy Queue/Pipeline picker.

- **Source** — Backend scans **`olo.configuration.dir`** (e.g. `olo-mono/olo-definition/olo-configuration/current-active/*.json`). Each file is a `WorkflowDefinition`; the backend maps fields to `ChatProfileDto`:
  - `role` → `displayName`
  - `shortDescription` → `displaySummary`
  - `emoji`, `queue`, `id` → `pipeline`
  - `runAgain` → `runAgain`
- **Selection** — User picks a preset **beside the message input**; `conversationPanelStore` holds `selectedProfileId`, `selectedQueueId`, and `selectedPipelineId` derived from that preset.
- **Task queue** — Each profile’s `queue` matches its workflow id (e.g. `ask`, `fast`). Sent on each message as `taskQueue`.
- **Empty config** — If `chatProfiles` is empty, **ChatView** shows a blocking message (no fallback UI).

### Tenant context

- **Tenant** is **not chosen in the UI.** The backend resolves `tenantId` from JWT (when present) or the default region folder under `olo.configuration.dir`.
- **`GET /api/ui/context`** supplies `tenantId`, footer labels (`tenant`, `user`), `oloVersion`, and **`chatProfiles`**.

The app defaults to **Chat → Conversation** and uses the **olo** backend as the source of truth for sessions, messages, runs, and execution events.

---

## Backend

| Repo path | Role |
|-----------|------|
| **olo** | Chat backend (Spring Boot). REST + SSE at `/api`. Default port **7080**. Start with `start.bat` or `./gradlew bootRun`. |
| **olo-mono/olo-definition/olo-configuration** | Active workflow JSON in `current-active/` (e.g. copied from `default/ask.json`). Drives chat profiles and Temporal task queues. |
| **olo-chat** | This frontend (Vite + React). Proxies `/api` to the backend in development. |

**Relevant backend docs:**

- **[olo/olo-temporal-sdk/docs/ARCHITECTURE.md](../../olo/olo-temporal-sdk/docs/ARCHITECTURE.md)** — Temporal SDK, client, backend integration.
- **[olo/olo-temporal-sdk/docs/DESIGN.md](../../olo/olo-temporal-sdk/docs/DESIGN.md)** — Workflow start/signal design.

---

## Run the app

1. **Start the olo backend** (port 7080), from the `olo` directory:
   ```bash
   start.bat
   ```
   or `./gradlew bootRun`. Ensure **`OLO_CONFIGURATION_DIR`** points at `olo-mono/olo-definition/olo-configuration` (set automatically by `start.bat`).

2. **Start olo-chat** (from this directory):
   ```bash
   npm install
   npm run dev
   ```
   Dev server: **http://localhost:3000**. Set `VITE_API_BASE=http://localhost:7080` in `.env.development`.

3. Open **http://localhost:3000**. Choose a **preset** next to the composer. If no presets appear, add workflow JSON under `olo-mono/olo-definition/olo-configuration/current-active/` and restart the backend.

---

## Chat flow (high level)

1. **Session** — **New chat** → `POST /api/sessions` with `{ "tenantId" }`. Optimistic list update + selection.
2. **Send message** — `POST /api/sessions/{sessionId}/messages` with `content` and **`taskQueue`** from the selected preset. Backend returns **`runId`**; SSE or WebSocket streams events. **Human WAITING** → worker **`input.options`**; **`POST /api/runs/{runId}/human-input`** resumes the workflow.
3. **Run events** — **`runEventsStore`** persists the last **200** workflow events per `runId` (deduped on replay). **Events** panel shows last **25**. **`GET /api/health`** polled for disconnected state.

---

## Project layout (relevant to chat)

### API

| File | Purpose |
|------|---------|
| **api/chatApi.ts** | Health, **`GET /api/ui/context`**, tenants, sessions, messages, runs (SSE, response, human-input). Base: `VITE_API_BASE` + `/api`. |
| **api/documentsUploadApi.ts** | **`POST /api/resource/upload`** — multipart upload; optional `taskQueue` / `pipelineId` from env. |
| **api/rest.ts** | Tenant CRUD via `/api/v1/tenants` (tenant config UI is currently disabled in `App.tsx`). |

### Stores (`src/store/`)

| Store | Purpose |
|-------|---------|
| **ui.ts** | Panels, theme, navigation, Events bell unread. |
| **chatSessions.ts** | Session list and selected session. |
| **conversationPanel.ts** | **selectedProfileId**, **selectedQueueId**, **selectedPipelineId** from the active preset. |
| **runEvents.ts** | Run events (SSE/WebSocket); persist last 200; dedupe; hydrate. |
| **sessionDisplay.ts** | Custom session titles and first-message previews. |
| **tenantConfig.ts** | Tenant list from `GET /api/tenants` (config form not exposed in nav). |

### Components

| File | Purpose |
|------|---------|
| **ChatView.tsx** | Messages, composer, preset select, config pills, Run again, human-input, worker progress strip. |
| **ToolsPanel.tsx** | **New chat**, sessions list, delete one/all (delete-all scoped by preset queue/pipeline). |
| **App.tsx** | **`getUiContext()`**, **`useBackendReachable()`**, passes `chatProfiles` downstream. |

### Lib and config

| File | Purpose |
|------|---------|
| **lib/queueDisplayName.ts** | Strips version suffix from queue names (e.g. `ask:1.0` → `ask`). |
| **lib/chatProfileUi.ts** | Emoji and preset labels for dropdown and Run again menu. |
| **lib/wsUrl.ts** / **lib/wsSingleton.ts** | WebSocket URL, shared connection, `SUBSCRIBE_RUN`. |
| **hooks/useWebSocketLiveness.ts** | PING/PONG liveness into `runEventsStore`. |
| **hooks/useBackendReachable.ts** | Polls `GET /api/health`. |
| **types/layout.ts** | Sections: Chat, Knowledge, Documents. |
| **routes.ts** | Path `/chat/conversation` default; query `menu`, `tools`, `props`. |

---

## Configuration

- **Backend URL** — `VITE_API_BASE=http://localhost:7080` in `.env.development`.
- **Chat profiles** — Backend **`olo.configuration.dir`**; not configured in the frontend.
- **Documents upload** — `VITE_CAPABILITY_SOURCE_OPTIONS`; optional `VITE_RESOURCE_UPLOAD_QUEUE` / `VITE_RESOURCE_UPLOAD_PIPELINE`.
- **Panels** — Widths in `localStorage` (`olo:panel-widths`); open/closed in URL query.

---

## Requirements and scripts

- **Node.js 18+**, npm or pnpm.
- **Build**: `npm run build`; **preview**: `npm run preview`.
- **Storybook**: `npm run storybook`.
- Store discipline: [src/store/README.md](../src/store/README.md).
