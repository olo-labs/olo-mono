<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->
# Stability and deprecation policy

## Contributor and owner notes

Stability contributors should document compatibility, deprecation, migration, and support expectations whenever public APIs or saved workflow formats change. Owners should review breaking changes for clear upgrade paths and contributor-facing notes. Credit stability stewardship in [MODULE_OWNERS.md](MODULE_OWNERS.md).

Defining this early builds trust and prevents silent breaking changes.

---

## Versioning

- **v0.x** — Unstable. APIs and UI may change without notice. Use for early development and OSS experimentation.
- **v1.x** — Stable. We follow the deprecation policy below; breaking changes are announced and migrated.

---

## Deprecation policy

1. **Announce before removal.** Any public API (REST endpoint, store method, extension API) that will be removed or changed in a breaking way must be documented as deprecated at least one minor version before removal (e.g. deprecate in 1.2, remove in 1.3).
2. **Document migration.** Deprecation notices must point to the replacement (e.g. “Use `/api/v2/tenants` instead”).
3. **No silent breaking changes.** Avoid changing request/response shapes or behavior in a way that breaks existing clients without a deprecation period.

---

## API versioning

- REST API uses a version prefix: **`/api/v1/...`**. When we introduce breaking changes, we add `/api/v2/...` and keep v1 supported for the deprecation period.
- Frontend uses `API_BASE = '/api/v1'`. New versions are additive (new endpoints or new base path).

This keeps OSS and enterprise users able to rely on stable contracts.
