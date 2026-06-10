# olo-core

Default **implementations** for OLO runtime SPI contracts (`olo-spi`).

## Usage

```gradle
dependencies {
    implementation 'org.olo:olo-core:0.1.0-SNAPSHOT'
}
```

```java
import org.olo.core.Core;
import org.olo.core.runtime.ExecutionEngine;

ExecutionEngine engine = Core.defaultEngine();
```

## Submodules

| Gradle project | Published artifact | Contents |
|----------------|-------------------|----------|
| `nodes` | `olo-core-nodes` | `PromptNode`, `AgentNode`, `ParallelNode`, `LoopNode`, `SwitchNode`, `ApprovalNode` |
| `tools` | `olo-core-tools` | `HttpTool`, `CalculatorTool`, `WebSearchTool` |
| `hooks` | `olo-core-hooks` | `LoggingHook`, `MetricsHook`, `TracingHook` |
| `runtime` | `olo-core-runtime` | `ExecutionEngine`, `NodeRegistry`, `ToolRegistry`, `HookRegistry` |
| `core` | **`olo-core`** | Aggregator — depend on this for everything |

## Build

Requires `olo-spi` in Maven local (or composite build):

```bash
cd ../olo-spi && ./gradlew publishToMavenLocal
cd ../olo-core
./gradlew test publishToMavenLocal
```

### Distribution folder (`dist/`)

```bash
./gradlew dist
```

| Path | Consumer | Purpose |
|------|----------|---------|
| `dist/lib/*.jar` | `olo-kernel`, worker | Runtime graph traversal + step execution |
| `dist/catalog/catalog.json` | `olo-be` → `olo-ui` | Merged editor catalog (`GET /api/v1/catalog`) |
| `dist/catalog/nodes.json` | local debug | Per-type slice (same merge, `catalogType: nodes`) |
| `dist/catalog/tools.json` | local debug | Per-type slice (`catalogType: tools`) |
| `dist/catalog/hooks.json` | local debug | Per-type slice (`catalogType: hooks`) |

Authoritative per-type files also live in plugin JARs (`META-INF/olo/catalog/`). Dist per-type files are **debug companions** alongside the merged `catalog.json`.

`olo-be` copies `dist/catalog/catalog.json` at build time (`syncExtensionCatalog`) and serves it at `/api/v1/catalog` — **no `org.olo` Java dependency in the backend**, JSON only for editor rendering.

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).
