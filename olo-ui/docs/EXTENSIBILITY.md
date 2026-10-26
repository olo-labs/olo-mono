<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->
# Extensibility and governance

## Contributor and owner notes

Extension contributors should keep APIs declarative, documented, and safe for module owners to review. If you add a feature flag, plugin hook, tool registration path, or integration point, document the owner and sunset or support expectation. Track sustained ownership in [MODULE_OWNERS.md](MODULE_OWNERS.md).

Extension surface and rules to prevent architecture erosion.

---

## 1. Extension API: declarative only

**Extensions register metadata, not behavior-heavy logic.**

- **Good:** `registerSidebarItem({ id: 'custom', section: 'runtime', label: 'Custom View', path: '/runtime/custom' })` — declarative; host navigates to path.
- **Risky:** `registerSidebarItem({ render: () => ... })` or any API that lets extensions inject logic with store imports or complex behavior. If the extension API becomes too powerful, domains and layout discipline erode.

**Rule:** Extension API = **declarative metadata** + **controlled injection points**. The host owns what gets rendered at each injection point (e.g. Tools panel resolves components by tool `id`/`slot` from a host-owned map). Extensions do not pass `render` functions or import stores.

- **Sidebar:** `id`, `section`, `label`, `path`, `order`. No `render`.
- **Tools:** `id`, `label`, `slot`. No `render`. Host provides the component for each `id`/`slot` when it mounts the Tools panel.
- **Store access:** Extensions may **not** mutate store state directly. They may only call **public store actions** (e.g. passed in as callbacks or via a host-provided API). This prevents plugin-store mutation hacks and keeps domain ownership clear.

---

## 2. Governance risks and rules

### Risk 1 — Silent domain leakage

When runtime and ledger are implemented, developers may import one domain store from another or reuse state across domains.

**Enforce:** Cross-domain sharing only through **URL** or **runContext**. No store imports another domain store.

**Code review checklist:**

- ❓ Does this store import another store?
- ❓ Does this violate DOMAIN_BOUNDARIES.md?

**Optional:** Use ESLint `no-restricted-imports` so that files under `store/*` cannot import from other `store/*` files (except `runContext` if allowed). See DOMAIN_BOUNDARIES.md for a suggested rule.

---

### Risk 2 — runContext becoming a dumping ground (and size explosion)

**runContext** is the only allowed shared domain. It must stay narrow **and small**.

**Allowed in runContext:**

- **Summary/normalized data only:** run id, run status, small shared summary object used by both Runtime and Ledger when viewing the same run.

**Not allowed:**

- Random feature flags, general UI state, or domain-specific state that belongs in `runtime` or `ledger`.
- **Heavy raw data:** full execution trees, unbounded event logs, MB-level snapshots. Those cause re-render cascades and memory spikes; they belong in **runtime** or **ledger** store. runContext stores **summary only**.

Define runContext responsibility narrowly: **run identity + shared run summary only.** Heavy payloads stay in the owning domain store. See **docs/DOMAIN_BOUNDARIES.md** for the full runContext size rule.

---

### Risk 3 — Feature flags growing unstructured

Feature flags are capability toggles. If unmanaged, they turn into nested business logic and permanent debt.

**Rules:**

- Flags are **boolean capability toggles** (e.g. “is this section/feature available?”).
- **No business logic branching inside core domain stores** based on flags. Avoid `if (flagA && !flagB || flagC)` inside stores.
- Use flags at **composition level** (e.g. section visibility in the left panel, sub-option visibility). The host (App, LeftPanel, routing) reads flags and composes the UI; stores stay flag-agnostic.
- **Sunset rule:** Every feature flag must include an **owner** and a **removal target version** (or milestone). Otherwise flags accumulate permanently. Document in code or config (e.g. `FEATURE_FLAG_META`) so flags are reviewed and removed when the feature is stable or deprecated.

---

## 3. Summary

| Area | Rule |
|------|------|
| **Extensions** | Declarative metadata only; no `render` or behavior-heavy registration. Host controls injection points. |
| **Domain boundaries** | No store imports another store. Cross-domain only via URL or runContext. Use checklist and optional lint. |
| **runContext** | Summary only (run id + small shared summary). No flags, no generic UI state, no heavy raw data (runtime/ledger own full payloads). |
| **Feature flags** | Boolean toggles; use at composition level (visibility). No nested flag logic inside domain stores. Every flag: owner + removal target version (sunset rule). |

This keeps extensibility and governance from drifting as features are added.
