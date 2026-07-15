<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->
# Contributing to OLO

OLO is built as a modular orchestration stack. The best contributions usually improve one clear layer: workflow definitions, invocation payloads, worker configuration, bootstrap, kernel runtime, SPI contracts, core implementations, or documentation.

## Start here

1. Read [README.md](README.md) for the project map.
2. Use [docs/MODULES.md](docs/MODULES.md) to find the module that owns the behavior you want to change.
3. Check [OWNERS.md](OWNERS.md) for the owner area and expected review path.
4. Open or update an issue with the module, problem, expected behavior, and test evidence.

## Contribution lanes

| Lane | Good first contributions | Best starting docs |
|------|--------------------------|--------------------|
| Workflow scenarios | New presets, clearer scenario READMEs, sample inputs, activation notes | [olo-definition/olo-configuration/README.md](olo-definition/olo-configuration/README.md) |
| Definition model | Graph POJOs, serializers, validation, samples | [olo-definition/README.md](olo-definition/README.md) |
| Runtime and traversal | Kernel entry point, traversal, return values, human input resume | [olo-kernel/README.md](olo-kernel/README.md) |
| Worker operations | Configuration, bootstrap, Temporal workers, local debug paths | [olo-worker/README.md](olo-worker/README.md) |
| SPI and extensions | Node, tool, hook contracts and implementations | [olo-spi/README.md](olo-spi/README.md), [olo-core/README.md](olo-core/README.md) |
| Annotation catalogs | `@OloNode`, `@OloTool`, `@OloHook`, generated UI metadata | [olo-annotation/README.md](olo-annotation/README.md), [olo-annotation-processor/README.md](olo-annotation-processor/README.md) |
| Documentation | Architecture notes, module ownership, examples, troubleshooting | [docs/README.md](docs/README.md) |

## Review expectations

- Keep changes scoped to the module or scenario named in the issue.
- Preserve module boundaries. `olo-definition` should not learn about workers, Temporal, or runtime execution.
- Add or update tests for behavior changes. For docs-only changes, include a quick self-review of links and examples.
- Update the nearest README when behavior, setup, ownership, or public contracts change.
- Credit the area owner and significant contributors in [CREDITS.md](CREDITS.md) when a contribution establishes or materially expands a module, scenario, or public capability.

## Local build notes

Each Gradle module is standalone. Use the module wrapper from that directory:

```bash
cd olo-worker
./gradlew run
```

On Windows, use `gradlew.bat`.

When not using composite builds, publish dependencies in the order listed in [docs/MODULES.md](docs/MODULES.md).

## Pull request checklist

- [ ] The PR title names the touched module or scenario.
- [ ] The description explains the user-visible behavior or documentation improvement.
- [ ] Tests, samples, or docs were updated for the changed behavior.
- [ ] The relevant owner area from [OWNERS.md](OWNERS.md) is requested for review.
- [ ] Contributor or owner credit updates were added when appropriate.

