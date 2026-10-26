<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->

# Technology, architecture, design, and communication

Detailed companion to [ARCHITECTURE.md](./ARCHITECTURE.md): technology choices, logical architecture, UI design, and backend communication.

---

## 1. Technology stack

| Area | Choice | Role |
|------|--------|------|
| **Language** | TypeScript | API DTOs, stores, routes. |
| **UI** | React 18 | Components and hooks. |
| **Build** | Vite 5 | HMR, `import.meta.env`, production bundle. |
| **Routing** | React Router 7 | SPA paths and search params. |
| **State** | Zustand 5 | Global stores. |
| **Testing** | Vitest + Testing Library | Unit/component tests. |

Static SPA after `npm run build`; dynamic data from the **olo** backend.

---

## 2. Logical architecture

### 2.1 Layers

```
┌─────────────────────────────────────────────────────────────┐
│  Presentation (React components, CSS)                        │
├─────────────────────────────────────────────────────────────┤
│  Application state (Zustand)                                 │
├─────────────────────────────────────────────────────────────┤
│  API adapters (chatApi, documentsUploadApi)                  │
├─────────────────────────────────────────────────────────────┤
│  Transport: HTTPS REST, SSE, WebSocket                       │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    Olo chat backend (Spring Boot)
                    olo.configuration.dir → chatProfiles
                              │
                              ▼
              Temporal + worker(s) per task queue
```

- No workflow execution logic in the browser.
- **Chat profiles** come from backend filesystem config (`olo-mono/olo-definition/olo-configuration`), not frontend JSON or Redis.

### 2.2 Module boundaries

| Folder | Responsibility |
|--------|----------------|
| `src/components/` | Screens; call APIs and stores. |
| `src/store/` | Cross-cutting state. |
| `src/api/` | HTTP contracts. |
| `src/lib/` | URLs, profile UI helpers, WebSocket. |
| `src/config/` | Feature flags, tool registry. |

---

## 3. Design and UX

### 3.1 Layout

Three-column shell: left nav + tools + main + properties. Resizable widths persisted in `localStorage`.

**Chat** — Preset beside composer (from `chatProfiles`). Tools panel: sessions only.

### 3.2 Chat UX

- **Presets required** — No legacy Queue/Pipeline mode.
- **Messages** — Optimistic user send; assistant from events or `GET .../response`; config pills per preset/run.
- **Run again** — Other presets with `runAgain: true`.
- **Human-in-the-loop** — Worker-defined `input.options`; timeline from `runEventsStore` (survives reload).
- **Worker progress strip** — Summarized event trace below composer; UI-only.
- **Availability** — Health poll; disconnected state when backend down.

### 3.3 Events panel

Last 25 events from `runEventsStore`; hydrate + dedupe from `localStorage`.

---

## 4. Communication with the backend

### 4.1 Base URL

- **`VITE_API_BASE`** — e.g. `http://localhost:7080`; paths are `{base}/api/...`. When empty, the app uses same-origin `/api`.
- **Dev proxy** — `vite.config.ts` proxies `/api` to port 7080 when `VITE_API_BASE` is unset.
- **Docker/production** — nginx serves static `dist` and proxies `/api` and `/ws` to `OLO_BACKEND_URL` (default `http://olo:7080`). See [DOCKER.md](./DOCKER.md).
- **Auth** — Optional Bearer token (`getApiAuthHeaders`, `wsUrl.ts`).

### 4.2 REST

| Concern | Endpoints |
|---------|-----------|
| Health | `GET /api/health` |
| Context | `GET /api/ui/context` — `tenantId`, `chatProfiles` |
| Sessions | `POST /api/sessions`, `GET/DELETE /api/tenants/{id}/sessions` |
| Messages | `GET/POST /api/sessions/{id}/messages` |
| Runs | `GET /api/runs/{runId}`, `.../response`, `.../human-input` |

### 4.3 SSE

`GET /api/runs/{runId}/events` — `text/event-stream`; JSON execution events per line.

### 4.4 WebSocket

`ws(s)://host/ws` — `SUBSCRIBE_RUN` for run events; PING/PONG liveness. Shared via `wsSingleton.ts`.

### 4.5 End-to-end (send → completion)

1. `POST .../messages` → `runId`; `taskQueue` from selected preset.
2. Subscribe SSE or WebSocket.
3. Worker callbacks → backend → stream to client.
4. UI updates stores, assistant text, progress.
5. On completion: refetch messages; re-enable Send.

---

## 5. Related documentation

| Document | Content |
|----------|---------|
| [ARCHITECTURE.md](./ARCHITECTURE.md) | Stores, routes, data flow. |
| [CHAT_UI.md](./CHAT_UI.md) | Chat APIs and profiles. |
| [UI_FEATURES.md](./UI_FEATURES.md) | Feature-level UI. |
| [README.md](./README.md) | Overview and run instructions. |
| [olo/olo-temporal-sdk/docs/ARCHITECTURE.md](../../olo/olo-temporal-sdk/docs/ARCHITECTURE.md) | Backend, Temporal, SDK. |

---

## 6. Operational notes

- **CORS** — Backend must allow the frontend origin if not same-origin proxied.
- **Chat profiles** — Backend must load `olo.configuration.dir` before `chatProfiles` appear.
- **Task queues** — Each preset’s `queue` must have a worker listening on that Temporal task queue.
