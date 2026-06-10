# Editor conventions

How workflow UIs (for example `olo-ui` Studio) should interpret catalog metadata. This is **not** part of the annotation API — see [ANNOTATIONS.md](ANNOTATIONS.md) for contracts.

Runtime loading: typed [`ExtensionCatalog`](ARCHITECTURE.md#runtime-lifecycle) via `ExtensionCatalogLoader.loadMerged()` — not `JsonNode`.

---

## Catalog metadata is descriptive only

Catalog fields exist for **editors**. Runtime execution must **not** branch on:

| Never use at runtime | Editor-only |
|---------------------|-------------|
| `featured` | Palette ranking |
| `examples` (node/tool/property) | Discovery bullets |
| `help` | Property panel guidance |
| `placeholder` | Empty-input hint |
| `group` | Property section |
| `order` | Property sort |
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
| `tags` | Search / filter |
| `examples` | “Used for” bullets (use-cases) |
| `featured` | Optional ⭐ section (advisory) |
| `deprecated` / `stability` | Badges |

Non-featured control nodes (Parallel, Loop, Switch) may live under “Advanced” by editor policy.

### Tool picker

Same discovery and visibility fields as nodes: `id`, `version`, `provider`, `stability`, `name`, `description`, `category`, `emoji`, `examples`, `featured`, `deprecated`.

### Properties form

From `configuration` / `arguments` — see [ANNOTATIONS.md — `@OloProperty`](ANNOTATIONS.md#oloproperty) for the full attribute list. Key editor fields: `label`, `type`, `help`, `placeholder`, `group`, `order`, `required`, `defaultValue`, `enumValues`, `secret`, `examples` (use-cases only).

### Ports

`inputs` / `outputs`: `id`, `name`, `schema`, `required`, `description`.

### Hooks

`phases`, `implementationClass`, `version`, `provider`, `stability`, `deprecated`.

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
