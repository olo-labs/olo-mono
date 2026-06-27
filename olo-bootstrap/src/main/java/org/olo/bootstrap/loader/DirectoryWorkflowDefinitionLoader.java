package org.olo.bootstrap.loader;

import org.olo.bootstrap.exception.BootstrapException;
import org.olo.bootstrap.model.CachedWorkflowDefinition;
import org.olo.bootstrap.registry.WorkflowDefinitionKey;
import org.olo.bootstrap.registry.WorkflowDefinitionRegistry;
import org.olo.definition.serializer.JsonWorkflowSerializer;
import org.olo.definition.serializer.WorkflowSerializer;
import org.olo.definition.serializer.YamlWorkflowSerializer;
import org.olo.definition.validation.WorkflowValidator;
import org.olo.definition.dynamicgraph.DynamicSubgraphInjectionSupport;
import org.olo.definition.workflow.WorkflowDefinition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Scans a folder for workflow definition JSON/YAML files and builds a {@link WorkflowDefinitionRegistry}.
 */
public final class DirectoryWorkflowDefinitionLoader {

    private final WorkflowSerializer jsonSerializer;
    private final WorkflowSerializer yamlSerializer;

    public DirectoryWorkflowDefinitionLoader() {
        this(new JsonWorkflowSerializer(), new YamlWorkflowSerializer());
    }

    public DirectoryWorkflowDefinitionLoader(
            WorkflowSerializer jsonSerializer,
            WorkflowSerializer yamlSerializer) {
        this.jsonSerializer = Objects.requireNonNull(jsonSerializer, "jsonSerializer");
        this.yamlSerializer = Objects.requireNonNull(yamlSerializer, "yamlSerializer");
    }

    public WorkflowDefinitionRegistry load(Path scanFolder, boolean recursive) {
        Objects.requireNonNull(scanFolder, "scanFolder");
        if (!Files.exists(scanFolder)) {
            throw new BootstrapException("workflow definition scan folder does not exist: " + scanFolder);
        }
        if (!Files.isDirectory(scanFolder)) {
            throw new BootstrapException("workflow definition scan path must be a directory: " + scanFolder);
        }
        try {
            List<CachedWorkflowDefinition> workflows = new ArrayList<>();
            Map<String, String> idVersionLocations = new HashMap<>();
            Map<String, String> defaultLocations = new HashMap<>();

            try (Stream<Path> paths = listWorkflowFiles(scanFolder, recursive)) {
                List<Path> files = paths.sorted(Comparator.comparing(p -> p.getFileName().toString())).toList();
                for (Path file : files) {
                    CachedWorkflowDefinition cached = loadFile(file);
                    validateUniqueIndexes(cached, idVersionLocations, defaultLocations);
                    workflows.add(cached);
                }
            }

            if (workflows.isEmpty()) {
                throw new BootstrapException("no workflow definitions found in " + scanFolder);
            }

            return WorkflowDefinitionRegistry.of(scanFolder.toAbsolutePath().normalize(), workflows);
        } catch (IOException e) {
            throw new BootstrapException("failed to scan workflow definitions in " + scanFolder, e);
        }
    }

    private Stream<Path> listWorkflowFiles(Path directory, boolean recursive) throws IOException {
        int maxDepth = recursive ? Integer.MAX_VALUE : 1;
        return Files.walk(directory, maxDepth)
                .filter(Files::isRegularFile)
                .filter(this::isWorkflowFile);
    }

    private boolean isWorkflowFile(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".injection.json")) {
            return false;
        }
        return name.endsWith(".json") || name.endsWith(".yaml") || name.endsWith(".yml");
    }

    private CachedWorkflowDefinition loadFile(Path file) {
        String location = file.toAbsolutePath().normalize().toString();
        try {
            WorkflowDefinition definition = deserialize(file);
            var validation = WorkflowValidator.validate(definition);
            if (!validation.valid()) {
                throw new BootstrapException(location + ": " + String.join("; ", validation.errors()));
            }
            return new CachedWorkflowDefinition(location, definition);
        } catch (IOException e) {
            throw new BootstrapException("failed to read workflow definition from " + location, e);
        }
    }

    private WorkflowDefinition deserialize(Path file) throws IOException {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        String content = Files.readString(file);
        if (name.endsWith(".json") && DynamicSubgraphInjectionSupport.isInjectionDocument(content)) {
            return DynamicSubgraphInjectionSupport.loadBuilderWorkflow(content);
        }
        if (name.endsWith(".json")) {
            return jsonSerializer.deserialize(content);
        }
        return yamlSerializer.deserialize(content);
    }

    private static void validateUniqueIndexes(
            CachedWorkflowDefinition cached,
            Map<String, String> idVersionLocations,
            Map<String, String> defaultLocations) {
        WorkflowDefinition definition = cached.getDefinition();
        String location = cached.getSourcePath();

        if (definition.getId() != null && !definition.getId().isBlank()) {
            WorkflowDefinitionKey key = WorkflowDefinitionKey.from(definition);
            String previous = idVersionLocations.put(key.compositeKey(), location);
            if (previous != null) {
                throw new BootstrapException("duplicate workflow id+version '" + key.compositeKey()
                        + "' at " + location + " and " + previous);
            }
            if (Boolean.TRUE.equals(definition.isDefault())) {
                String previousDefault = defaultLocations.put(definition.getId(), location);
                if (previousDefault != null) {
                    throw new BootstrapException("duplicate default workflow for id '" + definition.getId()
                            + "' at " + location + " and " + previousDefault);
                }
            }
        }
    }
}
