<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->
# Copy-paste content for Docker Hub repository page

## Contributor and owner notes

Keep this frontend Docker Hub copy in sync with the canonical root Docker Hub page. Contributors can help by validating commands and mounted workflow-folder examples; owners should update both docs together. Credit release and Docker stewardship in [../../docs/MODULE_OWNERS.md](../../docs/MODULE_OWNERS.md).

**Canonical version:** [../../docs/DOCKERHUB-PAGE.md](../../docs/DOCKERHUB-PAGE.md) (repo root docs).

Use the text below in your Docker Hub repository **Description** and **Short description**.

---

## Short description (Docker Hub "Short Description" field)

```
Olo Studio: Spring Boot API + React workflow builder in one container. Port 80.
```

---

## Full description (copy from here)

```
Olo — backend (olo-be) + frontend (olo-ui) in one container.

  • nginx serves the React UI on port 80
  • Spring Boot API on 8082 (proxied as /api)
  • Workflow builder: graph canvas, variables, tools, hooks, agent presets
  • Read-only Log view: runtime-injected graphs from olo-configuration/log/
  • Reads/writes olo-configuration JSON from a configurable folder

Quick start
----------
docker run -p 3000:80 YOUR_DOCKERHUB_USERNAME/olo-ui

Open http://localhost:3000 — no separate backend container needed.

Default landing: Workflows → Builder (/workflows/builder).

Environment variables (optional)
--------------------------------
Pass with -e or --env-file:

  SERVER_PORT=8082
  SPRING_DATA_REDIS_HOST=localhost
  SPRING_DATA_REDIS_PORT=6379
  OLO_TENANT_IDS=olo:tenants

  # Workflow preset folder (local path or Drive-synced directory in the container)
  OLO_CONFIGURATION_DIRECTORY=/data/olo-configuration

  # Dynamic subgraph injection logs (optional; default: {configuration}/log or sibling log/)
  OLO_LOG_DIRECTORY=/data/olo-configuration/log

To disable Redis (in-memory tenants, data lost on restart):
  SPRING_AUTOCONFIGURE_EXCLUDE=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration

Example with mounted workflow folder:
  docker run -p 3000:80 ^
    -v C:\path\to\olo-configuration:/data/olo-configuration ^
    -e OLO_CONFIGURATION_DIRECTORY=/data/olo-configuration ^
    YOUR_DOCKERHUB_USERNAME/olo-ui

Mount olo-core catalog into the image build context before docker build, or rebuild
olo-be so syncExtensionCatalog copies dist/catalog/*.json.

Full env reference: see repo docs/ENVIRONMENT.md

Tags
----
  latest — built from main
  1.0.0, 1.0, 1 — semantic versions

Source
------
https://github.com/YOUR_ORG_OR_USER/olo-ui
```

---

## Before publishing

Replace placeholders:

- **YOUR_DOCKERHUB_USERNAME** — Docker Hub username or organization
- **YOUR_ORG_OR_USER** — GitHub org or user (source URL)

---

## CI

Image build and push: [.github/workflows/docker-publish.yml](../../.github/workflows/docker-publish.yml). Secrets: `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`.
