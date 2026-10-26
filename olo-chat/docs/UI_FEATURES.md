<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->

# UI Features

User-facing features of the olo-chat frontend: layout, sections, panels, URL/query, and interactions.

---

## Overview

Multi-panel layout with URL-driven navigation:

- **Top bar** — Logo (home), theme toggle.
- **Left panel** — **Chat**, **Knowledge**, **Documents**. Chat has **Conversation** only. Tenant is backend-resolved (footer labels from UI context).
- **Center** — Chat: messages, composer, **preset pill** (required), thinking line, config pills, Run again, worker progress strip, human-input card.
- **Tools panel** — **New chat**, sessions list, delete one/all. **No** Queue/Pipeline dropdowns.
- **Properties panel** — Run events (Chat). Tenant config form exists in code but is **not** reachable in navigation (`isTenantConfig = false`).

Panel widths: `localStorage` (`olo:panel-widths`). Open/closed: URL query `menu`, `tools`, `props`.

**Backend availability** — `GET /api/health` polled. Chat column shows “Connecting to Olo backend…” when down.

---

## Top bar

| Feature | Description |
|--------|-------------|
| **Logo** | Navigates to `/chat/conversation` (keeps panel query). |
| **Theme toggle** | Light/dark; persisted as `olo-theme`. |

---

## Left panel

| Feature | Description |
|--------|-------------|
| **Section navigation** | Chat → Conversation; Knowledge → Sources / Create / Status; Documents → Upload. |
| **Effective tenant** | From `GET /api/ui/context` (not user-selectable). |
| **Events bell (footer)** | Opens Events panel. Highlight when **new workflow** events arrive while Events panel was closed. |

Feature flags: `config/features.ts` (chat, knowledge, documents).

---

## Main content — Chat → Conversation

| Feature | Description |
|--------|-------------|
| **Header** | “Chat” / “→ Conversation with Olo AI”. |
| **Preset pill** | `<select>` beside composer: emoji + display name; tooltip from `displaySummary`. **Required** — empty `chatProfiles` shows blocking error. |
| **RAG source pill** | Second `<select>` beside preset: "No RAG" + indexed knowledge sources from `GET /api/knowledge/sources`. Sent as optional `capabilitySource` on send. |
| **Cancel run** | **Cancel** button in `ChatWaitingBanner` while a run is active (`POST /api/runs/{runId}/cancel`). |
| **Thinking** | “{displayName} is thinking…” + `displaySummary` while run active. |
| **Config pills** | Per `runId` on user/assistant bubbles; persisted in `sessionStorage`. |
| **Run again** | Under user messages; menu of other presets with `runAgain: true`. |
| **Human input** | Card on HUMAN WAITING. Plugin-driven **form fields** (`parameters[].type` + `ui.widget`) and **footer actions** (`options`). See [CHAT_UI.md](./CHAT_UI.md#human-input-plugin-forms). |
| **Worker progress** | Collapsible strip below composer; last 200 events summarized; expand/height persisted. |
| **Send** | `taskQueue` from selected preset; disabled until run completes. |
| **Health** | Disconnected state when backend unreachable. |

### Knowledge / Documents

- **Knowledge** — **Sources** list (`KnowledgeSourcesList`), **Create** ingest form (`KnowledgeCreateView` → `POST /api/rag/ingest`), **Status** view (`KnowledgeStatusView`). Chat composer includes an optional **Knowledge source (RAG)** dropdown; selected source is sent as `capabilitySource` on message send.
- **Documents** — Capability source filter, upload modal, table; `POST /api/resource/upload`.

---

## Tools panel (Conversation)

| Feature | Description |
|--------|-------------|
| **New chat** | `POST /api/sessions` with `{ tenantId }`; clears run events store. |
| **Sessions list** | Tenant-wide (`GET .../sessions` without queue filter). Edit title (✎), delete (×). |
| **Delete all** | `DELETE .../sessions?queue=&pipeline=` using **current preset** queue and pipeline. |
| **Knowledge** | Live sources list (`KnowledgeSourcesList`) when section is Knowledge. |

Hidden for Documents section.

---

## Properties panel

| Feature | Description |
|--------|-------------|
| **Run events** | Last **25** workflow events (liveness excluded). Expand for JSON detail. Up to **200** persisted per `runId` in `localStorage`. |
| **Empty state** | “Send a message in Chat to see run events here.” |

---

## URL and query

| Aspect | Description |
|--------|-------------|
| **Path** | `/:sectionId/:subId`; default `/chat/conversation`. |
| **Query** | `menu`, `tools`, `props`. |
| **Last path** | `localStorage` for `/` redirect. |

---

## Resizable panels

| Panel | Default | Min / max | Key |
|-------|---------|-----------|-----|
| Left | 260px | 160–480px | `olo:panel-widths` |
| Tools | 220px | 160–400px | same |
| Properties | 260px | 160–480px | same |

---

## Session display

Custom title (✎), first-message preview, or date/time. `sessionDisplayStore`, max 80 entries.

---

## Related docs

- [ARCHITECTURE.md](./ARCHITECTURE.md)
- [CHAT_UI.md](./CHAT_UI.md)
- [README.md](./README.md)
