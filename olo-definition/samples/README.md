# Workflow samples

Example `WorkflowDefinition` artifacts in JSON and YAML. These files are **declarative only**—no secrets, no runtime configuration.

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
import io.olo.definition.serializer.YamlWorkflowSerializer;
import io.olo.definition.validation.WorkflowValidator;
import io.olo.definition.workflow.WorkflowDefinition;

Path path = Path.of("samples/stock-analysis/workflow.json");
WorkflowDefinition workflow =
    new JsonWorkflowSerializer().deserialize(Files.readString(path));

WorkflowValidator.validateOrThrow(workflow);
```

YAML:

```java
WorkflowDefinition workflow = new YamlWorkflowSerializer()
    .deserialize(Files.readString(Path.of("samples/stock-analysis/workflow.yaml")));
```

## Tests

**End-to-end (build in code → serialize → deserialize → write files):**

```bat
gradlew.bat :olo-definition:test --tests "io.olo.definition.samples.SampleWorkflowSerializationE2ETest"
```

This also writes serialized workflows to `samples/generated/` (gitignored). Each folder contains `workflow.json` and `workflow.yaml`.

**On-disk sample files (JSON/YAML under `samples/`):**

```bat
gradlew.bat :olo-definition:test --tests "io.olo.definition.samples.SampleWorkflowsTest"
```
