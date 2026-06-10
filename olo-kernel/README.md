# olo-kernel

Temporal queue execution entry point. Each task queue registers `OloKernelWorkflow` (`workflowType=olo`).

## Flow

```
Temporal queue task (WorkflowInput JSON object)
  → OloKernelWorkflowImpl
    → OloKernelActivitiesImpl
      → KernelEntryPoint
        → KernelContextBuilder (olo-kernel-context)
          → deserialize WorkflowInput
          → WorkflowDefinition.copy() for queue graph
          → GraphIsolation.prepare() [stub: true, no traversal]
          → UiCallbackReporter → execution.callbackUrl / context.callbackBaseUrl
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
