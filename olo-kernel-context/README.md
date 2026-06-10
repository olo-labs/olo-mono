# olo-kernel-context

Builds the kernel runtime context for a queue execution.

## KernelRuntimeContext

| Field | Source |
|-------|--------|
| `input` | Deserialized `WorkflowInput` from queue payload |
| `graph` | Immutable deep copy of `WorkflowDefinition` for the queue |
| `graphReady` | `GraphIsolation.prepare()` — currently `true` without graph traversal |

## API

```java
KernelRuntimeContext context = KernelContextBuilder.build(
        KernelContextBuildRequest.of(queue, inputPayloadJson, sourceGraph));

UiCallbackReporter.reportContextReady(context);
UiCallbackReporter.reportWorkflowResult(context, returnVariableName, returnVariableValue, message, usedAdminFallback);
```

Callback URL resolution: `execution.callbackUrl` (full POST URL) → `context.callbackBaseUrl` + `/api/runs/{runId}/events`.

`reportWorkflowResult` posts sequence `2` with `output.response` (workflow return message), `output.returnVariable`, `output.returnValue`, and `output.variables`.

Return variable resolution (`WorkflowReturnVariable`): `metadata.returnVariable` on the workflow → single variable with `metadata.role = "return"` → legacy `ReturnValue` name.
