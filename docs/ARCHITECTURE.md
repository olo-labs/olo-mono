# OLO monorepo architecture

OLO separates **what a workflow is** (a portable graph definition), **how a worker is deployed** (settings and scan paths), and **what data a single run carries** (invocation input). Execution today is routed through **Temporal**; the **kernel** is the synchronous entry point that builds runtime context and resolves the workflow return message.

## Design principles

1. **Definition vs invocation vs deployment** — `WorkflowDefinition`, `WorkflowInput`, and worker settings are three different artifacts with different lifecycles.
2. **Standalone Gradle modules** — Each library has its own `settings.gradle`, wrapper, and `publishToMavenLocal` coordinates (`org.olo:*:0.1.0-SNAPSHOT`). There is no single root Gradle build yet.
3. **Dependency direction** — Data flows inward: `olo-definition` has no knowledge of workers; `olo-kernel` orchestrates context building but does not own graph execution (planned in `olo-runtime`).
4. **Configuration on disk** — Preset workflows live under `olo-configuration/<region>/*.json`. The API and worker both load the same JSON shape; Docker dev mounts a copy from `olo-docker/dev/configuration/olo-configuration`.

## Layer model

```mermaid
flowchart TB
    subgraph external [External systems]
        UI[olo-chat / olo-ui]
        API[olo backend]
        TMP[Temporal]
        REDIS[Redis]
    end

    subgraph data [Data — not Gradle modules]
        CFG[olo-configuration/default/*.json]
    end

    subgraph definition [Definition layer]
        OD[olo-definition]
    end

    subgraph boundary [Invocation boundary]
        OWI[olo-workflow-input]
    end

    subgraph deploy [Deployment layer]
        OWC[olo-worker-configuration]
        OB[olo-bootstrap]
    end

    subgraph runtime [Runtime layer — current]
        OKC[olo-kernel-context]
        OK[olo-kernel]
        OW[olo-worker]
    end

    subgraph planned [Planned]
        OR[olo-runtime]
        OE[olo-extensions]
    end

    CFG --> OB
    OD --> OWI
    OD --> OB
    OD --> OKC
    OWI --> OKC
    OWC --> OW
    OB --> OW
    OB --> OK
    OKC --> OK
    OK --> OW
    OW --> TMP
    API --> TMP
    API --> UI
    OW -->|UI callback| API
    OR -.-> OK
    OE -.-> OR
```

| Layer | Responsibility | Mutable at runtime? |
|-------|----------------|---------------------|
| **Definition** | Graph shape: nodes, edges, variables, capability, queue | No (versioned JSON) |
| **Invocation** | Per-run payload: user inputs, session, callback URL, routing | Yes (each chat message) |
| **Deployment** | Worker port, Temporal target, cache, `scanFolder` | Yes (config refresh) |
| **Bootstrap** | In-memory index of definitions by id / queue | Yes (`load(..., refresh)`) |
| **Kernel context** | Isolated graph copy + variable map + UI events | Per Temporal task |
| **Kernel** | Queue entry, return message resolution | Per Temporal task |
| **Worker process** | Temporal pollers, one per task queue | Long-lived |

## End-to-end chat flow

Typical local dev: **olo-docker** runs API + Temporal + Redis + chat UI; **olo-worker** runs on the host.

```mermaid
sequenceDiagram
    participant User
    participant Chat as olo-chat
    participant API as olo backend
    participant TMP as Temporal
    participant W as olo-worker
    participant K as olo-kernel
    participant KC as olo-kernel-context

    User->>Chat: Send message
    Chat->>API: POST /api/.../messages
    API->>API: Build WorkflowInput (userQuery, runId, callbackBaseUrl)
    API->>TMP: Start workflow on task queue (e.g. agent)
    TMP->>W: Task (WorkflowInput)
    W->>K: OloKernelWorkflow → KernelEntryPoint
    K->>KC: KernelContextBuilder.build()
    KC->>KC: Deserialize input, copy graph, init variables
    KC->>API: POST /api/runs/{runId}/events (CONTEXT_READY)
    K->>K: WorkflowReturnResolver (ReturnValue / fallback)
    KC->>API: POST /api/runs/{runId}/events (WORKFLOW_RESULT)
    K-->>W: String return message
    W-->>TMP: Workflow result
    TMP-->>API: Async completion
    API->>Chat: SSE / WebSocket (SYSTEM COMPLETED + response)
```

