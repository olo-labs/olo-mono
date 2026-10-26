<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->

# Contributing to OLO

OLO welcomes contributors who want to build durable multi-agent AI orchestration in the open. You can help with code, docs, examples, testing, design review, architecture feedback, integrations, and module ownership.

## Start here

1. Read the root [readme.md](readme.md) for the product vision and module map.
2. Pick a path below that matches how you want to help.
3. Read the owning module docs before opening a PR.
4. Keep the change focused, explain the user value, and include the right tests or review notes.

## Contributor paths

| Path | Good first work | Read first |
|------|-----------------|------------|
| Frontend | React components, Storybook stories, workflow builder UX, docs | [olo-ui/README.md](olo-ui/README.md), [olo-ui/docs/README.md](olo-ui/docs/README.md) |
| Backend | REST APIs, validation, configuration services, Redis fallback, docs | [olo-be/README.md](olo-be/README.md), [olo-be/docs/README.md](olo-be/docs/README.md) |
| Platform architecture | domain boundaries, extensibility, stability contracts | [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), [docs/DOMAIN_BOUNDARIES.md](docs/DOMAIN_BOUNDARIES.md) |
| Testing | unit tests, integration tests, Storybook coverage, test docs | [docs/TEST_STRATEGY.md](docs/TEST_STRATEGY.md) |
| Documentation | quickstarts, examples, diagrams, owner guides | [docs/README.md](docs/README.md), [docs/MODULE_OWNERS.md](docs/MODULE_OWNERS.md) |
| Operations | Docker, environment variables, deployment notes | [docs/ENVIRONMENT.md](docs/ENVIRONMENT.md), [docs/DOCKERHUB-PAGE.md](docs/DOCKERHUB-PAGE.md) |

## What makes a strong contribution

- It has a clear module owner or reviewer path.
- It keeps domain boundaries intact: components call store actions; stores own side effects.
- It updates docs when behavior, APIs, routes, environment variables, or contributor workflows change.
- It adds focused tests for changed behavior.
- It credits prior work, reviewers, and module owners when the change builds on their area.

## Pull request checklist

- Describe the problem and the user or contributor value.
- Link the module docs you followed.
- Name the affected module owner area from [docs/MODULE_OWNERS.md](docs/MODULE_OWNERS.md).
- Include test evidence, or explain why tests were not needed.
- Update docs for any public API, workflow, route, environment, layout, or contributor-facing change.

## Credit and ownership

OLO is designed so contributors can grow into reviewers, maintainers, and module owners. We credit people in three places:

- PR descriptions and release notes for shipped work.
- Module ownership records in [docs/MODULE_OWNERS.md](docs/MODULE_OWNERS.md).
- Documentation pages that describe an owned area or major contributor-led feature.

If you want to own a module, propose the scope, expected responsibilities, and first roadmap items in your PR or discussion.

## Community standard

Be practical, generous, and specific. Review the work, not the person. Leave enough context that a new contributor can learn from the thread.
