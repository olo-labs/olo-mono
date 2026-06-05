# Workflow input samples

Example `WorkflowInput` JSON payloads under this directory. These files are **declarative only**—no secrets, no live cache or file contents.

## Regenerate

Samples are generated from `SampleWorkflowInputDefinitions` before tests run:

```bat
gradlew.bat generateSamples
```

Or as part of a full build:

```bat
gradlew.bat build
```

## Samples

| Folder | Description |
|--------|-------------|
| [minimal-local](minimal-local/) | Smallest valid payload: one LOCAL string input |
| [mixed-storage](mixed-storage/) | LOCAL string + CACHE (Redis) + FILE reference |
| [producer-offload](producer-offload/) | `WorkflowInputProducer`: small inline + large cache-offloaded string |
| [cache-in-memory](cache-in-memory/) | CACHE with `IN_MEMORY` provider |
| [typed-inputs](typed-inputs/) | STRING, NUMBER, BOOLEAN, JSON, and OBJECT input types |
| [agent-execution](agent-execution/) | `AGENT_EXECUTION` with `execution.callbackUrl` and `timeoutSeconds` |
| [workflow-run](workflow-run/) | `WORKFLOW_RUN` with `configVersion` and `execution.timeoutSeconds` |
| [storage-remote](storage-remote/) | S3 and DB storage modes (schema-level references) |
| [rag-metadata](rag-metadata/) | RAG tag and timestamp in metadata |

## Load in Java

```java
import org.olo.input.model.WorkflowInput;

Path path = Path.of("samples/mixed-storage/workflow-input.json");
WorkflowInput input = WorkflowInput.fromJson(Files.readString(path));
```

Optional scratch output from E2E tests: `samples/generated/` (gitignored).

`SampleWorkflowInputCopyTest` writes `workflow-input-copy.json` beside each sample (via `WorkflowInput.copy()`) and asserts it is byte-identical to the original. Copy sidecars are gitignored.
