# Research planner scenario

End-to-end scenario collection for a tool-call orchestrator with specialist child agents and mock literature lookup tools.

| Workflow | Role | Queue |
|----------|------|-------|
| `research-orchestrator` | Tool-call planner + delegation | `oloQueue2` |
| `literature-agent` | Paper discovery child agent | `oloQueue2` |
| `synthesis-agent` | Brief synthesis child agent | `oloQueue2` |

## Regenerate JSON

From `olo-definition/olo-definition`:

```bash
./gradlew :olo-definition:generateResearchPlanner
```

Writes **`olo-configuration/research-planner/`** only. For local olo-ui/worker testing, copy into **`current-active/`** (see `current-active/README.md`).

## Load in worker

Point `scanFolder` at this folder, or copy into `current-active/` and scan that:

```yaml
workflowDefinitions:
  scanFolder: "../../olo-definition/olo-configuration/research-planner"
```

## Mock literature topics

| Sample query | Fixture |
|--------------|---------|
| renewable energy storage | `renewable-energy-storage.json` |
| AI safety alignment | `ai-safety-alignment.json` |
| what is olo / Open LLM Orchestrator | `olo-open-llm-orchestrator.json` |

## Tests

- `ResearchPlannerConfigurationTest` — on-disk JSON matches programmatic definitions
- `ResearchPlannerEndToEndTest` (olo-kernel) — registry load, tool execution, tool-call merge
