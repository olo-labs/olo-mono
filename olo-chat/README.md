<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->

# OLO Chat

> The interactive chat experience for **OLO** — a platform for building durable, multi-agent AI workflows.

OLO Chat is more than a chatbot. It is the primary interface for interacting with AI workflows, collaborating with specialized agents, monitoring execution in real time, providing human approvals, and visualizing workflow progress.

---

# 🎥 See OLO Chat in Action

> **(PLACEHOLDER - Hero GIF / Demo Video)**

---

# Why OLO Chat?

Traditional AI chat applications stop after a single model response.

OLO Chat provides a window into an entire AI workflow.

```
User
   │
   ▼
Planner
   │
   ▼
Multiple AI Agents
   │
   ▼
Tools & Integrations
   │
   ▼
Human Approval
   │
   ▼
Workflow Result
```

Watch every step as it happens.

---

# ✨ Features

### 💬 Multi-Agent Conversations

Choose different AI workflow presets such as:

* Ask
* Fast
* Detailed
* Custom enterprise workflows

Every message records the workflow that generated the response.

---

### ⚡ Live Workflow Execution

Watch workflows execute in real time.

* Current executing agent
* Progress updates
* Execution summaries
* Timeline events
* Worker status
* **Cancel** in-progress runs (signals Temporal cancellation; run again after cancel completes)

> **(PLACEHOLDER - Execution Timeline Screenshot)**

---

### 🤝 Human-in-the-Loop

OLO workflows can pause for human decisions.

Approve, reject or provide additional input directly inside the conversation.

> **(PLACEHOLDER - Human Approval Screenshot)**

---

### 🔄 Run Again

Replay the same prompt using different workflow presets without rewriting your request.

Compare different AI strategies instantly.

---

### 📚 Knowledge Management

Upload and manage enterprise knowledge sources for AI workflows.

> **(PLACEHOLDER - Knowledge Screenshot)**

---

### 📄 Document Upload

Upload documents directly into shared backend storage for workflow processing.

Supported workflows can consume uploaded documents automatically.

> **(PLACEHOLDER - Upload Screenshot)**

---

# UI Overview

| Area      | Purpose                          |
| --------- | -------------------------------- |
| Chat      | Multi-agent conversations        |
| Events    | Live workflow execution timeline |
| Knowledge | Knowledge source management      |
| Documents | File uploads                     |
| Presets   | Switch AI workflows instantly    |

> **(PLACEHOLDER - Full UI Screenshot)**

---

# Architecture

OLO Chat communicates with the OLO Backend using REST, Server-Sent Events (SSE), and WebSockets.

```
Browser

↓

OLO Chat

↓

OLO Backend

↓

Temporal

↓

Distributed Workers
```

The backend manages:

* Sessions
* Messages
* Workflow Runs
* Live Events
* Human Interaction

Task queues and workflow selection are automatically resolved from workflow definitions. Activate scenario presets via **olo-ui → Administration → Scenarios** (copies into `current-active` and refreshes workers).

---

# Quick Start

## Prerequisites

* Node.js 18+
* OLO Backend running (default: localhost:7080)

## Install

```bash
npm install
```

## Development

```bash
npm run dev
```

Open:

```
http://localhost:3000
```

---

# Screenshots

## Conversation

> **(PLACEHOLDER)**

---

## Workflow Events

> **(PLACEHOLDER)**

---

## Human Approval

> **(PLACEHOLDER)**

---

## Knowledge Management

> **(PLACEHOLDER)**

---

## Documents

> **(PLACEHOLDER)**

---

# Documentation

| Document                        | Description                                |
| ------------------------------- | ------------------------------------------ |
| docs/README.md                  | Project overview and development guide     |
| docs/ARCHITECTURE.md            | Frontend architecture and state management |
| docs/TECHNOLOGY_ARCHITECTURE.md | Technology stack and design                |
| docs/UI_FEATURES.md             | User interface capabilities                |
| docs/CHAT_UI.md                 | Chat APIs, workflow presets and events     |
| docs/DOCKER.md                  | Docker deployment                          |
| docs/DOCKER_HUB_DESCRIPTION.md  | Docker Hub description                     |

---

# Contributing

We're building the next generation of enterprise AI user experiences.

Whether you're interested in:

* React
* TypeScript
* UX
* Workflow Visualization
* AI Interfaces
* Developer Experience

there's an opportunity to contribute.

See:

```
CONTRIBUTING.md
```

---

# Looking for Founding UI Maintainers

We're actively looking for contributors interested in owning parts of the OLO Chat experience.

Potential ownership areas include:

* Conversation Experience
* Workflow Timeline
* Workflow Visualization
* Human Approval UI
* Knowledge Management
* Document Management
* Accessibility
* Theme System
* Plugin Framework

If you'd like to help shape the future of AI workflow interfaces, we'd love to collaborate.

---

# Related Projects

* OLO Backend
* OLO Worker
* OLO Studio
* OLO Kernel
* OLO SDK
* OLO Definition

Together they form the complete OLO platform for enterprise AI orchestration.

---

Apache License 2.0
