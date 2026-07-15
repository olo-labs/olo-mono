<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->
# Contributor guide

This guide helps new contributors choose a useful first path through the OLO monorepo.

## Pick the right entry point

| You want to change | Start with | Ask for review from |
|--------------------|------------|---------------------|
| A workflow preset or example | `olo-definition/olo-configuration/<scenario>/README.md` | Workflow scenario owners |
| Graph structure, validation, or JSON/YAML | `olo-definition/README.md` | Definition model owners |
| Input payloads passed into Temporal | `olo-workflow-input/README.md` | Invocation payload owners |
| Worker config files or config providers | `olo-worker-configuration/README.md` | Worker configuration owners |
| Runtime context, variables, callbacks | `olo-kernel-context/README.md` | Kernel context owners |
| Traversal, return values, Temporal contract | `olo-kernel/README.md` | Kernel runtime owners |
| Worker startup or local operations | `olo-worker/README.md` | Worker process owners |
| Node/tool/hook contracts | `olo-spi/README.md` | Runtime SPI owners |
| Built-in nodes, tools, hooks, or engine | `olo-core/README.md` | Core implementation owners |
| Annotation metadata and editor catalogs | `olo-annotation/README.md` | Annotation catalog owners |

## Make docs contributor-friendly

Every module or scenario README should help visitors answer five questions quickly:

1. What does this area own?
2. What does it intentionally not own?
3. Where is the most important code or data?
4. How do I build, run, test, or activate it?
5. Who should review changes and receive credit?

## Credit owners and contributors

Use [../OWNERS.md](../OWNERS.md) to route review and [../CREDITS.md](../CREDITS.md) to record meaningful contribution credit. For scenario docs, credit the scenario owner when the preset is added or substantially improved. For module docs, credit owners when they define or maintain a public contract.