### Callback URL

The backend embeds a **host-reachable** callback base URL in `WorkflowInput` (e.g. `http://localhost:47080` when the worker runs on the host against olo-docker). The worker POSTs run events to:

`{callbackBaseUrl}/api/runs/{runId}/events`

Resolution order: `execution.callbackUrl` (full URL) → `context.callbackBaseUrl` + path.

### Workflow return message

Preset workflows declare a return variable in JSON:

```json
"metadata": { "returnVariable": "ReturnValue" },
"variables": [{
  "name": "ReturnValue",
  "type": "string",
  "metadata": { "role": "return" }
}]
```

When `metadata.returnVariable` is set, the kernel returns that variable's value from the runtime variable map. If the variable is missing or blank, the caller receives a fixed admin-contact message. When no return variable is configured, the kernel falls back to the user message from `WorkflowInput` (`userQuery` or first input).

Today the graph is **not executed** — `GraphIsolation.prepare()` is a stub — so `ReturnValue` is typically unset until `olo-runtime` populates variables during node execution.

## Worker bootstrap

On `WorkerBootstrap.start()`:

```mermaid
flowchart LR
    A[1. WorkerConfigurationProvider.load] --> B[2. Resolve scanFolder]
    B --> C[3. OloBootstrap.load]
    C --> D[4. TemporalWorkerFactory — one worker per queue]
    D --> E[5. Register OloKernelWorkflow per queue]
```

| Step | Module | Output |
|------|--------|--------|
| 1 | olo-worker-configuration | `WorkerSettings` (Temporal target, scan path, cache, port) |
| 2 | olo-worker | Absolute path to `olo-configuration/default` (or override) |
| 3 | olo-bootstrap | `WorkflowDefinitionRegistry` (12 presets → 12 queues) |
| 4–5 | olo-worker + olo-kernel | Temporal `Worker` per queue, `workflowType=olo` |

`start(true)` refreshes configuration and the definition registry without restarting the JVM.

## Configuration topology

Two configuration trees serve different roles:

| Path | Consumer | Purpose |
|------|----------|---------|
| `olo-configuration/default/*.json` | API (`OLO_CONFIGURATION_DIR`), worker (`scanFolder`) | `WorkflowDefinition` presets — chat profiles, task queues |
| `olo-worker-configuration/samples/*.yaml` | olo-worker only | Process settings — not workflow graphs |

Keep `olo-configuration`, `olo/olo-configuration`, and `olo-docker/dev/configuration/olo-configuration` aligned when changing presets.

## Build and local development

### Composite build (recommended for worker)

`olo-worker/settings.gradle` uses Gradle **composite builds** (`includeBuild`) for kernel and dependency modules. Running the worker compiles against monorepo sources directly:

```bash
cd olo-worker
./gradlew run
```

Restart the worker after editing `olo-kernel` or `olo-kernel-context`.

### Maven local (standalone modules)

When building a module in isolation:

```bash
cd olo-definition && ./gradlew publishToMavenLocal
# … repeat for dependents in dependency order (see MODULES.md)
```

### Docker + host worker

| Service | Host URL (olo-docker dev) |
|---------|---------------------------|
| OLO API | http://localhost:47080 |
| OLO Chat | http://localhost:43000 |
| Temporal | localhost:47233 |
| Redis | localhost:46379 |

Worker config: `olo-worker-configuration/samples/worker-config.local-debug.yaml`.

## Planned extensions

| Module | Role |
|--------|------|
| **olo-runtime** | Traverse and execute the workflow graph; write node outputs and `ReturnValue` |
| **olo-extensions** | LLM providers, tools, vector stores, MCP |
| **olo-annotation** / **olo-annotation-processor** | Reserved placeholders |

The kernel and kernel-context APIs are shaped so runtime execution can plug in after context build without changing the Temporal contract (`WorkflowInput` in, `String` message out).

## Related repositories

| Repo | Role |
|------|------|
| [olo](../olo/) | Spring Boot chat backend, REST/SSE/WebSocket, starts Temporal workflows |
| [olo-docker](../../olo-docker/) | Dev/prod Docker Compose stacks |
| [olo-chat](../olo-chat/) | Chat UI |
| [olo-ui](../olo-ui/) | Additional UI surfaces |
