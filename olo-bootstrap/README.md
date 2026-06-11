# olo-bootstrap

Builds an in-memory cache of workflow definitions from an `olo-configuration` folder.

## API

```java
Path scanFolder = Path.of("../olo-definition/olo-configuration/default");

// First call scans and caches; subsequent calls return the same registry.
WorkflowDefinitionRegistry registry = OloBootstrap.load(scanFolder, false);

registry.findById("agent");
registry.findByQueue("agent");

// Runtime refresh:
WorkflowDefinitionRegistry refreshed = OloBootstrap.load(scanFolder, false, true);
```

Called by `olo-worker` after `WorkerConfigurationProvider.load()` using `workflowDefinitions.scanFolder` from worker configuration.
