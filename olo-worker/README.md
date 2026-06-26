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

`run` defaults to `../olo-worker-configuration/samples/worker-config.local-debug.yaml`, which loads workflow definitions from `olo-definition/olo-configuration/current-active/` (same active folder as the olo backend and chat UI). Without Gradle args, the provider also falls back to monorepo samples when `worker-config.yaml` is missing in the working directory.

Override explicitly:

```bash
./gradlew run --args="../olo-worker-configuration/samples/worker-config.yaml"
# or
export OLO_WORKER_CONFIG_PATH=../olo-worker-configuration/samples/worker-config.local-debug.yaml
./gradlew run
```

When the worker runs **inside Docker** on `olo-net`, set `OLO_CHAT_CALLBACK_BASE_URL=http://olo:7080` on the `olo` service and use Temporal target `temporal:7233` in worker config.

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

Bootstrap logs each step (`Step 1/4` … `Step 4/4`). On failure, the log includes what failed and how to fix it (config path, scan folder, Temporal target). Set `org.slf4j.simpleLogger.defaultLogLevel=debug` for more detail.

## Refresh at runtime

```java
WorkerRuntimeContext refreshed = WorkerBootstrap.start(true);
```

### Studio UI refresh button (Redis)

olo-ui **Refresh** in the workflow builder calls `POST /api/v1/worker/refresh` on **olo-be**, which writes a new token to Redis key **`olo:worker:refresh`** (override with `olo.worker.refresh-key` / `OLO_WORKER_REFRESH_KEY`).

When `cache.enabled: true` in worker config, **olo-worker** polls that key every 2s (`OLO_WORKER_REFRESH_POLL_MS` to override). On value change it calls `WorkerBootstrap.start(true)` — reloading worker configuration, workflow definitions, and Temporal task queues without restarting the JVM.

Requirements:

- Redis reachable from **olo-be** (studio API) and **olo-worker**
- Worker config `cache.enabled: true` with matching `host` / `port`
- Same Redis key on both sides (default `olo:worker:refresh`)
