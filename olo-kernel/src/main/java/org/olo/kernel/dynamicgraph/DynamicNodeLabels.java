package org.olo.kernel.dynamicgraph;

import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.kernel.toolcall.ToolLabelResolver;

/**
 * Labels for nodes injected at runtime into the active workflow graph.
 */
public final class DynamicNodeLabels {

    public static final String PREFIX = "Dyn-";

    private DynamicNodeLabels() {
    }

    public static String prefixed(String label) {
        if (label == null || label.isBlank()) {
            return PREFIX + "Node";
        }
        String trimmed = label.trim();
        if (trimmed.startsWith(PREFIX)) {
            return trimmed;
        }
        return PREFIX + trimmed;
    }

    public static NodeDefinition withDynamicLabel(NodeDefinition node, WorkflowDefinition graph) {
        String baseLabel = node.getLabel();
        if (baseLabel == null || baseLabel.isBlank()) {
            if (NodeType.TOOL.name().equals(node.getType())) {
                Object toolIdValue = node.getConfiguration() == null ? null : node.getConfiguration().get("toolId");
                String toolId = toolIdValue == null ? null : String.valueOf(toolIdValue);
                baseLabel = ToolLabelResolver.resolve(toolId, graph);
            } else if (node.getType() != null && !node.getType().isBlank()) {
                baseLabel = node.getType();
            } else {
                baseLabel = node.getId();
            }
        }
        return NodeDefinition.builder()
                .id(node.getId())
                .type(node.getType())
                .label(prefixed(baseLabel))
                .configuration(node.getConfiguration())
                .ports(node.getPorts())
                .execution(node.getExecution())
                .build();
    }
}
