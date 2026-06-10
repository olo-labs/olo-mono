# Annotation reference

API contracts for `org.olo.annotation`. Retention is **`CLASS`** — compiler and annotation processor only, not runtime reflection.

| Doc | Contents |
|-----|----------|
| [EDITOR_CONVENTIONS.md](EDITOR_CONVENTIONS.md) | How UIs interpret `featured`, `examples`, groups, badges, placeholders |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Lifecycle, `ExtensionCatalog`, metadata philosophy |
| [V1.md](V1.md) | Frozen OLO 1.0 surface |

**Why not `RUNTIME`?** Process at compile time → catalog JSON. Runtime uses `ExtensionCatalogLoader` or SPI (`@NodeType`, `@ToolId`). See [ARCHITECTURE.md — CLASS retention](ARCHITECTURE.md#why-class-retention-not-runtime).

Nested annotations (`@OloPort`, `@OloProperty`) use `@Target(ANNOTATION_TYPE)` on `@OloNode` / `@OloTool`.

**Intentionally omitted:** `difficulty`, `priority`, `rank`, `min`, `max`, `pattern`, `subcategory`, `icon`, `color` — see [V1.md](V1.md).

---

## `@OloNode`

**Target:** `ElementType.TYPE`  
**Maps to:** `NodeDefinition.type`, `org.olo.spi.node.Node`

| Attribute | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `type` | `String` | yes | — | Node type token; match `@NodeType` / `nodeType()` |
| `version` | `String` | no | `"1.0.0"` | Extension semver for Studio upgrade notices |
| `provider` | `String` | no | `""` | Owning plugin id — materialized from `-Aolo.catalog.provider` when blank |
| `name` | `String` | yes | — | Display name |
| `description` | `String` | no | `""` | Catalog / developer summary |
| `category` | `String` | no | `"general"` | Palette grouping |
| `emoji` | `String` | no | `""` | Palette icon |
| `tags` | `String[]` | no | `{}` | Search tags |
| `examples` | `String[]` | no | `{}` | **Use-case** lines (not config values) — [conventions](EDITOR_CONVENTIONS.md#examples--use-cases-not-configuration-values) |
| `featured` | `boolean` | no | `false` | Advisory ⭐ hint — [conventions](EDITOR_CONVENTIONS.md#featured-is-advisory-only) |
| `deprecated` | `boolean` | no | `false` | ⚠ legacy marker |
| `stability` | `OloStability` | no | `STABLE` | `stable`, `beta`, or `experimental` in catalog JSON |
| `experimental` | `boolean` | no | `false` | **Deprecated** — use `stability = EXPERIMENTAL` |
| `inputs` | `OloPort[]` | no | `{}` | Input ports |
| `outputs` | `OloPort[]` | no | `{}` | Output ports |
| `configuration` | `OloProperty[]` | no | `{}` | Config schema |
| `capabilityInputs` | `String[]` | no | `{}` | Planner semantic inputs |
| `capabilityOutputs` | `String[]` | no | `{}` | Planner semantic outputs |

```java
@OloNode(
    type = "PROMPT",
    name = "Prompt",
    description = "Template prompt assembly without external LLM call",
    featured = true,
    category = "llm",
    emoji = "💬",
    tags = {"prompt", "core"},
    examples = {"Summarize a document", "Generate release notes", "Translate text"},
    inputs = @OloPort(id = "in", name = "in", schema = "any", required = true),
    outputs = @OloPort(id = "out", name = "out", schema = "any"),
    configuration = @OloProperty(
        name = "prompt",
        label = "Prompt Template",
        type = OloPropertyType.TEXTAREA,
        description = "Template used by PromptNode",
        help = "Use {{input}} to reference workflow input."),
    capabilityInputs = {"input"},
    capabilityOutputs = {"output"})
@NodeType("PROMPT")
public final class PromptNode implements Node { … }
```

---

## `@OloTool`

**Target:** `ElementType.TYPE`  
**Maps to:** `ToolDefinition.id`, `org.olo.spi.tool.Tool`

| Attribute | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `id` | `String` | yes | — | Tool id; match `@ToolId` / `toolId()` |
| `version` | `String` | no | `"1.0.0"` | Extension semver |
| `provider` | `String` | no | `""` | Owning plugin id |
| `name` | `String` | yes | — | Display name |
| `description` | `String` | no | `""` | Catalog summary |
| `category` | `String` | no | `"tools"` | Palette grouping |
| `emoji` | `String` | no | `""` | Icon |
| `tags` | `String[]` | no | `{}` | Search tags |
| `examples` | `String[]` | no | `{}` | Use-case lines (not config values) |
| `featured` | `boolean` | no | `false` | Advisory ⭐ hint |
| `deprecated` | `boolean` | no | `false` | ⚠ legacy marker |
| `stability` | `OloStability` | no | `STABLE` | Maturity for marketplace UX |
| `experimental` | `boolean` | no | `false` | **Deprecated** — use `stability = EXPERIMENTAL` |
| `arguments` | `OloProperty[]` | no | `{}` | Per-invocation arguments |
| `configuration` | `OloProperty[]` | no | `{}` | Persistent configuration |
| `capabilityInputs` | `String[]` | no | `{}` | Planner semantic inputs |
| `capabilityOutputs` | `String[]` | no | `{}` | Planner semantic outputs |

```java
@OloTool(
    id = "HTTP",
    name = "HTTP",
    featured = true,
    examples = {"Call REST API", "Fetch weather data"},
    arguments = @OloProperty(name = "url", label = "URL", type = OloPropertyType.STRING, required = true))
@ToolId("HTTP")
public final class HttpTool implements Tool { … }
```

---

## `@OloHook`

**Target:** `ElementType.TYPE`  
**Maps to:** `HookActionDefinition.implementationId`, `org.olo.spi.hook.Hook`

| Attribute | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `implementationId` | `String` | yes | — | Hook id; match `@ImplementationId` |
| `version` | `String` | no | `"1.0.0"` | Extension semver |
| `provider` | `String` | no | `""` | Owning plugin id |
| `name` | `String` | yes | — | Display name |
| `description` | `String` | no | `""` | Summary |
| `category` | `String` | no | `"observability"` | Grouping |
| `emoji` | `String` | no | `""` | Palette icon (single emoji or short glyph) |
| `phases` | `OloHookPhase[]` | no | `PRE`, `ON_ERROR`, `FINALLY` | Supported phases |
| `tags` | `String[]` | no | `{}` | Search tags |
| `deprecated` | `boolean` | no | `false` | ⚠ legacy marker |
| `stability` | `OloStability` | no | `STABLE` | Maturity for marketplace UX |
| `experimental` | `boolean` | no | `false` | **Deprecated** — use `stability = EXPERIMENTAL` |

No `featured` or `examples` on hooks in v1.

---

## `@OloPort`

**Target:** `ANNOTATION_TYPE` (nested in `@OloNode`)

| Attribute | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `id` | `String` | yes | — | Port id in graph edges |
| `name` | `String` | no | `""` | Display label |
| `schema` | `String` | no | `"any"` | Type hint |
| `required` | `boolean` | no | `false` | Must be connected |
| `description` | `String` | no | `""` | Help under port label |

---

## `@OloProperty`

**Target:** `ANNOTATION_TYPE` (nested in `@OloNode` / `@OloTool`)

| Attribute | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `name` | `String` | yes | — | Key in configuration JSON |
| `label` | `String` | no | `""` | Display label |
| `type` | `OloPropertyType` | no | `STRING` | Editor control type |
| `description` | `String` | no | `""` | Developer/catalog summary |
| `help` | `String` | no | `""` | End-user panel guidance |
| `placeholder` | `String` | no | `""` | Sample text in empty input |
| `group` | `String` | no | `"General"` | Panel section — [conventions](EDITOR_CONVENTIONS.md#property-groups-free-text) |
| `order` | `int` | no | `Integer.MAX_VALUE` | Sort within group |
| `required` | `boolean` | no | `false` | Validation hint |
| `defaultValue` | `String` | no | `""` | Default when unset |
| `enumValues` | `String[]` | no | `{}` | Dropdown values when `type = ENUM` |
| `secret` | `boolean` | no | `false` | Mask input |
| `examples` | `String[]` | no | `{}` | **Use-case** bullets under field (not sample config — use `placeholder`) |

```java
@OloProperty(
    name = "prompt",
    label = "Prompt Template",
    type = OloPropertyType.TEXTAREA,
    description = "Template used by PromptNode",
    help = "Use {{input}} to reference workflow input.",
    placeholder = "Summarize the following content",
    group = "General",
    order = 0,
    examples = {"Summarize document", "Generate email"})
```

---

## `OloPropertyType`

| Value | UI control |
|-------|------------|
| `STRING` | Single-line text |
| `TEXTAREA` | Multi-line text |
| `NUMBER` | Numeric input |
| `BOOLEAN` | Toggle |
| `ENUM` | Dropdown (`enumValues`) |
| `JSON` | JSON editor |
| `SECRET` | Masked credential / API key |
| `ARRAY` | List editor (JSON array) |
| `OBJECT` | Structured object editor |
| `CODE` | Source or expression editor |
| `CRON` | Cron schedule picker |
| `MODEL_SELECTOR` | LLM / embedding model picker |

Serialized in catalog JSON as enum name (e.g. `"TEXTAREA"`).

---

## `OloStability`

| Value | Catalog JSON | Studio UX |
|-------|--------------|-----------|
| `STABLE` | `stable` | Default — production-ready |
| `BETA` | `beta` | Preview / limited support |
| `EXPERIMENTAL` | `experimental` | In progress |

Always emitted on every node, tool, and hook descriptor.

---

## `OloHookPhase`

`PRE`, `ON_ERROR`, `FINALLY` — aligns with `org.olo.spi.hook.HookPhase`.

---

## `OloCatalogLocations`

`NODES_CATALOG`, `TOOLS_CATALOG`, `HOOKS_CATALOG` (authoritative; merged by `ExtensionCatalogLoader`), and `MERGED_CATALOG` / `catalog.json` (convenience snapshot; not merged) under `META-INF/olo/catalog/`.

---

## Checklist for new extensions

1. Implement SPI (`Node`, `Tool`, or `Hook`).
2. Add SPI annotation (`@NodeType`, `@ToolId`, `@ImplementationId`).
3. Add `@OloNode` / `@OloTool` / `@OloHook` with matching id/type.
4. Declare ports, properties, capabilities per [EDITOR_CONVENTIONS.md](EDITOR_CONVENTIONS.md).
5. `compileOnly olo-annotation` + `annotationProcessor olo-annotation-processor`.
6. `-Aolo.catalog.module=<artifact-name>` and `-Aolo.catalog.provider=<plugin-id>` (e.g. `olo-core`, `acme-aws`).
7. Verify `META-INF/olo/catalog/*.json` in the JAR.
