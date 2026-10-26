<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->
# Olo (frontend)

## Contributor and owner notes

Frontend contributors should read [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), [src/store/README.md](src/store/README.md), and the root [CONTRIBUTING.md](../CONTRIBUTING.md). UI owners should keep Storybook, route behavior, store boundaries, and builder docs current. Credit frontend ownership in [../docs/MODULE_OWNERS.md](../docs/MODULE_OWNERS.md).

REST-oriented frontend for Olo. Built with React, TypeScript, and Vite. Proxies `/api` to the backend (olo-be). **Build is Gradle-based** (Gradle runs Node/npm via the Node plugin).

## Requirements

- **Gradle 8.5+** (or use the wrapper: `./gradlew` / `gradlew.bat`). If the wrapper jar is missing, run `gradle wrapper` once (requires Gradle on PATH).
- Node.js 18+ (or let Gradle download it via the Node plugin)

## Setup

```bash
./gradlew npmInstall
# or: npm install
```

## Run

```bash
npm run dev
```

App runs at `http://localhost:3000`. Ensure olo-be is running on port 8082 for API calls.

## Build (Gradle)

```bash
./gradlew build
```

This runs `npm install` and `npm run build` via Gradle. Output is in `dist/`. To preview:

```bash
npm run preview
```

## Docker (combined olo-be + olo-ui in one container)

The **repo root** has a combined Dockerfile that builds both the backend (olo-be) and frontend (olo-ui) and runs them in a single container. Build from the **repository root** (parent of `olo-ui/` and `olo-be/`):

```bash
# From repo root (directory that contains olo-ui and olo-be)
docker build -t olo -f Dockerfile .
docker run -p 3000:80 olo
```

Open `http://localhost:3000`. The UI is served by nginx; `/api` is proxied to the backend (Spring Boot) inside the same container. No separate olo-be process needed.

Images are published to Docker Hub via GitHub Actions. See [.github/workflows/docker-publish.yml](../.github/workflows/docker-publish.yml). Configure repository secrets `DOCKERHUB_USERNAME` and `DOCKERHUB_TOKEN` (Docker Hub access token). Pushes to `main` and version tags (`v*`) trigger a build and push. Copy-paste content for the Docker Hub repository page is in [docs/DOCKERHUB-PAGE.md](docs/DOCKERHUB-PAGE.md).

## Storybook (UI in isolation)

Develop and review UI components without running the backend or Redis:

```bash
npm run storybook
```

Stories for TenantConfigurationList, TenantConfigForm, and ToolsPanel use mock data. Add stories in `src/**/*.stories.tsx` for new components.

## Docs

- **This app:** [docs/](docs/) — frontend-specific docs (e.g. [DOCKERHUB-PAGE.md](docs/DOCKERHUB-PAGE.md)); [docs/README.md](docs/README.md) indexes root docs.
- **Architecture (contributors):** [docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md) — state philosophy, naming, store vs local state, store creation guidelines.
- **UI/UX and implementation:** [docs/UI-UX-AND-IMPLEMENTATION.md](../docs/UI-UX-AND-IMPLEMENTATION.md).
- **Configuration scenarios:** [docs/ENVIRONMENT.md](../docs/ENVIRONMENT.md#configuration-scenarios-olo-ui) — activate folders via **Administration → Scenarios**.
