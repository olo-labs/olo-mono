<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->
# olo-be

## Contributor and owner notes

Backend contributors should read [docs/README.md](docs/README.md), the root [CONTRIBUTING.md](../CONTRIBUTING.md), and [../docs/STABILITY.md](../docs/STABILITY.md) before changing public APIs. Backend owners should keep endpoint docs, environment variables, validation behavior, and refresh semantics current. Credit backend ownership in [../docs/MODULE_OWNERS.md](../docs/MODULE_OWNERS.md).

Spring Boot REST backend for Olo.

## Requirements

- Java 17+
- Gradle 8.5+ (or use the wrapper: `./gradlew` / `gradlew.bat` after running `gradle wrapper` once if the wrapper jar is missing)

## Build

```bash
./gradlew build
# or: gradle build
```

## Run

```bash
./gradlew bootRun
```

Or on Windows:

```bash
gradlew.bat bootRun
```

API runs at `http://localhost:8082`. All endpoints are versioned under `/api/v1/` (e.g. health: `GET /api/v1/health`, tenants: `GET /api/v1/tenants`).

## Environment variables

| Variable | Default | Description |
|----------|---------|-------------|
| **SERVER_PORT** | `8082` | Port the backend listens on. |
| **SPRING_DATA_REDIS_HOST** | `localhost` | Redis host. |
| **SPRING_DATA_REDIS_PORT** | `6379` | Redis port. |
| **OLO_TENANT_IDS** | `olo:tenants` | Redis key for the tenant list. |
| **OLO_CONFIGURATION_DIRECTORY** | `../../olo-mono/olo-definition/olo-configuration/current-active` | Active workflow JSON folder. |
| **OLO_LOG_DIRECTORY** | — | Optional graph injection log folder. |
| **SPRING_AUTOCONFIGURE_EXCLUDE** | — | Set to `org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration` to disable Redis (in-memory store). |
| **SPRING_DATA_REDIS_PASSWORD** | — | Redis password if required. |

**How to set:** Export in the shell, use an env file with Docker/Compose, or set in your deployment. Full reference: [docs/ENVIRONMENT.md](../docs/ENVIRONMENT.md) (repo root). Sample files: [.env.dev.sample](../.env.dev.sample), [.env.production.sample](../.env.production.sample), [.env.demo.sample](../.env.demo.sample).

## API overview

| Area | Endpoints |
|------|-----------|
| Health | `GET /api/v1/health` |
| Tenants | `GET/POST/PUT/DELETE /api/v1/tenants` |
| Workflows (active folder) | `GET/PUT/DELETE /api/v1/configuration/workflows`, `GET .../meta/root` |
| **Scenario folders** | `GET /api/v1/configuration/folders`, `POST /api/v1/configuration/folders/{id}/activate` |
| Graph logs | `GET /api/v1/configuration/logs`, `GET .../meta/root` |
| Catalog | `GET /api/v1/catalog` |
| **Stack refresh** | `POST /api/v1/system/refresh` (worker Redis + olo runtime reload) |
| Worker refresh (legacy) | `POST /api/v1/worker/refresh` |

### Scenario activation

`POST /api/v1/configuration/folders/{folderId}/activate`:

1. Clears `current-active/`
2. Recursively copies `olo-configuration/{folderId}/` into `current-active/`
3. Writes `.olo-active-source` marker
4. Calls `POST /api/v1/system/refresh` (Redis worker signal + olo runtime reload)

Used by olo-ui **Administration → Scenarios**.

## Tenant list: Redis and in-memory fallback

- **With Redis:** Set env `OLO_TENANT_IDS` (e.g. `olo:tenants`) for the Redis key. Tenant list is read/written there.
- **Without Redis:** If Redis is disabled or unavailable, the backend uses an **in-memory tenant store**. Tenant CRUD works; data is lost on restart. No Redis is required for first-run or local contribution. **Worker refresh requires Redis.**

## Docs

See [docs/](docs/) for API and configuration notes and links to repo root docs.
