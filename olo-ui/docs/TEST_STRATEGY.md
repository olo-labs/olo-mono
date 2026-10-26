<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->
# Test strategy

## Contributor and owner notes

Testing contributors can add focused unit, integration, API, Storybook, and regression coverage. Owners should ask for tests that match risk rather than blanket snapshots. Credit testing stewardship and review areas in [MODULE_OWNERS.md](MODULE_OWNERS.md).

Define patterns before contributors invent their own.

---

## What to test

| Area | Approach | Avoid |
|------|----------|--------|
| **Stores** | Unit tests: actions, selectors, initial state. Mock API layer. | Testing implementation details; testing Zustand internals |
| **Forms** | Component tests: user input, submit, validation messages. Use Testing Library. | Snapshot tests of entire form DOM |
| **Routing** | Tests for path parsing and building: `parsePath()`, `buildPath()`, default routes. | Snapshot tests of router output |
| **API layer** | Unit tests with fetch mock: request URL, method, body; response handling. | No snapshot-heavy or fragile tests |

---

## What not to do

- **No snapshot-heavy tests** — Avoid large DOM or JSON snapshots that break on every UI copy or field reorder.
- **Prefer behavior over implementation** — Test “when user clicks Save, POST is called with payload” rather than “state.x is set”.
- **Mock at boundaries** — Mock `fetch` / API in store tests; mock stores in component tests when needed.

---

## Current coverage

- **`src/routes.test.ts`** — `parsePath()`, `buildPath()`, `buildPathWithTenant()`, default and run-level routing, tenant query param building. **Routing stability:** invalid sub-option fallback, run-level invalid tab fallback, missing runId → list view fallback.
- **`src/store/ui.test.ts`** — UI store navigation updates, properties auto-collapse on section change (tenant selection clearing is done in App; store stays decoupled).
- **`src/App.routing.test.tsx`** — Router integration: disabled section deep link redirects to DEFAULT_PATH; run-level deep link with tenant query syncs store and renders correct view.

Run: `npm run test` (or `npm run test:watch`). Lint: `npm run lint` in olo-ui (enforces no API in components, no store→store imports).

## Tools

- **Vitest** for unit and component tests (see `olo-ui/package.json` scripts: `test`, `test:watch`).
- **React Testing Library** for components: `render`, `screen`, `userEvent`.
- **MSW** (optional) for API mocking in tests.

When adding tests, follow these patterns so the suite stays maintainable.
