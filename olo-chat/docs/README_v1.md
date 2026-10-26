<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->

# Olo Chat

A chat interface for **Olo** — start conversations, send messages, and follow run events in real time.

## For users

- **Chat** — Pick a **preset** (role) beside the composer: Ask, Fast, Detailed, and others from backend workflow config. Subtitle **Conversation with Olo AI**. While the model runs, you see **who is thinking** and a short **summary** line; message history shows **which preset** was used per turn. **Run again** (under user messages) lists other presets by **emoji + name**. Run events in the **Events** sidebar; **human** steps show **buttons** from the worker.
- **Knowledge** — Manage knowledge sources (`GET /api/knowledge/sources`), ingest (`POST /api/rag/ingest`), and status views.
- **Documents** — Upload files to the backend shared storage.

The app talks to the **Olo backend** for sessions, messages, and events. **Tenant** is determined by the backend; **task queue** comes from the selected preset.

## Quick start

1. Start the **olo backend** (port 7080), e.g. `start.bat` from the `olo` folder.
2. From this directory:

   ```bash
   npm install
   npm run dev
   ```

3. Open **http://localhost:3000**.

For backend setup, environment variables, Docker, and technical details, see **docs** below.

## Documentation

| Doc | What’s inside |
|-----|----------------|
| **[docs/README.md](docs/README.md)** | Overview, backend requirements, run instructions, project layout. |
| **[docs/TECHNOLOGY_ARCHITECTURE.md](docs/TECHNOLOGY_ARCHITECTURE.md)** | Stack, architecture, design, communication. |
| **[docs/UI_FEATURES.md](docs/UI_FEATURES.md)** | Panels, presets, Events, feature flags. |
| **[docs/CHAT_UI.md](docs/CHAT_UI.md)** | Chat APIs, profiles, run events, WebSocket. |
| **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** | State, API layer, components, data flow. |
| **[docs/DOCKER.md](docs/DOCKER.md)** | Docker build/run, env vars, Compose, CI. |
| **[docs/DOCKER_HUB_DESCRIPTION.md](docs/DOCKER_HUB_DESCRIPTION.md)** | Docker Hub image description. |

## Logo and assets

Logo and related assets are in **public/** (e.g. `logo-full.svg`).

---

*Developers: Node 18+, `npm run build`, Storybook, and store discipline — [docs/README.md](docs/README.md) and [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).*
