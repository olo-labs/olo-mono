<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->
# olo-worker

Main OLO Temporal worker process.

## Bootstrap sequence

`WorkerBootstrap.start()` performs two cached loads:

1. **`WorkerConfigurationProvider.load()`** — worker settings (port, Temporal target, `scanFolder`, etc.)
2. **`OloBootstrap.load(scanFolder, recursive)`** — in-memory cache of workflow definitions from `olo-configuration`

Each Temporal task queue then registers **`olo-kernel`** (`OloKernelWorkflow`, `workflowType=olo`) as the execution entry point. The kernel calls **`olo-kernel-context`** to build runtime context from the queue payload and notify the UI callback.

Subsequent `start()` calls return the cached context. `start(true)` refreshes both layers from storage.

```java
WorkerRuntimeContext context = WorkerBootstrap.start();
context.settings().serverPort();
context.workflowRegistry().findByQueue("agent");
```

## Run

### Local debug against olo-docker dev stack

Use this when **olo API / Temporal / Redis run in Docker** and the worker runs on your machine (Gradle or IDE debugger).

| Service | Host URL |
|---------|----------|
| OLO API | http://localhost:47080 |
| OLO Chat UI | http://localhost:43000 |
| Temporal | localhost:47233 |
| Redis | localhost:46379 |

1. Start the dev stack (`olo-docker/dev/install.bat` or equivalent).
2. Ensure `olo` has `OLO_CHAT_CALLBACK_BASE_URL=http://localhost:47080` (default in `docker-compose-olo.yml`) so workflow callbacks reach the host worker.
3. Recreate `olo` after changing that env:  
   `docker compose -p olo ... up -d --force-recreate olo`
4. Run or debug the worker with `worker-config.local-debug.yaml` (Gradle `run` default).

**Ollama (required for AGENT workflows):** with the **olo-docker** dev stack, Ollama runs in container `olo-ollama` on the Docker network at `http://ollama:11434`. When the worker runs **on the host**, use the published host port (default `http://localhost:11435` in `docker-compose-ai-text.yml`). Gradle `run` and the VS Code launch config set `OLO_LLM_BASE_URL` accordingly. On Windows, port `51435` is often blocked by Hyper-V — if `docker compose up` fails to bind Ollama, check `netsh interface ipv4 show excludedportrange protocol=tcp` and pick a free host port.

**Observability demo data (log-reader, cpu-usage, etc.):** sample files live in `olo-core/tools/demo-data/`. The tools auto-discover that folder when the worker runs from `olo-worker` (or anywhere under `olo-mono`). Override with `OLO_DEMO_DATA_ROOT` pointing at `olo-core/tools` if needed.

```bash
cd olo-worker
./gradlew run
```

`settings.gradle` uses Gradle **composite builds** (`includeBuild`) for kernel modules, so local kernel changes are picked up without `publishToMavenLocal`. Restart the worker after editing `olo-kernel` or `olo-kernel-context`.

**IDE:** open `olo-mono/olo-worker` and use launch config **olo-worker (local debug, olo-docker)** (`.vscode/launch.json`).

`run` defaults to `../olo-worker-configuration/samples/worker-config.local-debug.yaml`, which loads workflow definitions from **`olo-definition/olo-configuration/current-active/`**. Activate a scenario in **olo-ui → Administration → Scenarios** (recommended) or copy presets there manually — see `olo-configuration/current-active/README.md`. olo-ui uses the same folder via `OLO_CONFIGURATION_DIRECTORY`.

Override explicitly:

```bash
./gradlew run --args="../olo-worker-configuration/samples/worker-config.yaml"
# or
export OLO_WORKER_CONFIG_PATH=../olo-worker-configuration/samples/worker-config.local-debug.yaml
./gradlew run
```

When the worker runs **inside Docker** on `olo-net`, set `OLO_CHAT_CALLBACK_BASE_URL=http://olo:7080` on the `olo` service and use Temporal target `temporal:7233` in worker config.

## Docker image

Build the worker image from the monorepo root:

```bash
docker build -f olo-worker/Dockerfile -t olo-worker:latest .
```

The image starts `olo-worker` with `/app/worker-config.yaml`, which points at the Docker network services (`temporal:7233`, `redis:6379`, `ollama:11434`). Override `OLO_WORKER_CONFIG_PATH` or mount a different config file if you want a custom deployment.


### Windows: `Unable to delete ... olo-definition.jar` / shared library jars

