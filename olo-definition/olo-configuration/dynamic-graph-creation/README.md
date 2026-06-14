# Dynamic graph creation preset

Experimental workflow preset that asks the model to **return JSON-only structured data** describing an OLO workflow graph, then **expands that JSON inline** in the same workflow run (no child workflow dispatch).

| Field | Value |
|-------|-------|
| Workflow id | `dynamic-graph-creation` |
| Queue | `dynamic-graph-creation` |
| Planner node | `graph-planner` (`AGENT`, `INLINE`) |
| JSON variable | `generatedGraphJson` |
| Retry variable | `generatedGraphJsonRetryCount` |

## Runtime flow

```text
START → graph-planner (inline LLM) → validate generatedGraphJson
                                   ├─ invalid JSON → re-execute graph-planner (up to 3 times)
                                   └─ valid JSON   → merge subgraph nodes/edges → continue → END
```

The kernel merges the model-produced subgraph into the active graph and continues traversal from the first merged step node. The original `graph-planner → end` shortcut edge is replaced with `graph-planner → subgraph → end`.

## Prompt contract

The default planner prompt instructs the model to:

- Return **only** a single JSON object (no markdown or commentary)
- Include `id`, `label`, `nodes`, and `edges`
- Always include `START` and `END` nodes with connecting edges

## Regenerate JSON

From `olo-definition/olo-definition`:

```bash
./gradlew :olo-definition:generateDynamicGraphCreation
```

## Load in worker

```yaml
workflowDefinitions:
  scanFolder: "../../olo-definition/olo-configuration/dynamic-graph-creation"
```
