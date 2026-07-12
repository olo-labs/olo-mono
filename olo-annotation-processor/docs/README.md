<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->
# Documentation

| Document | Description |
|----------|-------------|
| [OLO_1_0.md](../../olo-annotation/docs/OLO_1_0.md) | **OLO 1.0 architecture freeze** — stop redesigning metadata; next priorities |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Processor design, compile-time flow, generated catalog layout, Gradle wiring, runtime loading |
| [CATALOG_SCHEMA.md](CATALOG_SCHEMA.md) | JSON field reference — authoritative per-type catalogs plus convenience `catalog.json` |
| [VALIDATION_RULES.md](VALIDATION_RULES.md) | Planned compile-time validation codes (OLO-AP-*), examples, migration guidance |

## Related modules

| Module | Role |
|--------|------|
| [olo-annotation](../../olo-annotation/) | `@OloNode`, `@OloTool`, `@OloHook` — [docs](../../olo-annotation/docs/README.md) |
| [olo-spi](../../olo-spi/) | Runtime SPI contracts (`Node`, `Tool`, `Hook`) |
| [olo-core](../../olo-core/) | Reference implementations that use this processor |
| [olo-definition](../../olo-definition/) | Workflow graph POJOs that UIs edit using catalog metadata |
