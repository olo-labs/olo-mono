<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->
# olo-worker-configuration

**Single entry point for all worker configuration.** Worker code must read settings through this module so the storage medium (file today, database / Redis / GitHub later) can change without touching workers.

## Rule

| Do | Don't |
|----|-------|
| `WorkerConfigurationProvider.load()` → `WorkerSettings` | `System.getenv("SERVER_PORT")` |
| `settings.serverPort()`, `settings.workflowDefinitionsScanFolder()`, etc. | Hard-coded paths or env vars in worker bootstrap |

Bootstrap environment variables locate **where** configuration is stored — not the settings themselves:

| Variable | Purpose | Default |
|----------|---------|---------|
| `OLO_WORKER_CONFIG_SOURCE` | `FILE`, `DATABASE`, `REDIS`, `GITHUB` | `FILE` |
| `OLO_WORKER_CONFIG_PATH` | Config file path when source is `FILE` | `worker-config.yaml` |

## Example document

`samples/worker-config.yaml`:

```yaml
id: "default-worker"
name: "OLO Worker"
server:
  host: "0.0.0.0"
  port: 8080
workflowDefinitions:
  scanFolder: "../olo-definition/olo-configuration/current-active"
  recursive: false
temporal:
  namespace: "default"
  target: "localhost:47233"
cache:
  enabled: true
  host: "localhost"
  port: 46379
input:
  maxLocalMessageSize: 50
```

## Quick start

```java
import org.olo.worker.config.WorkerConfigurationProvider;
import org.olo.worker.config.WorkerSettings;

// At worker startup (uses OLO_WORKER_CONFIG_* bootstrap vars by default):
WorkerSettings settings = WorkerConfigurationProvider.load();

// Runtime refresh from storage:
WorkerSettings refreshed = WorkerConfigurationProvider.load(true);

int port = settings.serverPort();
var scanFolder = settings.workflowDefinitionsScanFolder();
int maxPayload = settings.maxLocalMessageSize();
```

Explicit file source (tests / embedded bootstrap):

```java
WorkerConfigurationProvider.configure(
        ConfigurationSourceFactory.forFile(Path.of("worker-config.yaml")));
WorkerSettings settings = WorkerConfigurationProvider.load();
```

## Adding a new storage medium

1. Implement `ConfigurationSource`.
2. Register it in `ConfigurationSourceFactory.forSourceType(...)`.
3. Add bootstrap variables in `WorkerConfigurationBootstrap` if needed.

Worker code stays unchanged — it always calls `WorkerConfigurationProvider`.

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Build

```bash
./gradlew test
./gradlew publishToMavenLocal
```
