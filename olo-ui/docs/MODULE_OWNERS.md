<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->

# Module Owners and Contributor Credit

This file is the public map of OLO ownership. It helps visitors find the right place to contribute, helps reviewers route PRs, and gives visible credit to the people who steward each module.

## Ownership model

| Role | Responsibilities |
|------|------------------|
| Contributor | Ships focused fixes, docs, tests, examples, or feature slices. |
| Reviewer | Reviews changes in an area, helps contributors follow local patterns, and asks for tests/docs when needed. |
| Module owner | Maintains the roadmap and quality bar for one module, mentors contributors, and makes final calls when tradeoffs are unclear. |
| Founding maintainer | Stewards cross-module direction, release readiness, and long-term platform health. |

## Current module map

| Module | Scope | Owner credit |
|--------|-------|--------------|
| OLO Platform | Repository direction, cross-module contracts, contributor experience | Olo Labs founding maintainers |
| OLO Backend (`olo-be`) | Spring Boot API, configuration folders, catalog, Redis fallback, refresh endpoints | Seeking module owners |
| OLO Studio / Frontend (`olo-ui`) | React app, workflow builder, panels, Storybook, UI state | Seeking module owners |
| Workflow Builder | Canvas, catalog nodes, variables, tools, hooks, workflow JSON editing | Seeking module owners |
| Runtime / Execution Views | live runs, queues, metrics, run-level views | Seeking module owners |
| Ledger / History Views | historical runs, replay, cost, immutable run snapshots | Seeking module owners |
| Extensions and MCP | extension API, feature flags, plugin metadata, integrations | Seeking module owners |
| Environment and Docker | compose files, container image, runtime configuration, Docker Hub docs | Seeking module owners |
| Testing and Quality | test strategy, CI expectations, regression coverage | Seeking module owners |
| Documentation | onboarding, diagrams, examples, owner guides, release notes | Seeking module owners |
| Security and Governance | responsible disclosure, dependency posture, review policy | Seeking module owners |

## How to become a module owner

1. Make two or more meaningful contributions in the module or document a clear ownership plan.
2. Add or improve tests, examples, or docs that reduce future reviewer load.
3. Propose the ownership scope in a PR that updates this file.
4. List the first roadmap items and the review areas you are comfortable owning.

## How to credit contributors

- Add contributor names or handles in release notes for shipped work.
- Mention co-authors and reviewers in PR descriptions.
- Update this file when a person takes ownership of a stable module area.
- Keep credit tied to real stewardship: code, docs, review, testing, design, examples, or community support all count.

## Areas looking for help

| Area | Starter opportunities |
|------|-----------------------|
| Documentation | Replace placeholders with real screenshots, diagrams, demos, and examples. |
| Frontend | Add Storybook stories, polish builder interactions, improve empty/error states. |
| Backend | Strengthen validation, add API tests, improve configuration-folder safety. |
| Testing | Expand route, store, component, and API regression coverage. |
| Docker | Verify quickstarts across Windows, macOS, and Linux. |
| Examples | Add scenario folders that show real multi-agent workflows. |

## Review routing

When opening or reviewing a PR, use this order:

1. Check the touched paths.
2. Match them to the module map above.
3. Ask the current owner or reviewer for that area.
4. If no owner exists, tag it as an open ownership opportunity and include enough context for a new contributor to learn the module.
