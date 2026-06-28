package org.olo.kernel.dynamicgraph;

import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.kernel.toolcall.AgentLabelResolver;
import org.olo.kernel.toolcall.ToolLabelResolver;

/**
 * Labels for nodes injected at runtime into the active workflow graph.
 */
public final class DynamicNodeLabels {

    public static final String LEGACY_PREFIX = "Dyn-";
    public static final String TOOL_PREFIX = "Dyn-Tool ";
    public static final String AGENT_PREFIX = "Dyn-Agent ";

    private DynamicNodeLabels() {
    }

    /** @deprecated Prefer {@link #prefixedTool(String)} or {@link #prefixedAgent(String)}. */
    @Deprecated
    public static String prefixed(String label) {
        if (label == null || label.isBlank()) {
            return LEGACY_PREFIX + "Node";
        }
        String trimmed = label.trim();
        if (trimmed.startsWith(LEGACY_PREFIX)) {
            return trimmed;
        }
        return LEGACY_PREFIX + trimmed;
    }

    public static String prefixedTool(String label) {
        return categoryPrefixed(TOOL_PREFIX, label, "Tool");
    }

    public static String prefixedAgent(String label) {
        return categoryPrefixed(AGENT_PREFIX, label, "Agent");
    }

    private static String categoryPrefixed(String prefix, String label, String fallback) {
        if (label == null || label.isBlank()) {
            return prefix + fallback;
        }
        String trimmed = label.trim();
        if (trimmed.startsWith(prefix)) {
            return trimmed;
        }
        if (trimmed.startsWith(LEGACY_PREFIX)) {
            trimmed = trimmed.substring(LEGACY_PREFIX.length()).trim();
        }
        return prefix + trimmed;
    }

    public static NodeDefinition withDynamicLabel(NodeDefinition node, WorkflowDefinition graph) {
        String baseLabel = node.getLabel();
        if (baseLabel == null || baseLabel.isBlank()) {
            if (NodeType.TOOL.name().equals(node.getType())) {
                Object toolIdValue = node.getConfiguration() == null ? null : node.getConfiguration().get("toolId");
                String toolId = toolIdValue == null ? null : String.valueOf(toolIdValue);
                baseLabel = ToolLabelResolver.resolve(toolId, graph);
            } else if (NodeType.AGENT.name().equals(node.getType())) {
                Object delegateAgentId = node.getConfiguration() == null
                        ? null
                        : node.getConfiguration().get("delegateAgentId");
                if (delegateAgentId != null) {
                    baseLabel = AgentLabelResolver.resolve(String.valueOf(delegateAgentId), graph, null);
                } else if (node.getType() != null && !node.getType().isBlank()) {
                    baseLabel = node.getType();
                } else {
                    baseLabel = node.getId();
                }
            } else if (node.getType() != null && !node.getType().isBlank()) {
                baseLabel = node.getType();
            } else {
                baseLabel = node.getId();
            }
        }
        String dynamicLabel = NodeType.TOOL.name().equals(node.getType())
                ? prefixedTool(baseLabel)
                : NodeType.AGENT.name().equals(node.getType())
                        ? prefixedAgent(baseLabel)
                        : prefixed(baseLabel);
        return NodeDefinition.builder()
                .id(node.getId())
                .type(node.getType())
                .label(dynamicLabel)
                .configuration(node.getConfiguration())
                .ports(node.getPorts())
                .execution(node.getExecution())
                .build();
    }
}
