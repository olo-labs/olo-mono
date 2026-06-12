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
    → OloKernelActivitiesImpl
      → KernelEntryPoint
        → KernelContextBuilder (olo-kernel-context)
          → deserialize WorkflowInput
          → WorkflowDefinition.copy() for queue graph
          → GraphIsolation.prepare()
          → UiCallbackReporter → execution.callbackUrl / context.callbackBaseUrl
        → GraphTraverser (START → AGENT[prompt+model] → END)
        → workflow defaultPromptId + modelRouting/modelProviders → local Ollama /api/chat
        → ReturnValue → queue return + UI callback
        → WorkflowReturnResolver: metadata.returnVariable (or role=return), or userQuery / fallback when null
```

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
