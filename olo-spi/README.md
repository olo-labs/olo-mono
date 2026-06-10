# olo-spi

**Service Provider Interface** for OLO runtime execution — contracts only, **no implementations**.

| Package | Contents |
|---------|----------|
| `org.olo.spi.context` | `ExecutionContext` |
| `org.olo.spi.node` | `Node`, `NodeRequest`, `NodeResult`, `NodeStatus` |
| `org.olo.spi.tool` | `Tool`, `ToolRequest`, `ToolResult`, `ToolStatus` |
| `org.olo.spi.hook` | `Hook`, `HookRequest`, `HookResult`, `HookPhase` |
| `org.olo.spi.extension` | `ExtensionPoint`, `NodeProvider`, `ToolProvider`, `HookProvider` |
| `org.olo.spi.annotation` | `@NodeType`, `@ToolId`, `@ImplementationId`, `@OloExtension` |

## Role in the stack

```
olo-definition     Workflow graph JSON (declarative)
olo-spi            Runtime execution contracts (this module)
olo-runtime        Graph engine + default executors (planned)
olo-extensions     Provider implementations — OpenAI, tools, hooks (planned)
```

`olo-spi` has **zero dependencies** on other OLO modules. Implementations in `olo-runtime` / `olo-extensions` depend on `olo-spi`, not the other way around.

## Example (implementation belongs in olo-extensions)

```java
@OloExtension(name = "Echo node")
@NodeType("ECHO")
public final class EchoNode implements Node {

    @Override
    public String nodeType() {
        return "ECHO";
    }

    @Override
    public NodeResult execute(NodeRequest request, ExecutionContext context) {
        return NodeResult.completed(request.input());
    }
}
```

## Build

```bash
./gradlew test
./gradlew publishToMavenLocal
```

Maven coordinates: `org.olo:olo-spi:0.1.0-SNAPSHOT`

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).
