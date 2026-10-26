<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->

# Store discipline: one store per domain

**Rule: one store per domain, not per component.**

Stores are scoped by **domain** (runtime, ledger, configuration, etc.), not by UI component. This keeps state meaningful and avoids store explosion.

- **What is a domain?** See **docs/ARCHITECTURE.md** §3 — a domain owns server data lifecycle, has its own loading/error states, spans multiple views, and evolves independently. Do not create per-screen stores (e.g. `treeStore`, `timelineStore`).
- **Store shape:** Every domain store that touches the API must have `loading`, `error`, and `clearError()`. See **docs/ARCHITECTURE.md** §3 (Store shape convention).

## Current stores

| Store | Domain | Purpose |
|-------|--------|---------|
| `ui.ts` | App shell | Panels (expanded state, widths persisted), navigation (sectionId, subId, runId, tenantId), theme. **`runEventsBellUnread`** — left footer Events bell highlight when new workflow events arrived while the properties (Events) panel was closed; cleared when the panel opens. URL sync in App. |
| `chatSessions.ts` | Chat sessions | Session list and selected session ID. Fetched tenant-wide; updated on create/delete (optimistic where applicable). |
| `conversationPanel.ts` | Conversation panel | **selectedProfileId**, **selectedQueueId**, **selectedPipelineId** from the active chat preset (`GET /api/ui/context` `chatProfiles`). Used at send time and for delete-all scoping. |
| `runEvents.ts` | Run execution | Current run ID and run events (SSE + WebSocket). **`ChatView` subscribes to `s.events`** as the chat timeline (human card, progress). `addEvent` (**dedupes** workflow events so hydrate + replay do not duplicate), `setRun` / **`setRun(null)`**, `hydrate`, `setOnRunEventCallback`, **`setOnWorkflowEventAppended`** (**`AppShell`** → **`useAppEffects`** → **`runEventsBellUnread`** for new workflow appends only). **Last 200** non-liveness events persisted to **`localStorage`** per `runId`; **`sessionStorage`** tracks last active run per chat session. Liveness (PING/PONG) stored in memory; EventsList filters. |
| `sessionDisplay.ts` | Session display | Per-session custom title and first-message preview. Persisted (localStorage), capped 80 entries. Used for list labels and delete cleanup. |
| `tenantConfig.ts` | Configuration (tenants) | Tenant list (from GET /api/tenants), selection, CRUD (save/update/delete), loading, “adding new”. |

| `knowledgeIngest.ts` | Knowledge (RAG) | Ingest job state for Knowledge Create/Status views. |
| `documentUploads.ts` | Documents | Upload queue and per-file status. |

## Placeholder stores (stub files exist)

| Store | Domain | Status |
|-------|--------|--------|
| `runtime.ts` | Runtime | Stub — reserved for future live-run views |
| `ledger.ts` | Ledger | Stub — reserved for historical run views |
| `runContext.ts` | Run context | Stub |
| `plugins.ts` | Plugins | Stub |
| `schema.ts` | Studio / schema | Stub |

## Lifecycle, errors, side effects, runContext

- **Stores own data lifecycle.** Polling and stream subscriptions (e.g. runtime live runs) belong in the domain store. Expose subscribe/unsubscribe if needed; do not put streaming or polling logic in App.
- **Domain-specific API errors belong in the owning store.** Each store holds its own `error` (or similar) for API failures; components read from the store. Do not put API errors in the UI store or component state.
- **Side effects only in store actions.** API calls, subscriptions, and timers belong in store actions. Components stay declarative: they read from stores and call store actions; they do not call the API layer directly.
- **runContext** holds summary/normalized data only. Heavy raw data (execution trees, event logs, large snapshots) stays in runtime/ledger store; runContext stays small to avoid re-render and memory issues.

See **docs/ARCHITECTURE.md** for store conventions and domain boundaries.

## Usage

- Use Zustand `create()` and export the hook and optionally `getState()` for non-React callers.
- Keep domains meaningful: e.g. `runtime` = everything about “current runtime view” (runs list, selected run, metrics filters).
- Cross-domain data (e.g. “current run”) can live in `ui.ts` (e.g. runId in URL) or in a small `runContext.ts` if both Runtime and Ledger need the same run payload.
