<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->
# Workflow samples

Example `WorkflowDefinition` artifacts in JSON and YAML under this directory. These files are **declarative only**—no secrets, no runtime configuration.

## Regenerate

Samples are generated from `SampleWorkflowDefinitions` before tests run:

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
| [minimal-echo](minimal-echo/) | Smallest valid graph: INPUT → OUTPUT |
| [stock-analysis](stock-analysis/) | Stock workflow with typed ports and `onFailure` retry + fallback model |
| [rag-chat](rag-chat/) | RAG pipeline with vector search, model providers, and extensions |
| [analysis-with-rag-extension](analysis-with-rag-extension/) | Base analysis workflow extended with a retriever node |
| [condition-branch](condition-branch/) | CONDITION routing to `AGENT` nodes (`true` / `false` ports) |
| [human-approval-trade](human-approval-trade/) | AI recommendation → `HUMAN` approval → trade execution |
| [multi-agent-orchestration](multi-agent-orchestration/) | Multi-agent handoff via mandatory `workflow` on each `AGENT` |
| [parallel-agent-fan-out](parallel-agent-fan-out/) | `PARALLEL` with `join: ALL` and child agent workflows |
| [research-agent](research-agent/) | Planner-readable `capability` contract (tags, examples, cost/latency) |
| [technical-analysis-agent](technical-analysis-agent/) | Agent with `workflowRef` + `runtimeBinding.implementationId` |

## Load in Java

```java
import org.olo.definition.serializer.JsonWorkflowSerializer;
import org.olo.definition.validation.WorkflowValidator;
import org.olo.definition.workflow.WorkflowDefinition;

Path path = Path.of("samples/stock-analysis/workflow.json");
WorkflowDefinition workflow =
    new JsonWorkflowSerializer().deserialize(Files.readString(path));

WorkflowValidator.validateOrThrow(workflow);
```

Optional scratch output from E2E tests: `samples/generated/` (gitignored).

`SampleWorkflowCopyTest` writes `workflow-copy.json` / `workflow-copy.yaml` beside each sample (via `WorkflowDefinition.copy()`) and asserts they are byte-identical to the originals. Copy sidecars are gitignored.
