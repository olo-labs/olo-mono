<!--
Copyright (c) 2026 Olo Labs
SPDX-License-Identifier: Apache-2.0
-->
# olo-bootstrap

Builds an in-memory cache of workflow definitions from an `olo-configuration` folder.

## API

```java
Path scanFolder = Path.of("../olo-definition/olo-configuration/current-active");

// First call scans and caches; subsequent calls return the same registry.
WorkflowDefinitionRegistry registry = OloBootstrap.load(scanFolder, false);

registry.findById("agent");              // isDefault version, else highest version
registry.findByIdAndVersion("agent", "1.0.0");
registry.findByIdAndVersion("agent", "9.9.9"); // falls back to isDefault workspace
registry.findByQueue("agent");
registry.getWorkflowsByIdAndVersion();   // all loaded id@version artifacts

// Runtime refresh:
WorkflowDefinitionRegistry refreshed = OloBootstrap.load(scanFolder, false, true);
```

Called by `olo-worker` after `WorkerConfigurationProvider.load()` using `workflowDefinitions.scanFolder` from worker configuration.
