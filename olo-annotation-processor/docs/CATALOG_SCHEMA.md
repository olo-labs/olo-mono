# Extension catalog JSON schema

Catalog files are generated at compile time and live under `META-INF/olo/catalog/`. Schema version is **`1.0`** (field `schemaVersion`).

## File types

### Per-kind files (`nodes.json`, `tools.json`, `hooks.json`)

```json
{
  "schemaVersion": "1.0",
  "moduleId": "olo-core-nodes",
  "catalogType": "nodes",
  "generatedAt": "2026-06-10T16:20:54.113802700Z",
  "nodes": [ … ]
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `schemaVersion` | string | yes | Catalog schema version (`"1.0"`) |
| `moduleId` | string | yes | Logical module id from `-Aolo.catalog.module` |
| `catalogType` | string | yes | Which array this file carries: `"nodes"`, `"tools"`, or `"hooks"` |
| `generatedAt` | string | yes | ISO-8601 instant when the processor ran. Useful for attribution and debugging; not used by merge or runtime. May be omitted or normalized in reproducible build pipelines (see [ARCHITECTURE.md — Document header](ARCHITECTURE.md#document-header)). |
| `generatedBy` | string | yes | Always `"olo-annotation-processor"` |
| `generatedByVersion` | string | yes | Processor release that wrote the file (currently `"1.0.0"`) |
| `moduleVersion` | string | no (reserved) | Plugin **implementation semver** — not emitted in v1; see [Module version (reserved)](#module-version-reserved) |
| `nodes` | array | if catalogType=nodes | Node descriptors |
| `tools` | array | if catalogType=tools | Tool descriptors |
| `hooks` | array | if catalogType=hooks | Hook descriptors |

### Runtime registry (`runtime.json`)

**Not Studio-facing.** JVM bindings are emitted separately so catalog JSON stays free of `implementationClass` / `spiInterface`:

```json
{
  "schemaVersion": "1.0",
  "moduleId": "olo-core-tools",
  "generatedAt": "…",
  "bindings": [
    {
      "kind": "TOOL",
      "id": "olo-core:http-tool",
      "implementationClass": "org.olo.core.tool.HttpTool",
      "spiInterface": "org.olo.spi.tool.Tool"
    }
  ]
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `bindings` | array | yes | One entry per extension in the module |
| `bindings[].kind` | string | yes | `"NODE"`, `"TOOL"`, or `"HOOK"` |
| `bindings[].id` | string | yes | Global extension id (same as catalog `id`) |
| `bindings[].implementationClass` | string | yes | Annotated type FQCN |
| `bindings[].spiInterface` | string | yes | SPI contract FQCN |

`ExtensionRuntimeRegistryLoader` merges `runtime.json` from all JARs (first id wins, same dedup rules as catalog). `olo-be` does **not** serve this file — workers and JVM tooling load it from the classpath.

### Convenience bundle (`catalog.json`)

**Not authoritative.** The processor emits `catalog.json` as a per-module snapshot for indexing, marketplace inspection, admin UIs, and CLIs. `ExtensionCatalogLoader` does **not** read this file — loading it alongside `nodes.json` / `tools.json` / `hooks.json` would duplicate every descriptor.

Same header fields except `catalogType` is omitted. Contains only non-empty arrays:

### Document `catalogType` vs descriptor `kind`

Do not confuse the **file header** `catalogType` (`"nodes"` / `"tools"` / `"hooks"`) with the **descriptor** field `kind` (`"NODE"` / `"TOOL"` / `"HOOK"` on each entry in an array). The header names which catalog file you are reading; the descriptor field tags the extension type of a single entry.

Type information appears at three levels in a per-kind file (`catalogType`, array name, per-entry `kind`). That overlap is **intentional** — do not remove `kind`. When catalogs are merged (`olo-core` `dist/catalog/catalog.json`, marketplace indexes, admin UIs scanning mixed arrays), every descriptor carries its own `kind` without inferring from the parent key. Convenience for cross-type search outweighs redundancy in single-type files.

```json
{
  "schemaVersion": "1.0",
  "moduleId": "olo-core-nodes",
  "generatedAt": "…",
  "nodes": [ … ]
}
```

### Runtime merged view (`ExtensionCatalogLoader`)

The loader merges **only** authoritative per-type files (`nodes.json`, `tools.json`, `hooks.json`) from every JAR on the classpath. After merge, consumers receive a typed `ExtensionCatalog` (not raw JSON):

```java
ExtensionCatalog catalog = ExtensionCatalogLoader.loadMerged();
catalog.schemaVersion(); // "1.0"
catalog.nodes();         // List<NodeDescriptor>
catalog.tools();         // List<ToolDescriptor>
catalog.hooks();         // List<HookDescriptor>
```

### Schema version vs module version

Three header fields answer different questions:

| Field | Question it answers | Example |
|-------|---------------------|---------|
| `schemaVersion` | What **JSON shape** is this? | `"1.0"` |
| `moduleId` | Which **plugin/module** produced this file? | `"olo-core-nodes"` |
| `moduleVersion` | What **release** of that plugin is this? | `"1.2.0"` (reserved — not emitted in v1) |

`schemaVersion` is about catalog **format**. `moduleVersion` is about plugin **implementation semver** — independent of catalog schema bumps.

### Module version (reserved)

**Not emitted in OLO 1.0.** Reserved for a future processor release. Consumers must **ignore** the field if present (forward-compatible via `@JsonIgnoreProperties(ignoreUnknown = true)`).

Planned header shape:

```json
{
  "schemaVersion": "1.0",
  "moduleId": "acme-slack-integration",
  "moduleVersion": "1.2.0",
  "catalogType": "tools",
  "generatedAt": "…",
  "tools": [ … ]
}
```

| Use case | How `moduleVersion` helps |
|----------|---------------------------|
| **Marketplace** | Display plugin release, filter by minimum version, gate publish/upload |
| **Diagnostics** | Log lines and support tickets: “`acme-slack-integration` **1.2.0** shipped `HTTP` tool metadata” |
| **Plugin compatibility** | Studio or worker checks `moduleVersion` against workflow/platform requirements before enabling an extension |

Likely source when implemented: Gradle/Maven project version via a processor option (for example `-Aolo.catalog.moduleVersion=1.2.0`) or build plugin integration. `ExtensionCatalogLoader` merge logic will not branch on `moduleVersion` for v1 — marketplace and UIs read it from per-file JSON or a future typed accessor.

`ExtensionCatalog.schemaVersion()` exposes the merged **catalog format** version only. A future API may expose per-module versions without conflating them with `schemaVersion`.

### Duplicate extension ids

When two JARs define the same `id` within a kind (node, tool, or hook) — for example `HTTP`, `EMAIL`, or `WEB_SEARCH` in two modules — duplicate ids generate a **warning log entry** including both module names. The first descriptor discovered on the classpath wins.

**Extension ids must be globally unique** across all plugins and core modules. Duplicate-id handling exists only for **diagnostics** and **backward compatibility** — it is **not** a supported override mechanism. Classpath order varies across IDEs, fat JARs, containers, and tests; plugin authors must not rely on it.

---

## Node descriptor

| Field | Type | Required | Source |
|-------|------|----------|--------|
| `kind` | string | yes | Always `"NODE"` |
| `id` | string | yes | Global id: `{provider}:{localType}` — materialized from `@OloNode.type()` and `provider` |
| `version` | string | yes | `@OloNode.version()` — extension semver (default `1.0.0`) |
| `provider` | string | yes | `@OloNode.provider()` or `-Aolo.catalog.provider` — owning plugin (`olo-core`, `acme-aws`, …) |
| `stability` | string | yes | `@OloNode.stability()` — `stable`, `beta`, or `experimental` |
| `name` | string | yes | `@OloNode.name()` |
| `description` | string \| null | no | `@OloNode.description()` |
| `category` | string | yes | `@OloNode.category()` (default `general`) |
| `emoji` | string \| null | no | `@OloNode.emoji()` |
| `tags` | string[] | yes | `@OloNode.tags()` |
| `examples` | string[] | yes | Use-case lines — not config values ([conventions](../../olo-annotation/docs/EDITOR_CONVENTIONS.md#examples--use-cases-not-configuration-values)) |
| `featured` | boolean | no | `@OloNode.featured()` — emitted only when `true` |
| `deprecated` | boolean | no | `@OloNode.deprecated()` — emitted only when `true` |
| `inputs` | port[] | yes | `@OloNode.inputs()` |
| `outputs` | port[] | yes | `@OloNode.outputs()` |
| `parameters` | parameter[] | yes | `@OloNode.configuration()` — emitted as `parameters` |
| `contract` | object | no | Machine-readable JSON Schema — see [Contract object](#contract-object). Omitted when unset. |
| `runtime` | object | yes (nodes/tools) | Orchestration hints — see [Runtime object (catalog)](#runtime-object-catalog). Omitted on hooks. |

Example:

```json
{
  "kind": "NODE",
  "id": "olo-core:PROMPT",
  "version": "1.0.0",
  "provider": "olo-core",
  "stability": "stable",
  "name": "Prompt",
  "description": "Template prompt assembly without external LLM call",
  "category": "llm",
  "emoji": "💬",
  "tags": ["prompt", "core"],
  "examples": [
    "Summarize a document",
    "Generate release notes",
    "Translate text"
  ],
  "featured": true,
  "inputs": [{ "id": "in", "name": "in", "schema": "any", "required": true }],
  "outputs": [{ "id": "out", "name": "out", "schema": "any" }],
  "parameters": [{
    "id": "prompt",
    "label": "Prompt Template",
    "type": "string",
    "description": "Template used by PromptNode",
    "required": false,
    "ui": {
      "widget": "STRING",
      "help": "Use {{input}} to reference workflow input.",
      "placeholder": "Summarize the following content",
      "order": 0
    },
    "examples": ["Summarize document", "Generate email"]
  }, {
    "id": "maxIterations",
    "label": "Max Iterations",
    "type": "number",
    "required": false,
    "ui": { "widget": "NUMBER" }
  }],
  "runtime": {
    "contractVersion": "1.0",
    "executionModel": "INLINE",
    "capabilities": []
  }
}
```

---

## Tool descriptor

| Field | Type | Required | Source |
|-------|------|----------|--------|
| `kind` | string | yes | Always `"TOOL"` |
| `id` | string | yes | Global id: `{provider}:{localId}` — materialized from `@OloTool.id()` and `provider` |
| `version` | string | yes | `@OloTool.version()` |
| `provider` | string | yes | `@OloTool.provider()` or `-Aolo.catalog.provider` |
| `stability` | string | yes | `stable`, `beta`, or `experimental` |
| `name` | string | yes | `@OloTool.name()` |
| `description` | string \| null | no | `@OloTool.description()` |
| `category` | string | yes | Default `tools` |
| `emoji` | string \| null | no | `@OloTool.emoji()` |
| `tags` | string[] | yes | `@OloTool.tags()` |
| `examples` | string[] | yes | `@OloTool.examples()` |
| `featured` | boolean | no | Emitted only when `true` |
| `deprecated` | boolean | no | Emitted only when `true` |
| `parameters` | parameter[] | yes | `@OloTool.arguments()` + `@OloTool.configuration()` merged |
| `contract` | object | no | Machine-readable JSON Schema — see [Contract object](#contract-object). Omitted when unset. |
| `runtime` | object | yes | Orchestration hints — see [Runtime object (catalog)](#runtime-object-catalog) |

---

## Hook descriptor

| Field | Type | Required | Source |
|-------|------|----------|--------|
| `kind` | string | yes | Always `"HOOK"` |
| `id` | string | yes | Global id: `{provider}:{localId}` — materialized from `@OloHook.implementationId()` and `provider` |
| `version` | string | yes | `@OloHook.version()` |
| `provider` | string | yes | `@OloHook.provider()` or `-Aolo.catalog.provider` |
| `stability` | string | yes | `stable`, `beta`, or `experimental` |
| `name` | string | yes | `@OloHook.name()` |
| `description` | string \| null | no | `@OloHook.description()` |
| `category` | string | yes | Default `observability` |
| `emoji` | string \| null | no | `@OloHook.emoji()` |
| `tags` | string[] | yes | `@OloHook.tags()` |
| `deprecated` | boolean | no | Emitted only when `true` |
| `phases` | string[] | yes | `@OloHook.phases()` enum names (`PRE`, `ON_ERROR`, `FINALLY`) |

Hooks do not have port or property arrays in the current schema.

---

## Port object (`@OloPort`)

| Field | Type | Required | Default |
|-------|------|----------|---------|
| `id` | string | yes | — |
| `name` | string \| null | no | `null` if blank |
| `schema` | string | yes | e.g. `any`, `string`, `object` |
| `required` | boolean | no | Emitted only when `true` |
| `description` | string \| null | no | `@OloPort.description()` |

---

## Parameter object (`parameters[]`)

Parameters are sorted by `ui.order` (ascending) in generated JSON. Same shape on nodes, tools, and workflow presets.

| Field | Type | Required | Default |
|-------|------|----------|---------|
| `id` | string | yes | Stable parameter key — from `@OloProperty.name()` / `@OloWorkflowParameter.name()` |
| `label` | string | yes | Display label (`name` accepted on deserialize for older catalogs) |
| `type` | string | yes | JSON value type via `ParameterSchemaMapping` — `string`, `number`, `boolean`, `object`, `array`, `enum`, … |
| `description` | string \| null | no | Developer/catalog summary |
| `help` | string \| null | no | End-user guidance in the property panel |
| `placeholder` | string \| null | no | — |
| `group` | string \| null | no | Omitted when default (`"General"`) — Studio treats missing as General. Use `Advanced`, `Security`, `Model Settings`, … when not General |
| `order` | int | no | Omitted when unset — Studio sorts unspecified fields after explicit ones |
| `required` | boolean | yes | Always `true` or `false` — never omitted |
| `validation` | object | no | Studio validation — omitted when unset |
| `validation.minLength` | int | no | Minimum string length |
| `validation.maxLength` | int | no | Maximum string length |
| `validation.minimum` | number | no | Minimum inclusive numeric bound |
| `validation.maximum` | number | no | Maximum inclusive numeric bound |
| `validation.step` | number | no | Numeric step increment |
| `defaultValue` | string \| null | no | — |
| `values` | string[] | no | Omitted when unset — emit only when `type` is `enum` |
| `secret` | boolean | no | Emitted only when `true` — mask as `*********` in UI |
| `examples` | string[] | no | Property use-case bullets — omitted when unset (use `placeholder` for sample config) |

UIs should ignore unknown `validation` fields for forward compatibility.

---

## Contract object

Machine-readable semantic contract for AI planners and strict validators. Sourced from `@OloNode.capabilityInputSchema()` / `@OloTool.capabilityInputSchema()` (annotation names unchanged). **Omitted** when neither schema is set.

| Field | Type | Required | Source |
|-------|------|----------|--------|
| `inputSchema` | object | no | JSON Schema object; omitted when unset |
| `outputSchema` | object | no | JSON Schema object; omitted when unset |

Example (`olo-core:web-search`):

```json
"contract": {
  "inputSchema": {
    "type": "object",
    "properties": {
      "query": { "type": "string", "description": "Search query" }
    },
    "required": ["query"]
  },
  "outputSchema": {
    "type": "object",
    "properties": {
      "results": { "type": "array", "items": { "type": "object" } }
    },
    "required": ["results"]
  }
}
```

---

## Catalog defaults (Studio inheritance)

Merged Studio bundles (`catalog.json`, `nodes.json`, `tools.json` from `exportStudioCatalog`) declare inherited runtime and editor baselines:

```json
"defaults": {
  "runtime": {
    "capabilities": ["DEBUG", "REPLAY"]
  },
  "connectionRules": { "strategy": "schema_match" },
  "connectionPolicy": {
    "maxInputs": -1,
    "maxOutputs": -1
  },
  "designer": {
    "nodeSize": { "width": 200, "height": 80 },
    "resizable": true,
    "draggable": true
  }
}
```

Per-extension `runtime.capabilities` lists **deviations only** (extras beyond defaults, or a full replacement when opting out of defaults). Omitted when effective capabilities equal the defaults. Capability names are {@link org.olo.spi.runtime.RuntimeCapability} enum values; resolve with {@link org.olo.spi.runtime.RuntimeCapabilities}.

Per-node `connectionPolicy` omits fields that match `defaults.connectionPolicy`.

Per-entry `designer` omits `nodeSize`, `resizable`, and `draggable` when they match `defaults.designer`. Palette and search fields are always entry-specific.

---

## Catalog metadata (closed vocabularies)

Schema definitions and closed enums — not per-extension inheritance defaults:

```json
"catalogMetadata": {
  "parameterWidgets": ["STRING", "TEXTAREA", "NUMBER", "SLIDER", "..."]
}
```

Widget values are {@link org.olo.spi.catalog.ParameterWidget} enum names. Legacy lowercase widgets normalize on deserialize.

---

## Runtime object (catalog)

Orchestration scheduling hints on each node/tool descriptor. **Not** JVM classpath bindings — those are in [`runtime.json`](#runtime-registry-runtimejson) (`implementationClass`, `spiInterface`).

| `executionModel` | Meaning |
|------------------|---------|
| `INLINE` | Execute inside the current workflow (in-process step) |
| `ACTIVITY` | Execute as a Temporal Activity (or equivalent schedulable unit) |
| `CHILD_WORKFLOW` | Execute as a child workflow boundary |
| `EXTERNAL` | Delegate to an external system |

Descriptor `version` is the **extension implementation semver** (e.g. `1.0.0`, `2.0.0`). `runtime.contractVersion` is the **runtime execution contract** version — they evolve independently.

| Field | Type | Required | Source | Emission |
|-------|------|----------|--------|----------|
| `contractVersion` | string | yes | `@OloNode.runtimeContractVersion()` / `@OloTool.runtimeContractVersion()` | Always (default `"1.0"`). Legacy alias `apiVersion` accepted on read only. |
| `executionModel` | string | yes | `@OloNode.executionModel()` / `@OloTool.executionModel()` | Always — Studio foundation for execution, replay, debugger, visualization, observability |
| `capabilities` | string[] | no | Deviations from `defaults.runtime.capabilities` | Omitted when effective equals defaults |
| `defaultTimeout` | string | no | `defaultTimeout()` | ISO-8601 duration (e.g. `PT30S`); omitted when blank |
| `defaultRetryPolicy` | string | no | `defaultRetryPolicy()` | `STANDARD`, `AGGRESSIVE`; omitted when `NONE` |

Inherited catalog defaults: `DEBUG`, `REPLAY`. Optional deviation tokens: `CHECKPOINT` (`supportsCheckpointing()`), `RETRY`, `TIMEOUT`, `ASYNC_COMPLETION`, `HEARTBEAT`. Workflow instances may also declare capabilities on `WorkflowDefinition.runtime` in olo-definition.

Defaults: nodes → `INLINE`; tools → `ACTIVITY`. Hooks omit `runtime`.

Examples:

```json
"runtime": {
  "contractVersion": "1.0",
  "executionModel": "INLINE"
}
```

```json
"runtime": {
  "contractVersion": "1.0",
  "executionModel": "ACTIVITY",
  "capabilities": ["CHECKPOINT", "RETRY", "TIMEOUT"],
  "defaultTimeout": "PT30S",
  "defaultRetryPolicy": "STANDARD"
}
```

```json
"runtime": {
  "contractVersion": "1.0",
  "executionModel": "CHILD_WORKFLOW",
  "capabilities": ["TIMEOUT"]
}
```

---

## Merge and deduplication

`ExtensionCatalogLoader` loads authoritative resources in this order:

1. `META-INF/olo/catalog/nodes.json` (all JAR occurrences)
2. `META-INF/olo/catalog/tools.json` (all JAR occurrences)
3. `META-INF/olo/catalog/hooks.json` (all JAR occurrences)

`catalog.json` is **not** loaded. For each resource path, every matching URL on the classpath is merged. Arrays are keyed by descriptor `id`. Duplicate ids generate a warning log entry including both module names; the first descriptor discovered on the classpath wins. See [Duplicate extension ids](#duplicate-extension-ids) — ids must be globally unique; merge deduplication is diagnostic only.

---

## JSON emission rules

The processor **materializes annotation defaults** so consumers never reconstruct them:

| Rule | Effective catalog value |
|------|-------------------------|
| **Port `name`** | `id` when annotation `name` is blank — always `"in"` not `null` |
| **Property `label`** | Explicit label, or humanized `name` (`maxIterations` → `"Max Iterations"`) |
| **Property `group`** | Omitted when annotation default / blank (`"General"`) — emit only for non-default sections |
| **Property `order`** | Omitted when annotation default (`Integer.MAX_VALUE`) — emit only when explicitly set |
| **Arrays** | Descriptor-level: never `null` — use `[]` for `tags`, `inputs`, `outputs`, `parameters`, node/tool `examples`, … Parameter-level `values` and `examples` are **omitted** when empty |
| **Ownership & version** | Always emit descriptor `version`, `provider`, and `stability` |
| **Parameter `required`** | Always emit `true` or `false` on every `parameters[]` entry |
| **Other booleans (Option A)** | Emit only non-default `true` (`featured`, `deprecated`, port `required`, …) — omit `false`. Missing means `false` |
| **Optional strings** | Omitted when blank (`description`, `help`, `placeholder`, `defaultValue`) — not written as `null` |
| **Contract** | Emit `contract` object only when `inputSchema` and/or `outputSchema` set |
| **Runtime** | Always emit `executionModel` and all scheduler booleans on nodes/tools |
| **Provenance** | `generatedBy`, `generatedByVersion` on every catalog file header |

Catalog consumers should read values as-is; see [ANNOTATIONS.md](../../olo-annotation/docs/ANNOTATIONS.md) for annotation source defaults.

### Extension id naming

Extension `id` values must be **globally unique** before 1.0. The processor materializes `{provider}:{localId}` when the annotation value does not already contain `:`.

| Style | Example | Notes |
|-------|---------|-------|
| **Required format** | `olo-core:http-tool`, `olo-core:PROMPT`, `acme-slack:notify` | Provider prefix prevents community collisions |
| **Annotation local id** | `http-tool` with `-Aolo.catalog.provider=olo-core` | Emitted as `olo-core:http-tool` in catalog and `runtime.json` |
| **Explicit global id** | `olo-core:http-tool` in `@OloTool.id()` and `@ToolId` | Preferred for core constants (`CoreToolIds`, `CoreNodeTypes`, …) |

SPI annotations (`@NodeType`, `@ToolId`, `@ImplementationId`) must match the **materialized global id** (local values are prefixed at validation time).

### Reserved (post-1.0, not emitted today)

| Field | On | Purpose |
|-------|-----|---------|
| `moduleVersion` | File header | Plugin implementation semver |
| `supportedTargets` | Hook descriptor | e.g. `["WORKFLOW", "NODE", "TOOL"]` if hooks attach at multiple levels |

Do not add these until the feature exists. Current schema is intentionally minimal for 1.0.

---

## Schema evolution

Catalog consumers **must ignore unknown fields**. Future catalog versions may add new header or descriptor attributes; existing readers should continue functioning without requiring immediate upgrades.

| Layer | Behavior |
|-------|----------|
| `ExtensionCatalogLoader` | `@JsonIgnoreProperties(ignoreUnknown = true)` on `CatalogFile` and all descriptor types |
| UIs and tooling | Skip unrecognized JSON keys when binding or rendering |
| Processor | Additive fields preferred; bump `schemaVersion` for breaking changes |

Property panels should already ignore unknown keys on individual properties (see [Property descriptor](#property-descriptor)). The same rule applies at every level of the catalog document.

---

## Versioning

| `schemaVersion` | Notes |
|-----------------|-------|
| `1.0` | Initial schema; processor and loader in `olo-annotation` / `olo-annotation-processor` |

Breaking changes should bump `schemaVersion` and update both the processor and `ExtensionCatalogLoader` consumers (for example `olo-ui`). The loader accepts legacy `version` → `schemaVersion` and `module` → `moduleId` as aliases during migration.
