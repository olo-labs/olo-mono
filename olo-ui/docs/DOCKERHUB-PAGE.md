<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->
# Docker Hub repository description

## Contributor and owner notes

Docker publishing docs are part of the first-run experience. Contributors can help by verifying commands, tags, mounted configuration paths, and screenshots; owners should keep Docker Hub copy aligned with the image behavior. Credit Docker and release ownership in [MODULE_OWNERS.md](MODULE_OWNERS.md).

Copy-paste the text below into your Docker Hub repository **Description** (and **Short description**) so users know how to pull and run the image.

---

## Short description (Docker Hub "Short Description" field)

One-line summary for search and listing:

```
Olo: Spring Boot backend + React frontend in one container. Single image, port 80.
```

---

## Full description (Docker Hub "Description" / README)

Paste this in the full description or README section:

```
Olo — backend (olo-be) + frontend (olo-ui) in one container. Spring Boot API on 8082, nginx serves the React UI and proxies /api to the backend.

Quick start
----------
docker run -p 3000:80 YOUR_DOCKERHUB_USERNAME/olo-ui

Then open http://localhost:3000 . No separate backend container needed.

Environment variables (optional)
--------------------------------
Pass with -e or --env-file. Backend (Spring Boot) reads:

  SERVER_PORT=8082
  SPRING_DATA_REDIS_HOST=localhost    (or your Redis host)
  SPRING_DATA_REDIS_PORT=6379
  OLO_TENANT_IDS=olo:tenants          (Redis key for tenant list)
  SPRING_DATA_REDIS_PASSWORD=...      (if Redis requires auth)
  OLO_CONFIGURATION_DIRECTORY=/data/olo-configuration   (workflow presets)
  OLO_LOG_DIRECTORY=/data/olo-configuration/log         (runtime graph logs, read-only in UI)

To disable Redis and use in-memory tenant store (data lost on restart):
  SPRING_AUTOCONFIGURE_EXCLUDE=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration

Example with external Redis:
  docker run -p 3000:80 -e SPRING_DATA_REDIS_HOST=redis -e SPRING_DATA_REDIS_PORT=6379 YOUR_DOCKERHUB_USERNAME/olo-ui

See the repo docs/ENVIRONMENT.md for full reference.

Tags
----
- latest — built from main
- 1.0.0, 1.0, 1 — semantic versions (use when we publish v1.0.0 etc.)

Source
------
https://github.com/YOUR_ORG_OR_USER/olo-ui
```

---

## Before publishing

Replace placeholders with your own values:

- **YOUR_DOCKERHUB_USERNAME** — your Docker Hub username or organization
- **YOUR_ORG_OR_USER** — your GitHub organization or username (for the source URL)

---

## CI

The image is built and pushed by GitHub Actions from the repo root. See [.github/workflows/docker-publish.yml](../.github/workflows/docker-publish.yml). Configure repository secrets `DOCKERHUB_USERNAME` and `DOCKERHUB_TOKEN` (Docker Hub access token).