Composite builds used to share the same `olo-definition` and `olo-workflow-input` jar outputs between **olo-worker** and **olo backend**, which causes Windows file-lock failures. Those modules now publish to **`olo-mono/build/repo`** and are consumed as Maven dependencies (same pattern as `olo-spi` / `olo-annotation`).

If a build still fails with “Unable to delete … jar”:

1. Stop services: `stop.bat` (worker) and `..\..\olo\stop.bat` (backend), or run `Restart.Bat` from the repo root.
2. Release daemon locks: `unlock-build.bat` in this directory (stops Gradle daemons for worker, olo, and olo-ui `olo-be`).
3. Run `..\publish-libs.bat`, then retry the build or IDE run.

`stop.bat` runs `gradlew --stop` automatically after killing the worker on port 8080.

### Shared library jars (`olo-spi`, `olo-annotation`, `olo-annotation-processor`, `olo-definition`, `olo-workflow-input`)

These modules publish to **`olo-mono/build/repo`** (local Maven layout). Downstream projects (`olo-core`, `olo-worker`, `olo` backend) consume from that repo instead of composite `includeBuild`, so concurrent Gradle runs no longer fight over the same `build/libs/*.jar` on Windows.

After changing library sources:

```bash
cd olo-mono
./publish-libs.sh   # or publish-libs.bat on Windows
```

`start.bat` runs `publish-libs.bat` before `gradlew run`. For IDE runs, publish once (or after lib edits). Later this repo can be replaced with `mavenLocal()` or a remote Maven repository without changing consumer coordinates.

## Logging

Bootstrap logs each step (`Step 1/5` … `Step 5/5`). On failure, the log includes what failed and how to fix it (config path, scan folder, **Ollama/LLM URL**, Temporal target). Set `org.slf4j.simpleLogger.defaultLogLevel=debug` for more detail.

**Loaded workflows:** Step 3 logs **every** workflow JSON under `scanFolder`, including child-agent presets (`isDefault=false`), with `queue`, `id`, `version`, `source`, and node activity names. Previously only the primary workflow per queue was logged.

**Child workflow runs:** When a parent dispatches child agents (`agentCalls` or `CHILD_WORKFLOW` nodes), logs include:

- `Child workflow dispatch start/complete` — `childWorkflowId`, `parentWorkflowId`, `transactionId`, execution mode (`temporal-child` vs `in-process`)
- `Kernel entry` / `Traversal context ready` — `workflowId` and `transactionId` per child Temporal workflow
- Search logs by `transactionId` to follow parent → child execution in Worker Log / SLF4J output

**LLM health check (Step 4/5):** before Temporal starts, the worker probes `GET {OLO_LLM_BASE_URL}/api/tags` (10s timeout) and verifies required models are installed. If Ollama is down you get an immediate error instead of a 5-minute timeout on the first workflow activity.

```powershell
curl http://localhost:11435/api/tags
docker exec olo-ollama ollama pull llama3.2
```

## Refresh at runtime

```java
WorkerRuntimeContext refreshed = WorkerBootstrap.start(true);
```

### Studio UI refresh (Redis)

olo-ui **Refresh stack** (workflow builder toolbar) and **Administration → Scenarios → Activate** call `POST /api/v1/system/refresh` on **olo-be**, which:

1. Writes a new token to Redis key **`olo:worker:refresh`** (override with `olo.worker.refresh-key` / `OLO_WORKER_REFRESH_KEY`)
2. Reloads the **olo** runtime configuration (`POST /api/admin/configuration/reload` on port 7080)
3. Reloads studio catalog and workflows in the browser

When `cache.enabled: true` in worker config, **olo-worker** polls that key every 2s (`OLO_WORKER_REFRESH_POLL_MS` to override). On value change it calls `WorkerBootstrap.start(true)` — reloading worker configuration, workflow definitions, and Temporal task queues without restarting the JVM.

Requirements:

- Redis reachable from **olo-be** (studio API) and **olo-worker**
- Worker config `cache.enabled: true` with matching `host` / `port`
- Same Redis key on both sides (default `olo:worker:refresh`)

## Contributors and owners

Contributions are welcome. Start with [CONTRIBUTING.md](../CONTRIBUTING.md), use the [contributor guide](../docs/CONTRIBUTOR_GUIDE.md) to find the right module or scenario, route review through [OWNERS.md](../OWNERS.md), and record meaningful module or scenario credit in [CREDITS.md](../CREDITS.md).
