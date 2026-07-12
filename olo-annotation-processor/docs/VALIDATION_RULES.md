<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->
# Annotation processor validation rules

Compile-time checks for `OloExtensionCatalogProcessor`. Rules fail the build with a stable **OLO-AP-*** code so plugin authors can search docs and CI logs.

## Status

**Implemented in v1.** The processor validates each `@OloNode`, `@OloTool`, and `@OloHook` type before emitting catalog JSON. When any rule fails, catalog files are not written for that compilation.

## SPI consistency

Extension implementations must keep compile-time catalog metadata aligned with runtime SPI markers on the **same class**.

| Code | Condition | Example |
|------|-----------|---------|
| **OLO-AP-001** | `@OloNode.type()` missing or ≠ `@NodeType` value | `@OloNode(type = "PROMPT")` without `@NodeType`, or `@NodeType("PROMPT_V2")` |
| **OLO-AP-002** | `@OloTool.id()` missing or ≠ `@ToolId` value | `@OloTool(id = "http-tool")` + `@ToolId("http-v2")` |
| **OLO-AP-003** | `@OloHook.implementationId()` missing or ≠ `@ImplementationId` value | `@OloHook(implementationId = "logging-hook")` + `@ImplementationId("log-hook")` |

### Expected compiler output

```text
error: [OLO-AP-001] @OloNode.type "PROMPT" does not match @NodeType "PROMPT_V2" on sample.PromptNode
```

Error messages name the code, both annotations, both values (when applicable), and the enclosing type.

### Fix guidance

| Code | Resolution |
|------|------------|
| OLO-AP-001 | Use the same string for `@OloNode.type()` and `@NodeType` (prefer a shared constant, e.g. `CoreNodeTypes.PROMPT`). |
| OLO-AP-002 | Use the same string for `@OloTool.id()` and `@ToolId`. |
| OLO-AP-003 | Use the same string for `@OloHook.implementationId()` and `@ImplementationId`. |

The catalog id is what editors and workflow definitions reference; the SPI marker is what the runtime registry resolves — they must match.

## Duplicate extension ids (per module)

Duplicate ids across JARs are a runtime merge concern (see [CATALOG_SCHEMA.md — Merge and deduplication](CATALOG_SCHEMA.md#merge-and-deduplication)). Within a **single compilation module**, duplicate ids are rejected at compile time:

| Code | Condition |
|------|-----------|
| **OLO-AP-004** | Two `@OloNode` types with the same `type` |
| **OLO-AP-005** | Two `@OloTool` types with the same `id` |
| **OLO-AP-006** | Two `@OloHook` types with the same `implementationId` |

## Structural validation

| Code | Condition |
|------|-----------|
| **OLO-AP-007** | Duplicate `name` among `@OloProperty` entries on the same extension (`configuration`, `arguments`, or across both on tools) |
| **OLO-AP-008** | Duplicate `id` within `inputs` or within `outputs` on a node |
| **OLO-AP-009** | `@OloProperty` with `type = ENUM` and no `enumValues` |
| **OLO-AP-011** | Blank `version` on `@OloNode`, `@OloTool`, or `@OloHook` |
| **OLO-AP-012** | `capabilityInputSchema` or `capabilityOutputSchema` set but not a valid JSON object |
| **OLO-AP-013** | `defaultTimeout` set but not a valid ISO-8601 duration (e.g. `PT30S`) |

## Implementation notes

- The processor uses a **compile-only** dependency on `olo-spi` annotation types (`@NodeType`, `@ToolId`, `@ImplementationId`). SPI markers are `RUNTIME`, but the processor only reads their string values at compile time.
- Extension modules already depend on `olo-spi` for implementations; no new runtime coupling for consumers.

## Out of scope (for now)

- Port schema validation
- Workflow-definition compatibility checks
- Cross-module duplicate extension ids (runtime merge warning only)

## Migration guidance

When a new **OLO-AP-*** rule ships in a processor release:

1. **Fix at source** — align annotations on the reported type; do not suppress processor errors.
2. **Pin processor version** only as a short-term bridge while upgrading a large extension repo; prefer fixing mismatches before merging.
3. **CI** — treat `[OLO-AP-…]` messages as build failures; grep logs by code when triaging multi-module builds.

New rules will be appended to this document with their code, examples, and fix steps before the processor enforces them.
