package org.olo.kernel.toolcall;

import org.olo.definition.planner.PlannerContextDefinition;
import org.olo.definition.workflow.WorkflowDefinition;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Reads {@code metadata.plannerContext} selections saved from the UI and applies them at runtime.
 */
public final class PlannerContextRuntime {

    private PlannerContextRuntime() {
    }

    public static Optional<Set<String>> selectedToolIds(WorkflowDefinition graph) {
        return readSelection(graph, PlannerContextDefinition.SELECTED_TOOLS);
    }

    public static Optional<Set<String>> selectedAgentIds(WorkflowDefinition graph) {
        return readSelection(graph, PlannerContextDefinition.SELECTED_AGENTS);
    }

    public static boolean injectCapabilities(WorkflowDefinition graph) {
        return readBoolean(graph, PlannerContextDefinition.INJECT_CAPABILITIES, true);
    }

    public static boolean injectAgents(WorkflowDefinition graph) {
        return readBoolean(graph, PlannerContextDefinition.INJECT_AGENTS, true);
    }

    private static Optional<Set<String>> readSelection(WorkflowDefinition graph, String key) {
        Objects.requireNonNull(graph, "graph");
        Map<String, Object> metadata = graph.getMetadata();
        if (metadata == null) {
            return Optional.empty();
        }
        Object plannerContext = metadata.get(PlannerContextDefinition.METADATA_KEY);
        if (!(plannerContext instanceof Map<?, ?> context)) {
            return Optional.empty();
        }
        Object raw = context.get(key);
        if (!(raw instanceof List<?> list)) {
            return Optional.empty();
        }
        Set<String> values = new LinkedHashSet<>();
        for (Object entry : list) {
            if (entry == null) {
                continue;
            }
            String text = String.valueOf(entry).trim();
            if (!text.isBlank()) {
                values.add(text);
            }
        }
        return Optional.of(Set.copyOf(values));
    }

    private static boolean readBoolean(WorkflowDefinition graph, String key, boolean defaultValue) {
        Map<String, Object> metadata = graph.getMetadata();
        if (metadata == null) {
            return defaultValue;
        }
        Object plannerContext = metadata.get(PlannerContextDefinition.METADATA_KEY);
        if (!(plannerContext instanceof Map<?, ?> context)) {
            return defaultValue;
        }
        Object raw = context.get(key);
        return raw instanceof Boolean value ? value : defaultValue;
    }
}
