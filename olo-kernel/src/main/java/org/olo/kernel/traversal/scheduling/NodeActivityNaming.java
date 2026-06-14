package org.olo.kernel.traversal.scheduling;

import org.olo.definition.capability.CapabilityDefinition;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.workflow.WorkflowDefinition;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Builds Temporal activity type names in {@code id:label} form.
 */
public final class NodeActivityNaming {

    private NodeActivityNaming() {
    }

    public static String format(String id, String label) {
        String resolvedId = normalizePart(id, "node");
        String resolvedLabel = normalizePart(label, resolvedId);
        return resolvedId + ":" + resolvedLabel;
    }

    public static String formatWorkflow(WorkflowDefinition graph) {
        Objects.requireNonNull(graph, "graph");
        return format(graph.getId(), graph.getLabel());
    }

    public static String formatQueue(String queue) {
        return format(queue, humanizeKebabCase(queue));
    }

    public static String formatNode(NodeDefinition node) {
        Objects.requireNonNull(node, "node");
        return format(node.getId(), resolveLabel(node));
    }

    static String resolveLabel(NodeDefinition node) {
        CapabilityDefinition capability = node.getCapability();
        if (capability != null && capability.getName() != null && !capability.getName().isBlank()) {
            return capability.getName().trim();
        }

        Map<String, Object> configuration = node.getConfiguration();
        if (configuration != null) {
            Object configuredLabel = configuration.get("label");
            if (configuredLabel != null) {
                String text = String.valueOf(configuredLabel).trim();
                if (!text.isBlank()) {
                    return text;
                }
            }
            if ("TOOL".equals(node.getType())) {
                Object toolId = configuration.get("toolId");
                if (toolId != null) {
                    return humanizeToolId(String.valueOf(toolId));
                }
            }
        }

        if (node.getType() != null && !node.getType().isBlank()) {
            return humanizeKebabCase(node.getType());
        }
        return node.getId();
    }

    private static String humanizeToolId(String toolId) {
        String trimmed = toolId.trim();
        int separator = trimmed.lastIndexOf(':');
        String suffix = separator >= 0 ? trimmed.substring(separator + 1) : trimmed;
        return humanizeKebabCase(suffix);
    }

    static String humanizeKebabCase(String value) {
        if (value == null || value.isBlank()) {
            return "Workflow";
        }
        String[] parts = value.trim().split("[-_\\s]+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            if (part.length() > 1) {
                builder.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return builder.isEmpty() ? value.trim() : builder.toString();
    }

    private static String normalizePart(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
