# Workflow configuration (`olo-configuration`)

Canonical **generated and versioned** workflow presets live here. Gradle generators write only to the collection folders below — never to `current-active/`.

| Folder | Contents | Regenerate |
|--------|----------|------------|
| `default/` | Core presets (`agent`, `architect`, …) | `./gradlew :olo-definition:generateConfiguration` |
| `research-planner/` | Research scenario | `./gradlew :olo-definition:generateResearchPlanner` |
| `travel-planner/` or `travel-Planner/` | Travel scenario | `./gradlew :olo-definition:generateTravelPlanner` |
| `dynamic-graph-creation/` | Dynamic graph scenario | `./gradlew :olo-definition:generateDynamicGraphCreation` |
| `fixtures/` | Committed samples for unit tests (injection logs, etc.) | Manual / copied from runtime `log/` |
| `log/` | Runtime graph injection audit files (kernel) | Written at execution time |
| `current-active/` | **Manual** active set for local UI/worker testing | Copy presets yourself — see `current-active/README.md` |

## Local UI / worker testing

1. Regenerate the collection you need (table above).
2. Copy the desired JSON into `current-active/` (flat or subfolders — olo-ui scans recursively).
3. Point olo-ui (`OLO_CONFIGURATION_DIRECTORY`) and the worker `scanFolder` at `current-active/`.
4. Use **Refresh** in olo-ui or restart the worker after changes.

## Tests

Configuration tests validate on-disk JSON under each **collection folder** (`default/`, `research-planner/`, …). Kernel E2E tests load the same paths directly — not `current-active/`.
