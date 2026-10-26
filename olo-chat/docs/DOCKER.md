<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->

# Docker for olo-chat (frontend)

This document describes how to build and run the olo-chat frontend as a Docker container: environment variables, Docker Compose variants, local build/run, and GitHub Actions (ghcr.io and Docker Hub).

---

## Overview

- **Dockerfile** — Multi-stage: stage 1 uses Node to build the Vite app (`npm ci`, `npm run build`); stage 2 uses Nginx to serve static `dist` on **port 80** and **proxy** `/api` and `/ws` to the backend at runtime.
- **Runtime backend URL** — Set **`OLO_BACKEND_URL`** when starting the container (default `http://olo:7080`). Nginx uses `nginx.conf.template` + `docker-entrypoint.sh` to inject the proxy target. No rebuild required when only the backend URL changes.
- **Build-time `VITE_*` vars** — Optional. When `VITE_API_BASE` is empty (default in the Dockerfile), the browser uses same-origin `/api` and `/ws` through the nginx proxy. Set `VITE_API_BASE` at build time only when the frontend must call a different origin directly.
- **GitHub Actions** — Workflow (`.github/workflows/docker-build.yml`, name **Publish Docker image**) runs on push to `main`/`master` and on manual `workflow_dispatch`. Builds the image and pushes to GitHub Container Registry (ghcr.io). Docker Hub push requires repository secrets `DOCKERHUB_USERNAME` and `DOCKERHUB_TOKEN`.

---

## Environment variable definitions

These are the only environment variables the frontend uses. They must be set at **build time** (Docker build args or CI variables).

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| **VITE_API_BASE** | No (proxy mode) | _(empty)_ | When empty, browser uses same-origin `/api` via nginx proxy. Set only when the SPA must call a remote API origin directly. |
| **VITE_WS_ACCESS_TOKEN** | No | _(empty)_ | Optional WebSocket access token. If set, it is used when the app has no token in `sessionStorage` (e.g. before login). Prefer setting the token at runtime via login and `sessionStorage.accessToken`. |
| **VITE_WS_PING_INTERVAL_SEC** | No | `10` | WebSocket ping interval (and reconnect delay) in seconds. Used for liveness checks. |
| **VITE_CAPABILITY_SOURCE_OPTIONS** | No | _(empty)_ | Comma-separated **capability source** ids for the Documents upload dropdown (e.g. `product-docs,legal`). Preferred over `VITE_RAG_OPTIONS`. |
| **VITE_RAG_OPTIONS** | No | _(empty)_ | Legacy alias for `VITE_CAPABILITY_SOURCE_OPTIONS` if the new variable is unset. |
| **VITE_RESOURCE_UPLOAD_QUEUE** | No | _(empty)_ | Optional workflow task queue after resource upload (e.g. indexing). Preferred over `VITE_RAG_QUEUE`. |
| **VITE_RESOURCE_UPLOAD_PIPELINE** | No | _(empty)_ | Optional pipeline id for that post-upload workflow. Preferred over `VITE_RAG_PIPELINE`. |
| **VITE_RAG_QUEUE** | No | _(empty)_ | Legacy: used if `VITE_RESOURCE_UPLOAD_QUEUE` is unset. |
| **VITE_RAG_PIPELINE** | No | _(empty)_ | Legacy: used if `VITE_RESOURCE_UPLOAD_PIPELINE` is unset. |

**Where used** — `VITE_API_BASE`, `VITE_WS_ACCESS_TOKEN`, `VITE_WS_PING_INTERVAL_SEC` in `api/chatApi.ts`, `lib/wsUrl.ts`, `hooks/useWebSocketLiveness.ts`. Upload / capability-source vars in `api/documentsUploadApi.ts` and Documents UI.

### Dockerfile build args today

The checked-in **`Dockerfile`** declares **ARG/ENV** only for:

- `VITE_API_BASE`
- `VITE_WS_ACCESS_TOKEN`
- `VITE_WS_PING_INTERVAL_SEC`

Upload-related `VITE_*` variables are documented above but **not** passed through the Dockerfile unless you add matching `ARG`/`ENV` lines and rebuild. For Documents upload defaults in Docker, extend the Dockerfile or set capability sources in the UI without build-time ids.

### Runtime variable (container start)

