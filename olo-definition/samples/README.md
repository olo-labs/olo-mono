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
| [stock-analysis](stock-analysis/) | Stock workflow: INPUT → MODEL → TOOL → OUTPUT |
| [rag-chat](rag-chat/) | RAG pipeline with vector search, model providers, and extensions |
| [analysis-with-rag-extension](analysis-with-rag-extension/) | Base analysis workflow extended with a retriever node |
| [condition-branch](condition-branch/) | CONDITION node with port-aware edges (`true` / `false`) |

## Load in Java

```java
import io.olo.definition.serializer.JsonWorkflowSerializer;
import io.olo.definition.validation.WorkflowValidator;
import io.olo.definition.workflow.WorkflowDefinition;

Path path = Path.of("samples/stock-analysis/workflow.json");
WorkflowDefinition workflow =
    new JsonWorkflowSerializer().deserialize(Files.readString(path));

WorkflowValidator.validateOrThrow(workflow);
```

Optional scratch output from E2E tests: `samples/generated/` (gitignored).
