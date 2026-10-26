<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->
# olo-be docs

## Contributor and owner notes

This folder is the backend contributor landing area. Add endpoint, configuration, validation, or operational notes here when backend behavior changes; link to root docs for platform contracts. Credit backend reviewers and owners in [../../docs/MODULE_OWNERS.md](../../docs/MODULE_OWNERS.md).

Backend-specific documentation. For run and build instructions, see [olo-be/README.md](../README.md).

## API

- Base path: `/api/v1/`
- Health: `GET /api/v1/health`
- Tenants: `GET /api/v1/tenants` (and related CRUD via dropdown/tenant endpoints)
- Workflows: `GET/PUT/DELETE /api/v1/configuration/workflows` (preset CRUD)
- Graph logs: `GET /api/v1/configuration/logs` (read-only list + `mergedGraph` by file)
- Catalog: `GET /api/v1/catalog`
- Configuration folders: `GET /api/v1/configuration/folders`, `POST /api/v1/configuration/folders/{id}/activate`
- System refresh: `POST /api/v1/system/refresh` (signals worker via Redis)
- Model provider test: `POST /api/v1/model-providers/test`
- Dropdown details: `GET /api/v1/dropdown-details`
- All endpoints are versioned under `/api/v1/`; see [STABILITY.md](../../docs/STABILITY.md) (in repo root docs) for versioning and deprecation.

## Configuration and environment variables

Backend behaviour is controlled by environment variables (or Spring properties). Key ones:

- **SERVER_PORT** — default 8082.
- **SPRING_DATA_REDIS_HOST** / **SPRING_DATA_REDIS_PORT** — Redis connection (optional; if disabled, in-memory tenant store is used).
- **OLO_TENANT_IDS** — Redis key for tenant list (default `olo:tenants`).
- **OLO_CONFIGURATION_DIRECTORY** — workflow preset folder (see `olo.configuration.directory`).
- **OLO_LOG_DIRECTORY** — dynamic subgraph graph logs for read-only Log view (see `olo.configuration.log-directory`).
- **SPRING_AUTOCONFIGURE_EXCLUDE** — set to `org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration` to disable Redis.
- **SPRING_DATA_REDIS_PASSWORD** — Redis password when required.

**Full reference (how to set in local, Docker, Compose, Kubernetes):** [ENVIRONMENT.md](../../docs/ENVIRONMENT.md) in repo root docs. Sample env files are at repo root: `.env.dev.sample`, `.env.production.sample`, `.env.demo.sample`.

## Repo root docs

Shared architecture and contributor docs live at the repository root [docs/](../../docs/) (e.g. ARCHITECTURE.md, STABILITY.md). The combined Docker image is built from repo root; see root [README.md](../../README.md) and [.github/workflows/docker-publish.yml](../../.github/workflows/docker-publish.yml).