| Variable | Default | Description |
|----------|---------|-------------|
| **OLO_BACKEND_URL** | `http://olo:7080` | Backend URL for nginx to proxy `/api` and `/ws`. Set in Compose `environment` or `docker run -e`. |

### Notes

- **CORS**: Not required when using same-origin proxy mode (empty `VITE_API_BASE`).
- **Direct API mode**: Set `VITE_API_BASE` at build time when the SPA talks to the backend origin directly (dev server or legacy deployments).
- **Chat profiles**: Not a frontend env var. The **olo backend** must serve `GET /api/ui/context` with `chatProfiles` from **`olo.configuration.dir`** (workflow JSON under e.g. `olo-mono/olo-definition/olo-configuration/current-active/`). Without that, the chat UI shows “No chat profiles configured.”

---

## Docker Compose

Sample Compose files are provided for local development and production.

### Development (`docker-compose.dev.yml`)

Proxies `/api` and `/ws` to the `olo` backend on the external `olo-net` network. Requires the backend stack to be running on that network.

```bash
docker compose -f docker-compose.dev.yml up --build
```

- Frontend: **http://localhost:3000**
- Runtime env: `OLO_BACKEND_URL` (default `http://olo:7080`)

### Demo (`docker-compose.demo.yml`)

For a quick demo: frontend only, with default backend URL `http://localhost:7080`. You can override with env (e.g. a remote demo API). Optionally add a backend service in the same file for an all-in-one demo.

```bash
# Backend on host at 7080:
docker compose -f docker-compose.demo.yml up --build

# Or point at a remote demo API (rebuild so the URL is baked in):
VITE_API_BASE=https://demo-api.example.com docker compose -f docker-compose.demo.yml up --build
```

- Frontend: **http://localhost:3000**
- `VITE_API_BASE` defaults to `http://localhost:7080`; set in env to override.
- To run frontend + backend in one stack, uncomment the `backend` service in `docker-compose.demo.yml`, set the backend image (e.g. your olo backend image), then run the same command. The browser will use port 7080 on the host for the API.

### Production (`docker-compose.prod.yml`)

Build args come from the environment. Create a `.env` from `.env.example`, set `VITE_API_BASE` (and optionally the others), then run.

```bash
cp .env.example .env
# Edit .env and set VITE_API_BASE (e.g. https://api.myolo.com)

docker compose -f docker-compose.prod.yml up --build -d
```

- Frontend: **http://localhost:80** (or map another port in the compose file).
- `VITE_API_BASE` is **required** (Compose will error if unset). Use the URL that the **browser** should use to reach the API (e.g. `https://api.myolo.com`).
- Optional: `VITE_WS_ACCESS_TOKEN`, `VITE_WS_PING_INTERVAL_SEC` (default `10`).
- Container runs with `restart: unless-stopped`.

| File | Purpose |
|------|---------|
| **docker-compose.dev.yml** | Dev: frontend only, backend on host at 7080, port 3000. |
| **docker-compose.demo.yml** | Demo: frontend only (or add backend service); port 3000, optional env override for backend URL. |
| **docker-compose.prod.yml** | Prod: frontend with build args from `.env`, port 80, restart policy. |
| **.env.example** | Template for production `.env` (VITE_API_BASE, etc.). |

---

## Building the Docker image locally

### With defaults (backend at `http://localhost:7080`)

```bash
docker build -t olo-chat .
```

### With custom backend URL (and optional env)

```bash
docker build -t olo-chat \
  --build-arg VITE_API_BASE=https://api.myolo.com \
  --build-arg VITE_WS_PING_INTERVAL_SEC=15 \
  .
```

### With WebSocket token (optional, usually not needed for production)

```bash
docker build -t olo-chat \
  --build-arg VITE_API_BASE=https://api.myolo.com \
  --build-arg VITE_WS_ACCESS_TOKEN=your-token \
  .
```

---

## Running the container

The image serves the app on **port 80** inside the container.

### Map port to host (e.g. 3000)

```bash
docker run -p 3000:80 olo-chat
```

Open `http://localhost:3000`.

### Example with name and restart policy

```bash
docker run -d --name olo-chat-ui -p 3000:80 --restart unless-stopped olo-chat
```

---

## GitHub Actions: build and push

### Trigger

