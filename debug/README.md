# OLO debug stack

Brings up the [olo-docker](../deploy/stack/dev) **dev** stack around this olo-mono checkout so an issue
can be reproduced end to end, with **one text model** in Ollama.

```powershell
cd D:\olo\olo-mono\debug
.\debug.ps1                 # or double-click debug.bat
```

## What runs

| Service | Container | Host endpoint | Source |
|---|---|---|---|
| PostgreSQL | `olo-db` | `localhost:45432` (pguser / pgpass) | Docker Hub |
| Redis | `olo-redis` | `localhost:46379` | Docker Hub |
| Elasticsearch (Temporal visibility) | `olo-elasticsearch` | http://localhost:29200 | Docker Hub |
| Qdrant | `olo-qdrant` | http://localhost:46333/dashboard | Docker Hub |
| Temporal | `olo-temporal` | `localhost:47233` | Docker Hub |
| Temporal UI | `olo-temporal-ui` | http://localhost:48092 | Docker Hub |
| Ollama (text model only) | `olo-ollama` | http://localhost:11435 | Docker Hub |
| OLO API | `olo` | http://localhost:47080/swagger-ui.html | `ololab/olo` (or local, see below) |
| OLO Chat | `olo-chat` | http://localhost:43000 | `ololab/olo-chat` (or local) |
| OLO UI | `olo-ui` | http://localhost:43001 | `ololab/olo-ui` (or local) |
| OLO Worker | `olo-worker` | — | **built locally** from this repo (`olo-worker:debug`) |

Not started: LocalAI, Kibana, pgAdmin, RedisInsight, Temporal admin-tools and the image/audio/video AI stacks.

The text model defaults to `llama3.2:latest` — the model every workflow in
`olo-definition/olo-configuration/current-active` uses. Override with `-Model` or `OLO_DEBUG_MODEL`.
The script warns if active workflows reference any other model (the worker's LLM health check would fail).

Workflow JSON (`olo-definition/olo-configuration`) and tool demo data (`olo-core/tools`) are mounted from
this checkout into `olo`, `olo-ui` and `olo-worker`, so edits here are what the stack runs.

## Required-container check

Before anything else starts, the script walks the required containers in dependency order —
**postgres → redis → elasticsearch → qdrant → temporal → ollama** — and for each one:

1. **missing** → creates it (first fails fast if its host port is already taken, and says by whom);
2. **stopped / exited** → starts it;
3. **running but not ready** → waits, restarts once, then stops with `docker inspect` state and the last 60 log lines.

Readiness is a real probe, not just "container is up": `pg_isready`, `redis-cli ping`, ES cluster health,
qdrant `/readyz`, `tctl cluster health == SERVING`, Ollama `/api/tags`. The overlay also adds matching
Docker health checks, so `docker ps` shows `(healthy)`.

A container with the expected name that belongs to a different compose project is reported; `-Force`
removes it. Run only this check with:

```powershell
.\debug.ps1 check
```

## Commands

| Command | What it does |
|---|---|
| `.\debug.ps1 up` | Check/start infra, pull the model, start apps and the locally built worker (default) |
| `.\debug.ps1 up -Worker host -JvmDebug` | Same, but the worker runs on the host via `gradlew run` with JDWP on 5005 |
| `.\debug.ps1 up -Worker none` | Everything except the worker (run it from your IDE yourself) |
| `.\debug.ps1 check` | Only the required-container check + model |
| `.\debug.ps1 status` | Table of state / health / readiness for every container |
| `.\debug.ps1 logs [-Service olo-worker]` | Follow logs |
| `.\debug.ps1 collect` | Save ps, merged compose config, logs and inspect output to `debug\logs\<timestamp>\` |
| `.\debug.ps1 worker [-Worker host]` | (Re)start only the worker after infra checks |
| `.\debug.ps1 down [-Volumes]` | Stop the `olo` compose project (`-Volumes` also deletes DB / ES / qdrant / model data) |

Options:

- `-LocalBuild olo-worker,olo,olo-ui,olo-chat` — build those images from `D:\olo\olo-mono`, `D:\olo\olo-mono\olo`,
  `D:\olo\olo-mono\olo-ui`, `D:\olo\olo-mono\olo-chat` instead of pulling. Default: `olo-worker` only.
- `-Rebuild` — force rebuilding the local images (after code changes).
- `-Cpu` — run Ollama without the NVIDIA GPU reservation.
- `-Timeout 180` — seconds to wait per container.
- `-OloDockerDir` / `OLO_DOCKER_DIR` — location of olo-docker (default `..\deploy\stack`).

## Worker on the host

With `-Worker host` the script stops the `olo-worker` container (so only one worker polls the Temporal
queue), recreates `olo` with `OLO_CHAT_CALLBACK_BASE_URL=http://localhost:47080`, and runs
`olo-worker` with `worker-config.local-debug.yaml` and `OLO_LLM_BASE_URL=http://localhost:11435`.
The worker's HTTP server binds `127.0.0.1:8080`; the script warns if that port is already in use.

## Layout

```
debug/
  debug.ps1                         main script
  debug.bat                         cmd / double-click wrapper
  compose/docker-compose.debug.yml  health checks + depends_on conditions
  compose/docker-compose.debug.cpu.yml
  compose/local/<service>.yml       local-build overlays
  logs/                             output of `collect` (git-ignored)
```

The compose project name is `olo` and the network `olo-net`, the same as `deploy/stack/dev/install.*`, so
containers and volumes created by the regular installer are reused rather than duplicated.
