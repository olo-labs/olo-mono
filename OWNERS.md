<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->
# OLO ownership guide

This file describes stewardship areas for module owners and reviewers. It is intentionally role-based until named maintainers are assigned publicly.

## Owner responsibilities

Module owners keep their area understandable, testable, and welcoming to contributors. Owners are expected to:

- Review changes that affect their module's public API, runtime behavior, or documentation.
- Keep the module README and architecture notes current.
- Label beginner-friendly issues and describe the expected test path.
- Credit meaningful contributions in [CREDITS.md](CREDITS.md).
- Help contributors find the right neighboring owner when a change crosses module boundaries.

## Stewardship areas

| Area | Paths | Owner focus |
|------|-------|-------------|
| Monorepo architecture | `README.md`, `docs/**` | Project map, module boundaries, contributor orientation |
| Workflow definition model | `olo-definition/**` except `olo-configuration/**` | Graph schema, serializers, validation, samples |
| Workflow scenarios | `olo-definition/olo-configuration/**` | Presets, scenario docs, active configuration guidance |
| Invocation payloads | `olo-workflow-input/**` | `WorkflowInput`, producer/consumer APIs, validation |
| Worker configuration | `olo-worker-configuration/**` | Worker settings, config providers, sample configs |
| Bootstrap registry | `olo-bootstrap/**` | Definition loading, registry behavior, scan refresh |
| Kernel context | `olo-kernel-context/**` | Runtime context construction, variables, UI callbacks |
| Kernel runtime | `olo-kernel/**` | Traversal, Temporal workflow contract, return resolution |
| Worker process | `olo-worker/**` | Worker startup, Temporal pollers, LLM checks, operations |
| Runtime SPI | `olo-spi/**` | Node/tool/hook contracts and extension interfaces |
| Core implementations | `olo-core/**` | Built-in nodes, tools, hooks, execution engine |
| Annotation catalog | `olo-annotation/**`, `olo-annotation-processor/**` | Metadata annotations and generated catalog schema |

## Review routing

- One owner review is expected for changes contained within one stewardship area.
- Two owner reviews are expected when a change crosses module contracts, runtime behavior, generated catalogs, or scenario activation.
- Documentation-only changes can be reviewed by the nearest module owner or a documentation maintainer.
- Security-sensitive tools, external network integrations, and credential-handling paths require an owner who understands the operational risk.

## Becoming an owner

Consistent contributors can become owners by repeatedly improving a module, reviewing related changes, and keeping docs current. Ownership should be earned through visible care: tests, design notes, issue triage, and helpful reviews.

