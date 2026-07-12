<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->
# olo-kernel

Temporal queue execution entry point. Each task queue registers `OloKernelWorkflow` (`workflowType=olo`).

## Docs

**Implementation contract**

- [docs/traversal.md](docs/traversal.md) — current graph traversal, strategies, handlers

**Target architecture (roadmaps)**

- [docs/orchestration-roadmap.md](docs/orchestration-roadmap.md) — agent backends, child workflows
- [docs/parallelism-roadmap.md](docs/parallelism-roadmap.md) — parallel fan-out/join, barriers
- [docs/runtime-roadmap.md](docs/runtime-roadmap.md) — workflow status, scope enforcement, resume

**Normative model**

- [docs/runtime-model.md](docs/runtime-model.md) — workflow lifecycle and variable scope model

## Flow

```
Temporal queue task (WorkflowInput JSON object)
    → OloKernelWorkflowImpl
    → buildContextAndNotifyUi activity (context + UI "started" callback only)
    → loop until traversal completes:
        → executeTraversalStep activity per node ({@code id:label}, default)
        → INLINE nodes without ACTIVITY kind run in the workflow loop
    → reportWorkflowResult activity (return value + UI callback)
```

`execution.executionModel` / `execution.executionKind` on each node control scheduling:

| Model / kind | Temporal behavior |
|--------------|-------------------|
| unset or `ACTIVITY` | Dedicated `executeTraversalStep` activity per node |
| `INLINE` + `executionKind: ACTIVITY` | Dedicated activity (e.g. dynamic graph planner LLM step) |
| `INLINE` without `ACTIVITY` kind | Executed synchronously inside the workflow loop |

Legacy synchronous entry (tests, direct API):

```java
String message = KernelEntryPoint.execute(queue, workflowInput, workflowRegistry);
```

This runs the same step engine in-process without separate Temporal activities.

## API

```java
String message = KernelEntryPoint.execute(queue, workflowInput, workflowRegistry);
```

The Temporal workflow method accepts a `WorkflowInput` **object** (JSON object on the wire), not a JSON string.

## Temporal registration

```java
KernelWorkflowRegistrar.register(worker, workflowDefinitionRegistry);
```

Called by `olo-worker` for every configured queue.
