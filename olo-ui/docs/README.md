<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->
# Olo

## Contributor and owner entry points

- New contributors: start with [CONTRIBUTING.md](../CONTRIBUTING.md).
- Module owners and reviewers: use [MODULE_OWNERS.md](MODULE_OWNERS.md) to route reviews, claim scope, and credit stewardship.
- Documentation contributors: replace placeholders, add diagrams, add screenshots, and keep quickstarts current when behavior changes.

Backend (olo-be) and frontend (olo-ui) for Olo. **This directory is the repository root.** It contains both projects; they can run as a **single Docker image** (combined container) or separately for development. Configure the backend via environment variables — see **[docs/ENVIRONMENT.md](ENVIRONMENT.md)**.

## Repository layout

- **Repo root** (this directory): `Dockerfile`, combined run scripts, and CI (`.github/workflows/`).
- **olo-be/** — Spring Boot REST API (Java 17+, Gradle).
- **olo-ui/** — React frontend (TypeScript, Vite, Gradle build).
- **docs/** — Architecture, domain boundaries, and contributor guides (shared).

## Combined Docker image (olo-be + olo-ui in one container)

From this directory (repo root, containing `olo-be/` and `olo-ui/`):

```bash
docker build -t olo -f Dockerfile .
docker run -p 3000:80 olo
```

Then open **http://localhost:3000**. Nginx serves the UI and proxies `/api` to the Spring Boot backend inside the same container.

- **Build:** Backend and frontend are built with Gradle; runtime is Eclipse Temurin 17 JRE + nginx.
- **Entrypoint:** Starts olo-be in the background, then nginx in the foreground.

## Development (run backend and frontend separately)

- **Backend:** See [olo-be/README.md](../olo-be/README.md) — Java 17+, Gradle, `./gradlew bootRun` (port 8082).
- **Frontend:** See [olo-ui/README.md](../olo-ui/README.md) — Node/Gradle, `npm run dev` (port 3000); ensure olo-be is running for API calls.

**Activate workflow scenarios:** olo-ui → **Administration → Scenarios** — copies a scenario folder into `current-active` and refreshes worker + studio. See [docs/ENVIRONMENT.md](ENVIRONMENT.md#configuration-scenarios-olo-ui).

## Environment and Docker Compose

**How to use environment variables:** Full reference (all variables, Docker, Compose, local) is in **[docs/ENVIRONMENT.md](ENVIRONMENT.md)**.

Summary: the backend (olo-be) and combined image use **SERVER_PORT**, **SPRING_DATA_REDIS_HOST**, **SPRING_DATA_REDIS_PORT**, and **OLO_TENANT_IDS** (tenant list Redis key). Optional: **SPRING_AUTOCONFIGURE_EXCLUDE** to disable Redis (in-memory store); **SPRING_DATA_REDIS_PASSWORD** for Redis auth. Set them in the shell, via `--env-file` or `-e` with `docker run`, or in Compose `env_file:` / `environment:`.

Sample env files (copy and rename to use):

| Sample | Copy to | Use with |
|--------|---------|----------|
| [.env.dev.sample](../.env.dev.sample) | `.env.dev` | Development |
| [.env.production.sample](../.env.production.sample) | `.env.production` | Production compose |
| [.env.demo.sample](../.env.demo.sample) | `.env.demo` | Demo compose |

Docker Compose (run from repo root):

| File | Purpose |
|------|---------|
| [docker-compose.dev.yml](../docker-compose.dev.yml) | Redis + optional olo-be in Docker; run frontend on host with `npm run dev`. |
| [docker-compose.production.yml](../docker-compose.production.yml) | Combined app + Redis; persist Redis data. Before first run: `cp .env.production.sample .env.production` |
| [docker-compose.demo.yml](../docker-compose.demo.yml) | Demo stack: combined app + Redis. Before first run: `cp .env.demo.sample .env.demo` |

Example — development with Redis only (backend & UI on host):

```bash
docker compose -f docker-compose.dev.yml up -d redis
cd olo-be && ./gradlew bootRun &
cd olo-ui && npm run dev
```

Example — production-style with Compose:

```bash
cp .env.production.sample .env.production
docker compose -f docker-compose.production.yml up -d
# App at http://localhost:3000
```

## Docker Hub

The combined image is published via GitHub Actions from the **repo root**. See [.github/workflows/docker-publish.yml](../.github/workflows/docker-publish.yml). **Docker Hub description:** copy-paste text for the repository page is in [docs/DOCKERHUB-PAGE.md](DOCKERHUB-PAGE.md).

## Docs

- **Repo root:** [docs/](./) — [Environment variables](ENVIRONMENT.md), architecture, domain boundaries, extensibility, layout contract, performance, stability, test strategy, UI/UX, [Docker Hub description](DOCKERHUB-PAGE.md).
- **Frontend:** [olo-ui/README.md](../olo-ui/README.md) and [olo-ui/docs/](../olo-ui/docs/).
- **Backend:** [olo-be/README.md](../olo-be/README.md) and [olo-be/docs/](../olo-be/docs/).
