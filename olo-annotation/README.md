# olo-annotation

Compile-time annotations for OLO extension metadata used by workflow editor UIs.

**Documentation:** [docs/](docs/README.md) — [v1 freeze](docs/V1.md), [API contracts](docs/ANNOTATIONS.md), [editor conventions](docs/EDITOR_CONVENTIONS.md), [architecture](docs/ARCHITECTURE.md)

| Annotation | Purpose |
|------------|---------|
| `@OloNode` | Node implementation catalog entry |
| `@OloTool` | Tool implementation catalog entry |
| `@OloHook` | Hook implementation catalog entry |
| `@OloPort` | Input/output port schema |
| `@OloProperty` | Configuration / argument field schema |
| `OloPropertyType` | Enum for property editor control (`STRING`, `TEXTAREA`, `NUMBER`, …) |
| `examples` on node/tool/hook | Use-case lines for UI discovery (e.g. "Summarize a document") |
| `featured` / `deprecated` / `experimental` | Palette badges on nodes/tools; hooks use deprecated/experimental only |
| `@OloProperty` UX fields | `label`, `help`, `placeholder`, `group`, `order`, `secret`, `examples` for polished editors |

Processed by [`olo-annotation-processor`](../olo-annotation-processor/) into `META-INF/olo/catalog/*.json`.

Load merged catalogs at runtime:

```java
import org.olo.annotation.catalog.ExtensionCatalog;
import org.olo.annotation.catalog.ExtensionCatalogLoader;

ExtensionCatalog catalog = ExtensionCatalogLoader.loadMerged(classLoader);
catalog.nodes().forEach(node -> System.out.println(node.name));
```

```gradle
dependencies {
    compileOnly 'org.olo:olo-annotation:0.1.0-SNAPSHOT'
    annotationProcessor 'org.olo:olo-annotation-processor:0.1.0-SNAPSHOT'
}
```
