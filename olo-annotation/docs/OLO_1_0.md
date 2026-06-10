# OLO 1.0 — catalog architecture freeze

The extension metadata system is **frozen for OLO 1.0**. The catalog architecture is strong enough to support community plugins, enterprise extensions, marketplace indexing, and long-term ecosystem growth. **Stop redesigning the metadata system** — invest effort elsewhere.

Annotation attribute details: [V1.md](V1.md)  
Processor and JSON layout: [ARCHITECTURE.md](../../olo-annotation-processor/docs/ARCHITECTURE.md)  
JSON field reference: [CATALOG_SCHEMA.md](../../olo-annotation-processor/docs/CATALOG_SCHEMA.md)

---

## Frozen architecture

```
olo-annotation
    Annotation API (@OloNode, @OloTool, @OloHook, …)
    ExtensionCatalog          (typed merge result)
    ExtensionCatalogLoader    (classpath merge)

olo-annotation-processor
    Catalog generation        (compile time only)

Catalog resources (per plugin JAR)
    META-INF/olo/catalog/nodes.json     authoritative
    META-INF/olo/catalog/tools.json     authoritative
    META-INF/olo/catalog/hooks.json     authoritative
    META-INF/olo/catalog/catalog.json   convenience bundle (not merged)

Runtime (olo-spi, olo-runtime, worker)
    Never reads @Olo* annotations
    Never branches on catalog metadata (featured, examples, help, …)
    Resolves implementations via SPI + workflow definition

UI (OLO Studio, marketplace, CLIs)
    Fully metadata-driven from ExtensionCatalog / per-type JSON
```

| Layer | Frozen responsibility |
|-------|----------------------|
| **`olo-annotation`** | Annotation API; `ExtensionCatalog`; `ExtensionCatalogLoader` |
| **`olo-annotation-processor`** | Generate catalog JSON from annotations |
| **Catalog resources** | `nodes.json`, `tools.json`, `hooks.json` (authoritative); `catalog.json` (convenience) |
| **Runtime** | SPI + workflow definition only — no annotation reflection, no catalog UX fields |
| **UI** | Palettes, property forms, ports, onboarding — all driven by catalog metadata |

---

## Rules that do not change in 1.0

| Rule | Detail |
|------|--------|
| **Authoritative catalogs** | `ExtensionCatalogLoader` merges only `nodes.json`, `tools.json`, `hooks.json` — **never** `catalog.json` (derived per-module snapshot only; a second merge path is forbidden) |
| **Global extension ids** | Must be globally unique; duplicate-id merge is diagnostic only |
| **Metadata is editor-only** | `featured`, `examples`, `help`, `placeholder`, `group` never affect execution |
| **Schema evolution** | Consumers ignore unknown JSON fields; bump `schemaVersion` for breaking changes |
| **SPI ↔ catalog ids** | Must match (`@OloNode.type` ↔ `@NodeType`, etc.) — enforced at compile time ([VALIDATION_RULES.md](../../olo-annotation-processor/docs/VALIDATION_RULES.md)) |
| **Boolean flags** | Option A — emit `true` only; omitted = `false` (compact marketplace catalogs) |
| **Descriptor `kind`** | Keep on every entry (`NODE` / `TOOL` / `HOOK`) for merged indexes despite `catalogType` redundancy |

---

## What we are not redesigning

Do not reopen for 1.0:

- Annotation attribute surface (see [V1.md](V1.md) for frozen vs reserved)
- Catalog file layout or authoritative vs convenience split
- Typed `ExtensionCatalog` consumer API
- CLASS retention model (compile-time → JSON, not runtime reflection)
- Per-type catalog files for marketplace / CLI / Studio

Post-1.0 additions require **`schemaVersion` bump** and coordinated processor + UI updates.

**Descriptor ownership:** every node/tool/hook carries `version`, `provider`, and `stability` for Studio and marketplace compatibility.

**Reserved (not in v1):** `moduleVersion` (file header); hook `supportedTargets` (attachment levels). See [CATALOG_SCHEMA.md — Reserved](../olo-annotation-processor/docs/CATALOG_SCHEMA.md#reserved-post-10-not-emitted-today).

---

## Where effort goes next

| Priority | Focus |
|----------|--------|
| **OLO Studio UX** | Palette, property forms, port graph, onboarding — consume frozen catalog as-is |
| **Plugin marketplace model** | Publish, validate, and index plugin JARs (`tools.json`, `catalog.json`, …) |
| **Compile-time validation** | OLO-AP-001 / 002 / 003 — SPI ↔ annotation consistency ([VALIDATION_RULES.md](../../olo-annotation-processor/docs/VALIDATION_RULES.md)) |
| **AI-assisted workflow generation** | Planner and copilot use catalog capabilities + workflow definition |

The metadata platform is done enough. Build on top of it.
