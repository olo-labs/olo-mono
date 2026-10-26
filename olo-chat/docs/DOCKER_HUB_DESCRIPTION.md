<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->

# Docker Hub description (copy-paste)

Use the content below in Docker Hub for your image's **Full Description**. It is written in Markdown so it renders nicely on the image page.

**Quick copy:** The same copy-paste content is in [.github/DOCKER_HUB_DESCRIPTION.md](../.github/DOCKER_HUB_DESCRIPTION.md) — open that file and copy everything into Docker Hub's Full Description field.

---

## Copy from here (for Docker Hub Full Description)

```markdown
# Olo Chat (frontend)

Chat UI for **Olo** — create sessions, send messages, and stream run events in real time. Pick a **preset** (role) beside the composer; each preset maps to a Temporal **task queue** from backend workflow JSON (`olo.configuration.dir`). **Run again** offers other presets (emoji + name) under user messages. Run events persist locally (last 200 per run) with deduped replay after refresh. This image serves the frontend only; it requires an **Olo backend** with workflow config so `GET /api/ui/context` returns **chatProfiles**.

## Quick run

```bash
docker run -p 3000:80 -e OLO_BACKEND_URL=http://host.docker.internal:7080 <your-dockerhub-username>/olo-chat:latest
```

Open **http://localhost:3000**. By default the image uses same-origin `/api` and `/ws` through nginx. Set **`OLO_BACKEND_URL`** at container start to point the proxy at your backend (default `http://olo:7080` on Docker networks).

## Port

The container serves the app on **port 80**. Map it to the host (e.g. `-p 3000:80`) as in the example above.

## Backend URL

**Runtime (recommended):** set `OLO_BACKEND_URL` when starting the container. Nginx proxies `/api` and `/ws` to that URL. No rebuild required.

**Build-time (direct API mode):** pass `VITE_API_BASE` as a Docker build arg when the SPA must call the backend origin directly:

```bash
docker build -t olo-chat --build-arg VITE_API_BASE=https://api.yourdomain.com .
```

## Optional build args

| Build arg | Default | Description |
|-----------|---------|-------------|
| `VITE_API_BASE` | _(empty)_ | When empty, browser uses same-origin `/api` via nginx proxy. |
| `VITE_WS_PING_INTERVAL_SEC` | `10` | WebSocket ping interval (seconds). |
| `VITE_WS_ACCESS_TOKEN` | _(empty)_ | Optional WebSocket token (usually leave unset). |

Upload-related `VITE_*` variables are not in the Dockerfile unless you extend it.

## CORS and WebSocket

When using proxy mode (empty `VITE_API_BASE`), CORS is not required. WebSocket uses same-origin `/ws` through nginx.

## Full documentation

- **Docker**: [docs/DOCKER.md](https://github.com/YOUR_ORG/olo-chat/blob/main/docs/DOCKER.md) — build, env vars, Docker Compose, GitHub Actions.
- **Project**: [README](https://github.com/YOUR_ORG/olo-chat) and [docs/README.md](https://github.com/YOUR_ORG/olo-chat/blob/main/docs/README.md) for overview and run instructions.

Replace `YOUR_ORG` and `olo-chat` with your GitHub org/repo if different.
```

---

## Copy from here (plain text, no code fence)

If Docker Hub strips the outer code block, use the version inside the fenced block above — same content, no wrapping fence.
