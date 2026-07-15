<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->
# Demo observability data for olo-core tools

Sample folders used by the built-in observability tools. Each tool maps to its own folder via the `dataFolder` configuration property.

| Tool | Default folder | Demo file |
|------|----------------|-----------|
| Log Reader | `demo-data/logs` | `application.log` |
| CPU Usage | `demo-data/cpu` | `metrics.csv` |
| Memory Usage | `demo-data/memory` | `metrics.csv` |
| Numeric Metric | `demo-data/latency` | `metrics.csv` |
| Recently Changed Code | `demo-data/recent-changes` | `*.patch` files |

## Failure incident window

All demo data describes the same synthetic outage on **2026-06-14** around **14:30 UTC**:

- Payment gateway becomes unreachable
- CPU and memory spike
- Request latency jumps from ~50ms to ~3000ms
- Error logs include `ConnectionTimeout`, circuit breaker OPEN, and retry exhaustion

### Example queries

Observability tools resolve `demo-data/...` relative to the current directory first, then auto-discover `olo-core/tools/demo-data` by walking up from the process working directory (so the worker can run from `olo-worker`). Set `OLO_DEMO_DATA_ROOT` to `olo-core/tools` to pin a custom location.

| Tool | startTime | endTime |
|------|-----------|---------|
| Log Reader | `2026-06-14T14:30:00Z` | `2026-06-14T14:31:00Z` |
| CPU Usage | `2026-06-14T14:29:00Z` | `2026-06-14T14:32:00Z` |
| Memory Usage | `2026-06-14T14:29:00Z` | `2026-06-14T14:32:00Z` |
| Numeric Metric | `2026-06-14T14:29:00Z` | `2026-06-14T14:32:00Z` |

### Metric CSV format

```csv
timestamp,value
2026-06-14T14:30:00Z,92.8
```

- **CPU** — `value` is CPU utilization percentage
- **Memory** — `value` is heap usage in MB
- **Latency** — `value` is upstream latency in milliseconds

## Recently changed code (pull request stubs)

The **Recently Changed Code** tool reads `.patch`, `.diff`, `.java`, `.json`, and `.txt` files from its folder and returns raw file content (no GitHub API call yet).

Demo PRs tied to the same payment-service incident:

| File | PR | Summary |
|------|----|---------|
| `839-retry-pool-resize.patch` | #839 | Retry thread pool enlarged — may explain queue depth warnings |
| `842-gateway-timeout.patch` | #842 | Gateway connect/read timeouts reduced to 1s/3s — likely outage trigger |
| `847-circuit-breaker-hotfix.patch` | #847 | Post-incident circuit breaker hotfix |

Optional arguments: `limit` (default 5), `pullRequestNumber` (e.g. `842` to filter by filename).

## Multi-agent scenarios

Observability scenarios (`log-rca-analysis`, `performance-triage`, `security-incident-response`, `release-readiness-review`, `api-integration-triage`, `capacity-planning`) use this data. Research and literature scenarios use `research-literature` mock fixtures. Activate a scenario in **olo-ui → Administration → Scenarios**, then run the orchestrator on `oloQueue2`. See [`olo-configuration/README.md`](../../../olo-definition/olo-configuration/README.md).

## Contributors and owners

Contributions are welcome. Start with [CONTRIBUTING.md](../../../CONTRIBUTING.md), use the [contributor guide](../../../docs/CONTRIBUTOR_GUIDE.md) to find the right module or scenario, route review through [OWNERS.md](../../../OWNERS.md), and record meaningful module or scenario credit in [CREDITS.md](../../../CREDITS.md).
