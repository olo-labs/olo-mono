<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->


# OLO

> **The Open Source Platform for Durable Multi-Agent AI Orchestration**

Build enterprise AI systems using durable workflows, distributed workers, visual orchestration, human-in-the-loop approvals, and intelligent multi-agent collaboration.

---

# 🎥 See OLO in Action

> **(PLACEHOLDER - Hero Video / GIF)**

---

# Why OLO?

Most AI frameworks help you build prompts.

OLO helps you build **AI systems**.

Instead of writing orchestration code, retries, routing, workflow state, approvals and distributed execution yourself, OLO provides a platform that handles them for you.

```
User

↓

Planner

↓

Dynamic Workflow

↓

Multiple AI Agents

↓

Tools & MCP

↓

Human Approval

↓

Distributed Workers

↓

Enterprise Result
```

---



# What Makes OLO Different?


| Traditional AI Framework | OLO                           |
| ------------------------ | ----------------------------- |
| Prompt → Response        | Complete AI Workflow Platform |
| Single Agent             | Multi-Agent Collaboration     |
| Stateless Execution      | Durable Workflows             |
| Local Runtime            | Distributed Workers           |
| Manual Orchestration     | Visual Workflow Engine        |
| Sequential Logic         | Dynamic Execution Graph       |
| Limited Visibility       | Live Execution Timeline       |


---



# ✨ Platform Features

✅ Multi-Agent Orchestration

✅ Durable Workflow Execution (Temporal)

✅ Dynamic Workflow Planning

✅ Visual Workflow Studio

✅ Human-in-the-loop

✅ Distributed Worker Runtime

✅ Java-first SDK

✅ Tool Calling

✅ MCP Ready

✅ Enterprise Architecture

✅ Modular Plugin System

---



# Platform Overview

> **(PLACEHOLDER - Platform Architecture Image)**

---



# Product Modules

OLO consists of multiple independent modules working together.


| Module             | Description                                         |
| ------------------ | --------------------------------------------------- |
| **OLO Backend**    | REST APIs, workflow orchestration, sessions, events |
| **OLO Studio**     | Visual workflow designer                            |
| **OLO Chat**       | Multi-agent conversational interface                |
| **OLO Worker**     | Distributed workflow execution                      |
| **OLO Kernel**     | Workflow runtime and graph execution                |
| **OLO Definition** | Portable workflow definitions                       |
| **OLO SDK**        | Java APIs for building workflows                    |
| **OLO Bootstrap**  | Runtime initialization                              |
| **OLO Extensions** | AI providers, tools and integrations                |


---



# Platform Demo



## 🤖 AI Software Company

> **(PLACEHOLDER)**

Watch multiple specialized AI agents design, implement, review and deliver software collaboratively.

---



## 🔒 Security Analyzer

> **(PLACEHOLDER)**

Analyze repositories using specialized security agents coordinated through durable workflows.

---



## 🧠 Dynamic Planner

> **(PLACEHOLDER)**

Planner generates workflow JSON dynamically.

The execution graph expands automatically while the workflow is running.

---



## 👥 Human Approval

> **(PLACEHOLDER)**

Pause workflows for approvals, corrections and additional input before execution continues.

---



# Repository Structure

```
.

├── olo-be
│   Spring Boot Backend
│
├── olo-ui
│   React Frontend
│
├── docs
│   Platform Documentation
│
├── Dockerfile
│
├── docker-compose.dev.yml
├── docker-compose.demo.yml
└── docker-compose.production.yml
```

---



# Quick Start



## Clone

```bash
git clone https://github.com/<org>/olo.git
```



## Start

```bash
docker compose -f docker-compose.demo.yml up
```

Open

```
http://localhost:3000
```

Start building AI workflows.

---



# Screenshots



## Chat

> **(PLACEHOLDER)**

---



## Studio

> **(PLACEHOLDER)**

---



## Workflow Timeline

> **(PLACEHOLDER)**

---



## Human Approval

> **(PLACEHOLDER)**

---



## Execution Events

> **(PLACEHOLDER)**

---



# Documentation



## Platform

- [docs/](docs/)

- [Contributor guide](CONTRIBUTING.md)
- [Module owners and credit](docs/MODULE_OWNERS.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Domain boundaries](docs/DOMAIN_BOUNDARIES.md)
- [Environment](docs/ENVIRONMENT.md)
- [Extensibility](docs/EXTENSIBILITY.md)
- [Performance](docs/PERFORMANCE.md)
- [Testing](docs/TEST_STRATEGY.md)
- [Stability](docs/STABILITY.md)

---



## Backend

- olo-be/README.md

---



## Frontend

- olo-ui/README.md

---



# Development



## Backend

```bash
cd olo-be
./gradlew bootRun
```

---



## Frontend

```bash
cd olo-ui
npm install
npm run dev
```

---



## Combined Docker

```bash
docker build -t olo .
docker run -p 3000:80 olo
```

---



# Roadmap

Current

- ✅ Multi-Agent Runtime
- ✅ Durable Workflows
- ✅ Visual Chat
- ✅ Distributed Workers
- ✅ Plugin Architecture

Coming Next

- Dynamic Graph Expansion
- Memory
- Marketplace
- Agent Registry
- Cloud Deployment
- Multi-region Execution

---



# Become a Founding Module Maintainer

OLO is growing rapidly and we're looking for engineers who want to shape the future of enterprise AI orchestration.

Current opportunities include:

- Studio
- Workflow Visualization
- AI Providers
- GitHub Integration
- Kubernetes
- MCP
- Documentation
- Security
- Examples
- Developer Experience

As a maintainer you'll help define the roadmap, review contributions, mentor the community and guide the evolution of your module.

See [docs/MODULE_OWNERS.md](docs/MODULE_OWNERS.md).

## Contributor credit

OLO credits contributors through PR notes, release notes, and module ownership records. Code, docs, tests, examples, reviews, design feedback and community support all count as project work. If you want to become a reviewer or owner, start with a focused contribution in one module and update the ownership map when your scope is clear.

---



# Community

We welcome contributors interested in:

- Artificial Intelligence
- Java
- Spring
- React
- TypeScript
- Temporal
- Distributed Systems
- Workflow Engines
- Developer Experience

Whether you're fixing your first issue or leading a module, we'd love to collaborate.

---



# Contributing

Start with:

[CONTRIBUTING.md](CONTRIBUTING.md)

Every contribution matters.

Our long-term goal is to build a sustainable open platform where contributors can grow into reviewers, maintainers and module owners.

---



# License

Apache License 2.0

---



# ⭐ Join the Journey

OLO is building an open platform for the next generation of enterprise AI systems.

If this vision resonates with you:

- ⭐ Star the repository
- 🍴 Explore the code
- 💬 Join discussions
- 🚀 Build a module
- ❤️ Become a founding maintainer

> **One Platform. Infinite AI Workflows.**

