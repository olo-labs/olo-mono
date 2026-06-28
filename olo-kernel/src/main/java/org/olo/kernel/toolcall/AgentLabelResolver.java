package org.olo.kernel.toolcall;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.workflow.ChildWorkflowDefinition;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.definition.workflow.WorkflowReferenceDefinition;

/**
 * Resolves human-readable agent labels for dynamically injected AGENT nodes.
 */
public final class AgentLabelResolver {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AgentLabelResolver() {
    }

    public static String resolve(String agentId, WorkflowDefinition graph, String plannerNodeId) {
        return resolve(agentId, graph, plannerNodeId, null);
    }

    public static String resolve(
            String agentId, WorkflowDefinition graph, String plannerNodeId, String availableAgentsJson) {
        if (agentId == null || agentId.isBlank()) {
            return "Agent";
        }
        String fromCanvas = resolveFromCanvasAgentNode(agentId, graph);
        if (fromCanvas != null) {
            return fromCanvas;
        }
        String fromChildWorkflows = resolveFromChildWorkflows(agentId, graph);
        if (fromChildWorkflows != null) {
            return fromChildWorkflows;
        }
        if (plannerNodeId != null && graph != null) {
            String fromAvailableAgents = resolveFromAvailableAgentsJson(
                    agentId, AvailableAgentsJsonResolver.resolve(graph, plannerNodeId));
            if (fromAvailableAgents != null) {
                return fromAvailableAgents;
            }
        }
        String fromJson = resolveFromAvailableAgentsJson(agentId, availableAgentsJson);
        if (fromJson != null) {
            return fromJson;
        }
        return humanizeAgentId(agentId);
    }

    static String resolveFromCanvasAgentNode(String agentId, WorkflowDefinition graph) {
        if (graph == null || graph.getNodes() == null) {
            return null;
        }
        for (NodeDefinition node : graph.getNodes()) {
            if (!NodeType.AGENT.name().equals(node.getType())) {
                continue;
            }
            if (!agentId.equals(readDelegateAgentId(node))) {
                continue;
            }
            if (node.getLabel() != null && !node.getLabel().isBlank()) {
                return node.getLabel().trim();
            }
        }
        return null;
    }

    static String resolveFromChildWorkflows(String agentId, WorkflowDefinition graph) {
        if (graph == null || graph.getChildWorkflows() == null) {
            return null;
        }
        for (ChildWorkflowDefinition child : graph.getChildWorkflows()) {
            if (child != null && agentId.equals(child.getWorkflowId())) {
                return humanizeAgentId(agentId);
            }
        }
        return null;
    }

    static String resolveFromAvailableAgentsJson(String agentId, String availableAgentsJson) {
        if (availableAgentsJson == null || availableAgentsJson.isBlank()) {
            return null;
        }
        try {
            JsonNode root = MAPPER.readTree(availableAgentsJson.trim());
            if (!root.isArray()) {
                return null;
            }
            for (JsonNode entry : root) {
                if (!entry.isObject()) {
                    continue;
                }
                JsonNode entryAgentId = entry.get("agentId");
                if (entryAgentId == null || !entryAgentId.isTextual() || !agentId.equals(entryAgentId.asText())) {
                    continue;
                }
                JsonNode name = entry.get("name");
                if (name != null && name.isTextual() && !name.asText().isBlank()) {
                    return name.asText().trim();
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private static String readDelegateAgentId(NodeDefinition node) {
        if (node.getConfiguration() != null) {
            Object configured = node.getConfiguration().get("delegateAgentId");
            if (configured != null) {
                String delegateAgentId = String.valueOf(configured).trim();
                if (!delegateAgentId.isBlank()) {
                    return delegateAgentId;
                }
            }
        }
        WorkflowReferenceDefinition workflow = node.getWorkflow();
        if (workflow != null && workflow.getWorkflowId() != null && !workflow.getWorkflowId().isBlank()) {
            return workflow.getWorkflowId().trim();
        }
        return null;
    }

    private static String humanizeAgentId(String agentId) {
        String normalized = agentId.trim().replace('-', ' ');
        if (normalized.isEmpty()) {
            return "Agent";
        }
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }
}
