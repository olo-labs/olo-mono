<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->
# OLO monorepo documentation

Architecture and module reference for **olo-mono** — the Java libraries and worker process that implement the **Open LLM Orchestrator (OLO)**.

## Documents

| Document | Contents |
|----------|----------|
| [ARCHITECTURE.md](ARCHITECTURE.md) | System layers, end-to-end request flow, deployment topology, configuration model |
| [MODULES.md](MODULES.md) | Per-module responsibilities, Maven coordinates, dependency graph, build notes |

## Module-level docs

Deeper design for individual Gradle projects:

| Module | Docs |
|--------|------|
| olo-definition | [olo-definition/doc/ARCHITECTURE.md](../olo-definition/doc/ARCHITECTURE.md) |
| olo-workflow-input | [olo-workflow-input/docs/ARCHITECTURE.md](../olo-workflow-input/docs/ARCHITECTURE.md) |
| olo-worker-configuration | [olo-worker-configuration/docs/ARCHITECTURE.md](../olo-worker-configuration/docs/ARCHITECTURE.md) |
| olo-worker | [olo-worker/README.md](../olo-worker/README.md) |
| olo-spi | [olo-spi/README.md](../olo-spi/README.md), [docs/ARCHITECTURE.md](../olo-spi/docs/ARCHITECTURE.md) |
| olo-core | [olo-core/README.md](../olo-core/README.md), [docs/ARCHITECTURE.md](../olo-core/docs/ARCHITECTURE.md) |
| olo-kernel | [olo-kernel/README.md](../olo-kernel/README.md), [docs/traversal.md](../olo-kernel/docs/traversal.md) |
| olo-annotation | [olo-annotation/README.md](../olo-annotation/README.md) |
| olo-annotation-processor | [olo-annotation-processor/README.md](../olo-annotation-processor/README.md) |
| olo-kernel-context | [olo-kernel-context/README.md](../olo-kernel-context/README.md) |
| olo-bootstrap | [olo-bootstrap/README.md](../olo-bootstrap/README.md) |

## Quick orientation

```
olo-definition/olo-configuration/   Scenario presets + current-active/ (activate via olo-ui Administration → Scenarios)
olo-definition/        Graph POJOs, serializers, validation
olo-workflow-input/    Per-run invocation payload (WorkflowInput)
olo-worker-configuration/  Worker deployment settings (port, Temporal, scanFolder)
olo-bootstrap/         Load workflow JSON into an in-memory registry
olo-kernel-context/    Build runtime context + UI callbacks
olo-spi/               Runtime SPI contracts (Node, Tool, Hook)
olo-annotation/        Extension metadata annotations
olo-annotation-processor/  Compile-time catalog JSON generator
olo-core/              Default SPI implementations + ExecutionEngine
olo-kernel/            Graph traversal + Temporal entry point (OloKernelWorkflow)
olo-worker/            Runnable Temporal worker application
```

Start with [ARCHITECTURE.md](ARCHITECTURE.md) for the big picture, then [MODULES.md](MODULES.md) when implementing or extending a specific layer.

## Contributors and owners

Contributions are welcome. Start with [CONTRIBUTING.md](../CONTRIBUTING.md), use the [contributor guide](../docs/CONTRIBUTOR_GUIDE.md) to find the right module or scenario, route review through [OWNERS.md](../OWNERS.md), and record meaningful module or scenario credit in [CREDITS.md](../CREDITS.md).
