<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->
# Runtime Roadmap

**Target architecture** for workflow lifecycle (`WorkflowStatus`), variable scope enforcement, suspension/resume, and multi-agent output merging. Planned work — not the current kernel contract.

For **what traversal implements today**, see [traversal.md](./traversal.md).

For the **normative lifecycle and scope model**, see [runtime-model.md](./runtime-model.md).

Related:

- [orchestration-roadmap.md](./orchestration-roadmap.md) — child workflow coordinator, agent executors
- [parallelism-roadmap.md](./parallelism-roadmap.md) — barriers and join `WAITING`

---

## Implementation gaps (today)

| Target | Current code | Gap |
|--------|--------------|-----|
| `WORKFLOW` map | `WorkflowRuntimeVariables` — flat map from graph definitions | No scope enforcement on `set()` |
| `SESSION` fields | `WorkflowInput` + `Context` | Not unified with variable map |
| `NODE` scratch | `DefaultExecutionContext` via SPI | `VariableScopeBridge` merges into WORKFLOW wholesale |
| `AGENT` isolation | *Not implemented* | Child runs would share parent map today |
| `GLOBAL` | Bootstrap / tenant config | Not visible in kernel variable map |
| Catalog `scope` on `VariableDefinition` | Validated at load | **Ignored at runtime** in kernel |
| `WorkflowStatus` enum | `ChatRunStore` string statuses | No shared enum in kernel/SPI |
| `CANCELLED` | Reserved | Not implemented |
| `WAITING` traversal | `TraversalResult.waiting()` on WAITING; sync `execute()` cannot complete | Temporal path resumes via `OloKernelWorkflowImpl` + `HumanInputResumeSupport`; checkpoint gaps remain |

**Planned home for status:** `org.olo.kernel.runtime.WorkflowStatus` (or shared SPI) consumed by `olo` API, Temporal workflow, and UI event derivation.

---

## Planned deliverables

| # | Deliverable | Unlocks |
|---|-------------|---------|
| 1 | **`ScopedVariableStore`** | Layered maps with write guards per `VariableScope` |
| 2 | **`WorkflowStatus` enum** | Replace ad-hoc `"running"` / `"completed"` in `ChatRunStore` |
| 3 | **`WAITING` traversal** | Pause graph walk without failing; persist checkpoint (node id, variables snapshot) |
| 4 | **AGENT scope fork/merge** | `ChildWorkflowCoordinator` — see [orchestration-roadmap.md](./orchestration-roadmap.md) |
| 5 | **Enforce `READONLY_EXTERNAL`** | Reject or no-op writes to `message` after START bind |

### When each state becomes essential

| Feature | States involved |
|---------|-----------------|
| Human approval | `RUNNING` → `WAITING` → `RUNNING` |
| Child workflows | Parent `WAITING` while child `RUNNING`; merge on child `COMPLETED` |
| Long-running activities | `RUNNING` across Temporal task boundaries |
| Chat session replay | `COMPLETED` runs remain in session history; new message → new `CREATED` |
| Operator cancel | `CANCELLED` from any non-terminal state |

### Variable scopes — target vs presets

```
GLOBAL        tenant secrets, defaults
SESSION       chat session, correlation, callback URL
WORKFLOW      message, ReturnValue, node outputs  ← kernel today (flat map)
AGENT         child workflow state                ← not wired
NODE          per-step SPI scratch                ← partial via VariableScopeBridge
```

Resolution algorithm (target) is documented in [runtime-model.md](./runtime-model.md#resolution-algorithm-target).

---

## Suggested phases

| Phase | Deliverable |
|-------|-------------|
| **R0** (now) | `ExecutionOutputs`, `WorkflowReturnResolver`, flat `WORKFLOW` map |
| **R1** | `WorkflowStatus` enum + UI/API alignment |
| **R2** | `ScopedVariableStore` + `READONLY_EXTERNAL` enforcement |
| **R3** | `WAITING` checkpoint + resume (with Human / child workflows) |
| **R4** | `AGENT` scope isolation + merge via coordinator |

---

## Summary

| Concept | Today | Target |
|---------|-------|--------|
| Workflow status | String statuses in UI store | `WorkflowStatus` enum end-to-end |
| Variable scope | Catalog validated; runtime flat map | Enforced scope stack |
| Suspension | Sync kernel fails on `WAITING` | Checkpoint + resume |
| Child isolation | Shared parent map | `AGENT` scope fork/merge |
