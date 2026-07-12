<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->
# Editor conventions

How workflow UIs (for example `olo-ui` Studio) should interpret catalog metadata. This is **not** part of the annotation API — see [ANNOTATIONS.md](ANNOTATIONS.md) for contracts.

Runtime loading: typed [`ExtensionCatalog`](ARCHITECTURE.md#runtime-lifecycle) via `ExtensionCatalogLoader.loadMerged()` — not `JsonNode`.

---

## `defaults` vs `catalogMetadata`

| Root key | Purpose | Examples |
|----------|---------|----------|
| `defaults` | Inherited baselines Studio merges with per-extension values | `runtime.capabilities`, `connectionRules`, `connectionPolicy`, `designer` |
| `catalogMetadata` | Closed vocabularies and schema definitions | `parameterWidgets` |

`parameterWidgets` is a catalog enum list — not a default value for extensions to inherit.

---

## Catalog metadata is descriptive only

Catalog fields exist for **editors**. Runtime execution must **not** branch on:

| Never use at runtime | Editor-only |
|---------------------|-------------|
| `featured` | Palette ranking |
| `examples` (node/tool/property) | Discovery bullets |
| `help` | Property panel guidance |
| `placeholder` | Empty-input hint |
| `visibleWhen` | Conditional field visibility |
| `group` | Property section |
| `order` | Property sort |
| `connectionPolicy` | Canvas edge attachment rules |
| `deprecated` / `stability` | Badges and warnings |
| `label`, `secret` | Form rendering |

```java
// Wrong — catalog UX must not drive execution
if ("experimental".equals(node.stability)) { … }

// Right — SPI + workflow definition JSON
if ("PROMPT".equals(request.nodeType())) { … }
```

See [ARCHITECTURE.md — Metadata philosophy](ARCHITECTURE.md#metadata-philosophy).

---

## `examples` = use-cases, not configuration values

`examples` on `@OloNode`, `@OloTool`, and `@OloProperty` describe **what you can do with** an extension or field — not sample values to paste into the form.

| Correct (`examples`) | Wrong (`examples`) | Use instead for sample text |
|----------------------|--------------------|-----------------------------|
| `Summarize document` | `Summarize the following article…` | `placeholder` on `@OloProperty` |
| `Call REST API` | `https://api.example.com/data` | `placeholder` or `defaultValue` |
| `Generate email` | Full prompt template body | `placeholder`, future `templates` |

### Extension level (node / tool)

Render as discovery bullets:

```
Used for:
• Summarize a document
• Generate release notes
• Translate text
```

### Property level

Render as inspiration under a field — still **use-cases**, not literals to insert:

```
Examples:
• Summarize document
• Generate email
```

For default or sample **configuration text**, use `placeholder`, `defaultValue`, or (future) node `templates` — not `examples`.

---

## `featured` is advisory only

`featured = true` is **not** “recommended by OLO”. It is a hint from the **extension author** that an editor *may* show the item in a ⭐ section.

- Editors **may ignore** `featured` or combine it with tenant policy, usage stats, and search ranking.
- Authors must **not** set `featured = true` on every extension — editors will down-rank inflated use.
- Use sparingly (for example Prompt, Approval in `olo-core`).

---

## Visibility badges

| Flag | UI label | Typical use |
|------|----------|-------------|
| `featured: true` | ⭐ Recommended (advisory) | Suggest surfacing first — editors may override |
| `deprecated: true` | ⚠ Deprecated | Legacy; warn on new use |
| `stability: experimental` | 🧪 Experimental | Preview / stub extensions |
| `stability: beta` | 🔬 Beta | Limited-support preview |
| `stability: stable` | — | Default production-ready |

Hooks support `deprecated` and `stability` only (no `featured` / `examples` in v1).

Flags can combine (for example Agent: `featured` + `stability: experimental`).

---

## Property groups (free text)

Use **exact** recommended names (case-sensitive):

| Group | Use for |
|-------|---------|
| `General` | Primary fields (default) |
| `Advanced` | Optional tuning |
| `Security` | Credentials (`secret = true`) |
| `Model Settings` | LLM/agent parameters |

Unknown groups may appear under “Other”. No `OloPropertyGroup` enum in v1 — conventions only.

Properties are sorted by `order` within each group in generated catalog JSON. Fields with an explicit `order` come first (ascending); fields without `order` in the catalog follow in stable annotation order — Studio should treat a missing `order` the same way.

A missing `group` means `"General"` (the annotation default). Only non-default groups (`Advanced`, `Security`, `Model Settings`, …) are emitted in catalog JSON.

---

## `description` vs `help` vs `placeholder`

| Field | Audience | Example |
|-------|----------|---------|
| `description` | Developers, catalogs, codegen | `Template used by PromptNode` |
| `help` | End users in the property panel | `Use {{input}} to reference workflow input.` |
| `placeholder` | Sample text inside an empty input | `Summarize the following content` |

```
Prompt Template
Use {{input}} to reference workflow input.
[ Summarize the following content          ]
```

---

## Palette and picker behavior

### Node palette

| Field | UI use |
|-------|--------|
| `category`, `name`, `emoji` | Grouping and display |
| `designer` | Reusable Studio metadata — see [Designer metadata](#designer-metadata) |
| `tags` | Search / filter |
| `examples` | “Used for” bullets (use-cases) |
| `featured` | Optional ⭐ section (advisory) |
| `deprecated` / `stability` | Badges |

Non-featured control nodes (Parallel, Loop, Switch) may live under “Advanced” by editor policy.

### Designer metadata

Reusable `designer` object on **workflows**, **workflow presets**, **nodes**, **tools**, and **hooks**:

```json
{
  "designer": {
    "paletteGroup": "Agents",
    "searchKeywords": ["planning", "task"],
    "nodeSize": {
      "width": 300,
      "height": 120
    },
    "resizable": true,
    "draggable": true
  }
}
```

| Field | Use |
|-------|-----|
| `paletteGroup` | Palette section label (falls back to title-cased `category`) |
| `searchKeywords` | Extra search terms (merged with `tags` when unset) |
| `nodeSize.width` / `nodeSize.height` | Canvas rendering hints (`@OloDesigner.canvasShape` when unset) |
| `resizable` / `draggable` | Canvas interaction hints (default `true`) |

Catalog `defaults.designer` documents the inherited baseline (`200×80`, `resizable: true`, `draggable: true`). Per-entry `designer` omits fields that match — Agent emits only `nodeSize: {300, 120}` plus palette/search metadata.

Declare via nested `@OloDesigner` on `@OloNode`, `@OloTool`, `@OloHook`, and `@OloWorkflowPreset`. Workflow JSON uses `designer` on the root workflow document.

Canvas sizes when `width` / `height` are unset on `@OloDesigner` (authoring hints — catalog baseline remains `STANDARD`):

| `canvasShape` | Default size |
|---------------|--------------|
| `STANDARD` | 200 × 80 |
| `AGENT` | 300 × 120 |
| `TOOL` | 160 × 72 |

### Tool picker

Same discovery and visibility fields as nodes: `id`, `version`, `provider`, `stability`, `name`, `description`, `category`, `emoji`, `examples`, `featured`, `deprecated`.

### Properties form

Nodes, tools, and workflow presets all use the same `parameters` array:

```json
"parameters": [
  {
  "id": "prompt",
  "label": "Prompt Template",
  "type": "string",
  "description": "Template used by PromptNode",
  "ui": {
    "widget": "TEXTAREA",
    "help": "Use {{input}} to reference workflow input.",
    "placeholder": "Summarize the following content",
    "order": 0
  }
}
]
```

- `parameters` — ordered array; each entry includes stable `id` and display `label`
- `id` — stable parameter key for saved workflows, migration, and Studio state (never rename)
- `label` — display text (may change without breaking stored values)
- `type` — JSON Schema value type (`string`, `number`, `integer`, `boolean`, `object`, `array`, `enum`)
- `values` — allowed options when `type` is `enum` (not `string` + `enumValues`)
- `ui.widget` — closed enum `org.olo.spi.catalog.ParameterWidget` (`STRING`, `TEXTAREA`, `SLIDER`, …)
- `ui.group`, `ui.help`, `ui.placeholder`, `ui.order` — presentation (same as workflow parameters)
- `required` — always `true` or `false` on every parameter (never omitted); Studio validates before workflow submission
- `validation` — bounds for client-side validation (`minLength`, `maxLength`, `minimum`, `maximum`, `step`)
- `visibleWhen` — conditional visibility; show the field only when sibling values match (string equality in v1)
- `@OloProperty.type` (`OloPropertyType`) is an authoring shortcut; catalogs emit the unified shape via `ParameterSchemaMapping`
- `secret: true` on `@OloProperty` emits `ui.widget: SECRET` (not a top-level `secret` flag)

Example — required string with length bounds:

```json
{
  "id": "url",
  "label": "URL",
  "type": "string",
  "required": true,
  "validation": {
    "minLength": 8,
    "maxLength": 2048
  }
}
```

Example — HTTP tool body visible only for POST:

```json
{
  "id": "body",
  "label": "Request Body",
  "type": "object",
  "visibleWhen": {
    "method": "POST"
  },
  "ui": {
    "widget": "JSON"
  }
}
```

Studio evaluates `visibleWhen` against sibling parameter values keyed by `id`. All listed keys must match (AND semantics). Omit `visibleWhen` when the field is always shown.

Catalog `catalogMetadata.parameterWidgets` lists all allowed widget values (closed enum — not an inheritance default). Legacy lowercase widgets (`slider`) normalize on deserialize.

### Ports

`inputs` / `outputs`: `id`, `name`, `schema`, `required`, `description`, `ui`.

| Field | UI use |
|-------|--------|
| `schema` | **Connection rule** for drag-and-drop — compare output `schema` to input `schema` |
| `ui.position` | Canvas side for the handle: `LEFT`, `RIGHT`, `TOP`, `BOTTOM` |

Defaults when omitted in catalog generation: inputs → `LEFT`, outputs → `RIGHT`. Override with `@OloPort(position = …)` for multi-handle nodes (for example branch outputs on `BOTTOM`).

### Connection rules (port `schema`)

Studio validates edges while dragging using the same rules as `WorkflowValidator` at save time. Catalog `defaults.connectionRules` describes the strategy for clients.

| Rule | Behavior |
|------|----------|
| Exact match | `string → string` ✓, `Stock[] → Stock[]` ✓ |
| No coercion | `string → number` ✗ |
| Wildcards | Input or output `any` / `*` accepts any counterpart |
| Primitives | Case-insensitive aliases (`String`, `str` → `string`) |
| Domain types | Case-sensitive (`Stock[]` ≠ `stock[]`) |
| Arrays | Element types must match; `string` ≠ `string[]` |

Java: `org.olo.spi.port.PortSchemaCompatibility` / `org.olo.annotation.catalog.PortConnectionRules`  
TypeScript: `olo-ui/src/lib/portConnection.ts` (`arePortSchemasCompatible`)

### Connection policy (node-level)

Controls how many edges may attach to a node on the canvas. Declared per node type via `@OloConnectionPolicy` on `@OloNode`:

```json
{
  "connectionPolicy": {
    "maxInputs": 1,
    "maxOutputs": -1
  }
}
```

| Field | Default | Use |
|-------|---------|-----|
| `maxInputs` | `-1` (unlimited) | `1` for single fan-in control nodes (Switch, Parallel, Loop) |
| `maxOutputs` | `-1` (unlimited) | Positive integer to cap outgoing edges |

`-1` means unlimited. Omitted from catalog when both match platform defaults (`-1` / `-1`). Catalog `defaults.connectionPolicy` documents the baseline.

### Hooks

`phases`, `version`, `provider`, `stability`, `deprecated`. JVM bindings (`implementationClass`) are in `runtime.json`, not Studio catalog.

---

## Reserved: node templates (post-v1)

Not v1. **Templates** would preset configuration when a node is added (distinct from `examples`):

```
Add Prompt Node
Templates:
○ Summarize Document  → fills prompt field
```

See [V1.md](V1.md).

---

## What not to add yet

The v1 model already supports palette generation, search, categorization, property forms, grouping, onboarding, recommendations, and in-editor documentation. **Do not add** until a schema version bump:

`min`, `max`, `pattern`, `subcategory`, `difficulty`, `priority`, `icon`, `color`, …

Reserved directions: [V1.md](V1.md).
