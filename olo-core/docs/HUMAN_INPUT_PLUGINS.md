<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->

# Human-input plugins

Human-input plugins define the **operator form** shown when a workflow `HUMAN` node pauses. Each plugin is an `@OloTool` with `category = "human-input"`. The tool is **schema-only** at runtime (`HumanInputPluginSupport.schemaOnlyInvoke`) — it is not executed during graph traversal. Instead, the kernel resolves the plugin catalog entry and sends `parameters` + `options` to the chat UI.

## Wiring a workflow step

Reference a plugin from the human node approval block:

```json
{
  "id": "human-input",
  "type": "HUMAN",
  "execution": {
    "executionKind": "HUMAN_WAIT",
    "approval": {
      "title": "Approve container restart",
      "description": "Provide restart target details.",
      "approvers": ["operator"],
      "inputPluginId": "olo-core:human-input-restart-container"
    }
  }
}
```

At runtime, `HumanInputSchemaResolver` enriches the WAITING event `output` with:

| Field | Source |
|-------|--------|
| `inputType` | Always `"plugin"` |
| `inputPluginId` | Plugin id |
| `pluginName`, `pluginDescription` | Catalog tool metadata |
| `parameters` | Plugin `@OloProperty` arguments → `ParameterDescriptor[]` |
| `options` | `HumanInputPluginOptions` (footer action buttons) |

## Declaring fields in a plugin

Use `@OloProperty` on the `@OloTool.arguments` array. The annotation processor emits JSON Schema-style `type` plus `ui.widget` for each field.

```java
@OloProperty(
        name = "approveRestart",
        label = "Approve container restart?",
        type = OloPropertyType.APPROVAL_TOGGLE,
        required = true,
        description = "Select Yes to authorize the restart",
        group = "Approval",
        order = 0)
@OloProperty(
        name = "containerId",
        label = "Container ID",
        type = OloPropertyType.STRING,
        required = true,
        placeholder = "payment-api-7f8c9d",
        group = "Restart action",
        order = 1)
```

### Input types (`OloPropertyType` → UI)

| `OloPropertyType` | JSON `type` | `ui.widget` | Chat UI control |
|-------------------|-------------|-------------|-----------------|
| `STRING` | `string` | `STRING` | Single-line textbox |
| `TEXTAREA` | `string` | `TEXTAREA` | Multi-line text area |
| `NUMBER` | `number` | `NUMBER` | Numeric input |
| `BOOLEAN` | `boolean` | `BOOLEAN` | Checkbox |
| `APPROVAL_TOGGLE` | `boolean` | `APPROVAL_TOGGLE` | **Yes / No** button pair |
| `ENUM` | `enum` | `SELECT` | Dropdown (`enumValues`) |

Additional types (`SECRET`, `JSON`, `CODE`, `CRON`, `MODEL_SELECTOR`, `ARRAY`) follow the same catalog mapping used by workflow tools.

### Field metadata

| Annotation attribute | Catalog field | Purpose |
|---------------------|---------------|---------|
| `name` | `id` | Stable key in submitted JSON |
| `label` | `label` | Visible field title |
| `description` / `help` | `description`, `ui.help` | Operator guidance |
| `placeholder` | `ui.placeholder` | Empty-state hint |
| `group` | `ui.group` | Form section (fieldset) |
| `order` | `ui.order` | Sort order within group |
| `required` | `required` | Validation gate before Approve |
| `enumValues` | `values` | Allowed options for `ENUM` |
| `defaultValue` | `defaultValue` | Pre-filled value |

## Operator submit payload

The chat UI posts:

```http
POST /api/runs/{runId}/human-input
{ "approved": true, "message": "{...}" }
```

When the form has `parameters`, `message` is a JSON object string keyed by parameter `id`. Boolean and approval-toggle fields are encoded as JSON booleans (`true` / `false`). The kernel parses this via `HumanResumeInput.fromOperatorMessage` and stores structured values in `humanInputFields`.

Example after restart-container approval:

```json
{
  "approveRestart": true,
  "scopeNotes": "Latency spike on checkout",
  "containerId": "payment-api-7f8c9d",
  "namespace": "production"
}
```

## Footer actions (`options`)

`HumanInputPluginOptions` supplies plugin-specific action buttons (Approve / Cancel, Continue / Cancel, etc.). When no plugin options are defined, the kernel emits default **Approve** / **Cancel** buttons (`inputType: options`).

- **Form fields** — operator data entry (`parameters`, driven by `OloPropertyType`) — only when the plugin declares non-button fields
- **Footer buttons** — workflow actions (`options`, `{ label, approved }`) — preferred over free-text input

Approve buttons stay disabled until required fields are valid. When an `APPROVAL_TOGGLE` field is present, Approve requires **Yes** on every approval toggle.

## Built-in plugins

| Plugin id | Use case |
|-----------|----------|
| `olo-core:human-input-scenario-scope` | Scope intake before planner |
| `olo-core:human-input-restart-container` | Approve + container restart args |
| `olo-core:human-input-book-ticket` | Approve + travel booking args |
| `olo-core:human-input-create-pull-request` | Approve + PR metadata |
| `olo-core:human-input-git-revert` | Approve + revert args |

## Adding a new human-input plugin

1. Create `@OloTool(category = "human-input")` in `org.olo.core.tool.humaninput`.
2. Add the id to `CoreHumanInputPluginIds`.
3. Register footer actions in `HumanInputPluginOptions`.
4. Set `approval.inputPluginId` on the scenario `HUMAN` node (Java `ScenarioHumanStepSupport` or workflow JSON).
5. Rebuild `olo-core/tools` so `META-INF/olo/catalog/tools.json` is regenerated.
6. Restart the kernel worker so `ExtensionCatalogLoader` picks up the new catalog.

## Related code

| Component | Location |
|-----------|----------|
| Schema resolver | `HumanInputSchemaResolver` |
| Kernel handler | `HumanNodeTypeHandler` |
| Approval model | `HumanApprovalDefinition.inputPluginId` |
| Widget mapping | `ParameterSchemaMapping`, `ParameterWidget` |
| Chat rendering | `olo-chat` → `humanInputWidget.ts`, `ChatHumanInputCard.tsx` |

See also [olo-chat `CHAT_UI.md`](../../olo-chat/docs/CHAT_UI.md) for the operator-facing UI behavior.

## Contributors and owners

Contributions are welcome. Start with [CONTRIBUTING.md](../../CONTRIBUTING.md), use the [contributor guide](../../docs/CONTRIBUTOR_GUIDE.md) to find the right module or scenario, route review through [OWNERS.md](../../OWNERS.md), and record meaningful module or scenario credit in [CREDITS.md](../../CREDITS.md).
