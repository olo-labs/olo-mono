<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->
# olo-definition

Pure Java library for **serializable Open LLM Orchestrator (OLO) workflow graphs**. No execution, no model calls, no runtime state, no Spring.

## Build

From the monorepo root (Java 21):

```bash
./gradlew :olo-definition:build
```

## Usage

### Fluent builder

```java
WorkflowDefinition workflow = WorkflowBuilder.create("Stock Workflow")
    .id("stock-analysis")
    .inputNode("request")
    .modelNode("analysis", "CHAT")
    .toolNode("screener")
    .outputNode("response")
    .connect("request", "analysis")
    .connect("analysis", "screener")
    .connect("screener", "response")
    .build();

WorkflowValidator.validateOrThrow(workflow);
```

### Dynamic extension

```java
WorkflowDefinition enhanced = WorkflowBuilder.from(baseWorkflow)
    .addNode(ragNode)
    .connect("input", "rag1")
    .build();
```

### JSON / YAML

```java
JsonWorkflowSerializer json = new JsonWorkflowSerializer();
String text = json.serialize(workflow);
WorkflowDefinition loaded = json.deserialize(text);

YamlWorkflowSerializer yaml = new YamlWorkflowSerializer();
```

## Module boundaries

| Module           | Responsibility                          |
|------------------|-----------------------------------------|
| `olo-definition` | Declarative workflow POJOs + serializers  |
| `olo-kernel`     | Graph traversal + Temporal orchestration |
| `olo-core`       | Default SPI node/tool/hook implementations |
| `olo-extensions` | Additional provider integrations (planned) |

`olo-definition` must never depend on `olo-kernel` or worker code.

## Package layout

- `org.olo.definition.workflow` — `WorkflowDefinition`, `WorkflowBuilder`
- `org.olo.definition.node` — `NodeDefinition`, `NodeRouterDefinition`, `NodeType`
- `org.olo.definition.edge` — `EdgeDefinition`
- `org.olo.definition.variable` — `VariableDefinition`
- `org.olo.definition.model` — `ModelProviderDefinition`, `ModelRoutingDefinition`
- `org.olo.definition.extension` — `ExtensionDefinition`
- `org.olo.definition.serializer` — JSON/YAML serializers
- `org.olo.definition.validation` — `WorkflowValidator`

## Contributors and owners

Contributions are welcome. Start with [CONTRIBUTING.md](../../CONTRIBUTING.md), use the [contributor guide](../../docs/CONTRIBUTOR_GUIDE.md) to find the right module or scenario, route review through [OWNERS.md](../../OWNERS.md), and record meaningful module or scenario credit in [CREDITS.md](../../CREDITS.md).
