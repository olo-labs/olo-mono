<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->
# Workflow presets baked into the olo-ui Docker image.

## Contributor and owner notes

Runtime configuration contributors should keep bundled presets, sample logs, and scenario folders useful for first-run demos. Owners should verify that Docker image defaults match this folder and credit configuration stewardship in [../../../docs/MODULE_OWNERS.md](../../../docs/MODULE_OWNERS.md).

This folder is copied to `/app/olo-configuration` at container runtime.

- **CI:** refreshed from `olo-mono/olo-configuration/default` plus scenario folders (`research-planner/`, etc.). **`current-active/` is not used** — it is for manual local copies only.
- **Fallback:** these committed JSON files are used when olo-mono has no `olo-configuration` on GitHub yet.

To update locally from olo-mono:

```bash
./scripts/bundle-docker-configuration.sh
```

Subfolders are supported; olo-be scans recursively (e.g. `agents/agent.json`).

### Graph logs (`log/`)

Sample runtime graph logs are bundled under `log/` for the **Workflows → Log** view in olo-ui. These are written by the Olo kernel when dynamic subgraphs are injected at runtime (`DynamicSubgraphInjectionLogger` in olo-kernel). The Docker image sets `OLO_LOG_DIRECTORY=/app/olo-configuration/log`.

Log files are **read-only** in the UI; they contain a `mergedGraph` field with the post-injection workflow graph.