- **Push** to branches `main` or `master`: workflow runs, builds the image, and pushes to GitHub Container Registry (ghcr.io). If Docker Hub secrets are set, it also pushes to Docker Hub.
- **Manual**: Run the workflow from the **Actions** tab (**Build and push Docker image** → **Run workflow**). Use the **push** input to choose whether to push to registries (default: true).

### Where to set build configuration

- **Repository variables** (Settings → Secrets and variables → Actions → Variables):
  - **VITE_API_BASE**: Backend base URL used at build time (e.g. `https://api.myolo.com`). If not set, defaults to `http://localhost:7080`.
  - **VITE_WS_PING_INTERVAL_SEC**: Optional; default `10`.
- **Repository secrets** (Settings → Secrets and variables → Actions → Secrets):
  - **VITE_WS_ACCESS_TOKEN**: Optional; only if you want a token baked in for WebSocket (usually leave unset).
  - **DOCKERHUB_USERNAME**: Your Docker Hub username. If set, the workflow also pushes the image to Docker Hub as `docker.io/<DOCKERHUB_USERNAME>/olo-chat`.
  - **DOCKERHUB_TOKEN**: Docker Hub access token (Settings → Security → Access tokens in Docker Hub). Required for push when **DOCKERHUB_USERNAME** is set.

### Image location and tags

- **GitHub Container Registry**: `ghcr.io/<owner>/<repo>` (e.g. `ghcr.io/myorg/olo-chat`).
- **Docker Hub** (when secrets are set): `docker.io/<DOCKERHUB_USERNAME>/olo-chat`.
- **Tags**: branch name, Git SHA, and `latest` (only for `main`/`master`).

### Pull and run (after workflow has run)

**From GitHub Container Registry:**

```bash
docker pull ghcr.io/<owner>/<repo>:latest
docker run -p 3000:80 ghcr.io/<owner>/<repo>:latest
```

**From Docker Hub:**

```bash
docker pull <DOCKERHUB_USERNAME>/olo-chat:latest
docker run -p 3000:80 <DOCKERHUB_USERNAME>/olo-chat:latest
```

For a private ghcr.io repo, create a PAT with `read:packages` and:

```bash
echo <PAT> | docker login ghcr.io -u <user> --password-stdin
```

---

## File reference

| File | Purpose |
|------|---------|
| **Dockerfile** | Multi-stage build: Node build, then Nginx serving `dist`. |
| **nginx.conf** | Nginx config: SPA fallback to `index.html`, static asset caching. |
| **.dockerignore** | Excludes `node_modules`, `dist`, `.git`, env files, tests, etc. |
| **docker-compose.dev.yml** | Development: frontend only, backend on host at 7080. |
| **docker-compose.demo.yml** | Demo: frontend with optional backend; port 3000, env override for API URL. |
| **docker-compose.prod.yml** | Production: frontend with build args from `.env`. |
| **.env.example** | Example env vars for production Compose. |
| **.github/workflows/docker-build.yml** | Builds and (on main/master) pushes the image to ghcr.io and optionally to Docker Hub when **DOCKERHUB_USERNAME** and **DOCKERHUB_TOKEN** are set. |

---

## Troubleshooting

- **Blank page or wrong API URL** — Rebuild the image with the correct `VITE_API_BASE` for the environment where the app is served. The browser must be able to reach that URL for REST and WebSocket.
- **CORS errors** — Configure the olo backend to allow the origin of the frontend (scheme + host + port). The origin is where the user opens the app (e.g. `http://localhost:3000` or your production domain).
- **WebSocket fails** — Ensure `VITE_API_BASE` uses the same host/port the browser should use for WebSocket (e.g. `wss://` in production). The app derives the WebSocket URL from `VITE_API_BASE` (e.g. `https://api.example.com` → `wss://api.example.com/ws`).
- **Documents upload dropdown empty** — Set `VITE_CAPABILITY_SOURCE_OPTIONS` or `VITE_RAG_OPTIONS` at build time (comma-separated capability source ids) and rebuild.

---

## Related docs

- [README.md](./README.md) — Overview and run instructions.
- [ARCHITECTURE.md](./ARCHITECTURE.md) — App architecture and build-time env.
- [DOCKER_HUB_DESCRIPTION.md](./DOCKER_HUB_DESCRIPTION.md) — Copy-paste description for Docker Hub image page.
