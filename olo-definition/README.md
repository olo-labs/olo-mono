<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->
# olo-definition

Gradle project for **OLO workflow graph definitions** — serializable POJOs, builders, JSON/YAML serializers, and validation. Part of the [olo-mono](../README.md) monorepo.

## Documentation

| Document | Scope |
|----------|--------|
| [doc/ARCHITECTURE.md](doc/ARCHITECTURE.md) | Graph model, serialization, validation |
| [olo-definition/README.md](olo-definition/README.md) | Module API and package layout |
| [olo-configuration/README.md](olo-configuration/README.md) | Scenario presets and `current-active/` runtime folder |

Example workflows: [samples/](samples/). Preset definitions: [olo-configuration/](olo-configuration/).

## Related monorepo modules

| Module | Role |
|--------|------|
| [olo-workflow-input](../olo-workflow-input/) | Per-run `WorkflowInput` invocation payload |
| [olo-bootstrap](../olo-bootstrap/) | Loads `olo-configuration` folders into registry |
| [olo-kernel](../olo-kernel/) | Graph traversal and Temporal orchestration |
| [olo-core](../olo-core/) | Default node/tool/hook SPI implementations |

## Requirements

- Java 21 (toolchain configured in `build.gradle`)
- Gradle 8.12+ (wrapper included)

## Build

From this directory:

```bash
./gradlew build
```

From the monorepo parent, use the module wrapper here or publish via Maven local (see [docs/MODULES.md](../docs/MODULES.md)).

Windows:

```bat
gradlew.bat build
```

The build generates workflow samples under `samples/` (`./gradlew generateSamples`).

## License

Apache License 2.0 — see [LICENSE](LICENSE).
