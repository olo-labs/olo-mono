<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->
# olo-annotation-processor

Annotation processor that generates extension catalog JSON for plug-and-play workflow editing.

**Documentation:** [docs/](docs/README.md) — [architecture](docs/ARCHITECTURE.md), [catalog schema](docs/CATALOG_SCHEMA.md)

## Generated resources

| File | Contents |
|------|----------|
| `META-INF/olo/catalog/nodes.json` | All `@OloNode` types in the module |
| `META-INF/olo/catalog/tools.json` | All `@OloTool` types |
| `META-INF/olo/catalog/hooks.json` | All `@OloHook` types |
| `META-INF/olo/catalog/catalog.json` | Per-module convenience bundle (authoritative sources: the three files above) |

## Compiler option

`-Aolo.catalog.module=olo-core-nodes` — module id embedded in generated JSON as `moduleId`.

## Loading catalogs (runtime)

```java
import org.olo.annotation.catalog.ExtensionCatalog;
import org.olo.annotation.catalog.ExtensionCatalogLoader;

ExtensionCatalog catalog = ExtensionCatalogLoader.loadMerged(classLoader);
```

Or via `org.olo.core.catalog.CoreExtensionCatalog.loadMerged()` when using `olo-core`.
