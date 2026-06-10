package org.olo.annotation.catalog;

import java.util.List;

/**
 * Merged extension catalog from all {@code META-INF/olo/catalog/*.json} resources on the classpath.
 * <p>
 * Immutable — treat returned descriptor lists and fields as read-only. Consumers that need
 * mutable or enriched state should copy or wrap, not mutate catalog objects in place.
 */
public final class ExtensionCatalog {

    private final String schemaVersion;
    private final List<NodeDescriptor> nodes;
    private final List<ToolDescriptor> tools;
    private final List<HookDescriptor> hooks;

    ExtensionCatalog(
            String schemaVersion,
            List<NodeDescriptor> nodes,
            List<ToolDescriptor> tools,
            List<HookDescriptor> hooks) {
        this.schemaVersion = schemaVersion;
        this.nodes = List.copyOf(nodes);
        this.tools = List.copyOf(tools);
        this.hooks = List.copyOf(hooks);
    }

    /** Catalog JSON schema version (read from per-file {@code schemaVersion}). */
    public String schemaVersion() {
        return schemaVersion;
    }

    public List<NodeDescriptor> nodes() {
        return nodes;
    }

    public List<ToolDescriptor> tools() {
        return tools;
    }

    public List<HookDescriptor> hooks() {
        return hooks;
    }
}
