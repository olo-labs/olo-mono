<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->
# Environment variables

## Contributor and owner notes

Environment docs are a high-impact entry point for new users. Contributors can help by testing setup on Windows, macOS, Linux, Docker, and Compose; owners should keep sample env files, defaults, and deployment notes aligned. Credit environment and Docker stewardship in [MODULE_OWNERS.md](MODULE_OWNERS.md).

This document describes how to use environment variables with Olo (olo-be and the combined Docker image). The studio frontend (olo-ui) uses `/api/v1` for olo-be and `/runtime-api` for workflow execution (olo chat/runtime backend on port 7080).

---

## Quick reference (olo-be / combined image)

| Variable | Default | Description |
|----------|---------|-------------|
| **SERVER_PORT** | `8082` | Port the backend listens on (inside the container it must stay 8082 when using the default nginx proxy). |
| **SPRING_DATA_REDIS_HOST** | `localhost` | Redis server host (e.g. `localhost`, `redis`, or your Redis hostname). |
| **SPRING_DATA_REDIS_PORT** | `6379` | Redis server port. |
| **OLO_TENANT_IDS** | `olo:tenants` | Redis key where the tenant list is stored. Set via env or `olo.tenant.ids` in Spring. |
| **SPRING_AUTOCONFIGURE_EXCLUDE** | — | Optional. Set to `org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration` to disable Redis and use in-memory tenant store (data lost on restart). |
| **SPRING_DATA_REDIS_PASSWORD** | — | Optional. Redis password when your Redis server requires auth. |
| **OLO_CONFIGURATION_DIRECTORY** | `../../olo-mono/olo-definition/olo-configuration/current-active` (dev) | Active workflow preset folder (`olo.configuration.directory`). Parent catalog: sibling scenario folders; activate via olo-ui **Administration → Scenarios**. |
| **OLO_LOG_DIRECTORY** | — | Graph log folder (`olo.configuration.log-directory`). When unset, olo-be resolves `{configuration}/log` or sibling `olo-configuration/log`. |
| **OLO_CATALOG_DIRECTORY** | — | Optional override for merged `catalog.json` (Docker image bundles classpath catalog). |
| **olo.worker.refresh-key** / **OLO_WORKER_REFRESH_KEY** | `olo:worker:refresh` | Redis key for worker config reload (`POST /api/v1/system/refresh`). |
| **olo.runtime.base-url** | `http://localhost:7080` | Olo chat/runtime backend for stack refresh and Builder workflow runs. |

### olo-ui frontend (dev)

| Variable | Default | Description |
|----------|---------|-------------|
| **VITE_OLO_RUNTIME_API_BASE** | _(empty — uses `/runtime-api`)_ | Override runtime API base when not using the Vite dev proxy. |

In development, `vite.config.ts` proxies `/api` → `localhost:8082` (olo-be) and `/runtime-api` → `localhost:7080` (workflow execution).

Spring Boot maps environment variables to properties: uppercase with underscores become lowercase with dots (e.g. `OLO_TENANT_IDS` → `olo.tenant.ids`, `SPRING_DATA_REDIS_HOST` → `spring.data.redis.host`).

---

## Configuration scenarios (olo-ui)

Scenario folders live under `olo-definition/olo-configuration/` (e.g. `log-rca-analysis`, `travel-planner`). The **active** runtime folder is `current-active/`. The Scenarios tab lists only activatable scenario folders — **`current-active`** and **`log`** (runtime injection logs) are excluded.

| Action | How |
|--------|-----|
| List scenarios | olo-ui **Administration → Scenarios**, or `GET /api/v1/configuration/folders` |
| Activate scenario | Click **Activate** in UI, or `POST /api/v1/configuration/folders/{id}/activate` |
| Refresh worker + studio | Automatic on activate; manual: **Refresh stack** or `POST /api/v1/system/refresh` |

Activation copies the selected folder into `current-active`, clears previous files, writes `.olo-active-source`, and signals the worker via Redis. Worker and studio keep reading `current-active` — no `scanFolder` YAML change required.

---

## How to set environment variables

### 1. Local development (backend only)

- **Option A — Export in the shell before running:**
  ```bash
  export SPRING_DATA_REDIS_HOST=localhost
  export SPRING_DATA_REDIS_PORT=6379
  export OLO_TENANT_IDS=olo:tenants
  cd olo-be && ./gradlew bootRun
  ```
- **Option B — Use a `.env` file (not loaded by Spring by default):** Copy [.env.dev.sample](../.env.dev.sample) to `.env.dev` and source it, or use a tool that loads it:
  ```bash
  set -a; source .env.dev; set +a
  cd olo-be && ./gradlew bootRun
  ```
- **Option C — Without Redis:** Omit Redis or set `SPRING_AUTOCONFIGURE_EXCLUDE=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration`. The backend uses an in-memory tenant store (data lost on restart).

### 2. Docker run (combined image)

Pass variables with `-e` or `--env-file`:

```bash
# With env file (create from .env.production.sample or .env.demo.sample)
docker run -p 3000:80 --env-file .env.production YOUR_IMAGE/olo-ui

# Inline (e.g. with external Redis)
docker run -p 3000:80 \
  -e SPRING_DATA_REDIS_HOST=redis.example.com \
  -e SPRING_DATA_REDIS_PORT=6379 \
  -e OLO_TENANT_IDS=olo:tenants \
  YOUR_IMAGE/olo-ui
```

### 3. Docker Compose

The repo provides sample env files and compose files that use them:

| Env file | Compose file | Use |
|----------|--------------|-----|
| Copy [.env.dev.sample](../.env.dev.sample) → `.env.dev` | [docker-compose.dev.yml](../docker-compose.dev.yml) | Development (optional env_file) |
| Copy [.env.production.sample](../.env.production.sample) → `.env.production` | [docker-compose.production.yml](../docker-compose.production.yml) | Production |
| Copy [.env.demo.sample](../.env.demo.sample) → `.env.demo` | [docker-compose.demo.yml](../docker-compose.demo.yml) | Demo |

Compose injects the file specified in `env_file:` into the service container. You can override or add variables in the `environment:` block in the compose file.

**Example — first run with production compose:**
```bash
cp .env.production.sample .env.production
# Edit .env.production if needed (e.g. Redis password)
docker compose -f docker-compose.production.yml up -d
```

### 4. Kubernetes / cloud

Set the same variables in your Deployment or Pod spec (e.g. `env` or `envFrom` with a ConfigMap or Secret). Use the same names as in the table above.

---

## Sample files in this repo

- **[.env.dev.sample](../.env.dev.sample)** — Development (backend port, Redis, OLO_TENANT_IDS).
- **[.env.production.sample](../.env.production.sample)** — Production (Redis, optional password, optional exclude).
- **[.env.demo.sample](../.env.demo.sample)** — Demo (same as production; optional demo-specific Redis key).

Copy the relevant sample to `.env.dev`, `.env.production`, or `.env.demo` and adjust. **Do not commit real `.env` files with secrets.**

---

## Frontend (olo-ui)

The UI is built as static assets and talks to the backend at `/api/v1`. In development, Vite proxies `/api` to `http://localhost:8082` (see [olo-ui/vite.config.ts](../olo-ui/vite.config.ts)). No environment variables are required for the frontend in normal use.
