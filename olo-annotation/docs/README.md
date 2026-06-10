# Documentation

| Document | Description |
|----------|-------------|
| [OLO_1_0.md](OLO_1_0.md) | **OLO 1.0 architecture freeze** — modules, catalogs, runtime/UI boundaries, next priorities |
| [V1.md](V1.md) | **Frozen annotation API** — attributes that ship, what is reserved |
| [ANNOTATIONS.md](ANNOTATIONS.md) | Annotation API contracts only |
| [EDITOR_CONVENTIONS.md](EDITOR_CONVENTIONS.md) | UI semantics: `featured`, `examples`, groups, badges, runtime boundaries |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Module role, `ExtensionCatalog`, lifecycle, Gradle usage |

Runtime API: `ExtensionCatalog`, `NodeDescriptor`, `ToolDescriptor`, `HookDescriptor` in `org.olo.annotation.catalog`.

## Related modules

| Module | Role |
|--------|------|
| [olo-annotation-processor](../../olo-annotation-processor/) | Turns annotations into `META-INF/olo/catalog/*.json` at compile time |
| [olo-spi](../../olo-spi/) | Runtime SPI (`Node`, `Tool`, `Hook`) and discovery annotations |
| [olo-core](../../olo-core/) | Reference implementations annotated with this module |
| [olo-definition](../../olo-definition/) | Workflow graph model that UIs build using catalog metadata |

Generated JSON schema: [olo-annotation-processor/docs/CATALOG_SCHEMA.md](../../olo-annotation-processor/docs/CATALOG_SCHEMA.md)
