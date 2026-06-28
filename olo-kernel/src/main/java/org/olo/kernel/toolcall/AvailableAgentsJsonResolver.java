package org.olo.kernel.toolcall;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.olo.definition.edge.EdgeDefinition;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.definition.workflow.WorkflowReferenceDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Builds {@code availableAgentsJson} from {@code agentPlug} edges on the orchestrator canvas.
 * Only child agents connected to the planner host's {@code agentPlug} input are eligible at runtime.
 */
public final class AvailableAgentsJsonResolver {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AvailableAgentsJsonResolver() {
    }

    public static String resolve(WorkflowDefinition graph, String plannerNodeId) {
        List<Map<String, Object>> agents = new ArrayList<>();
        for (EdgeDefinition edge : graph.getEdges()) {
            if (!plannerNodeId.equals(edge.getTargetNodeId()) || !"agentPlug".equals(edge.getTargetPortId())) {
                continue;
            }
            resolveAgentEntry(graph, edge.getSourceNodeId()).ifPresent(agents::add);
        }
        try {
            String json = MAPPER.writeValueAsString(agents);
            return filterAgentsByPlannerContext(graph, json);
        } catch (Exception e) {
            return "[]";
        }
    }

    private static String filterAgentsByPlannerContext(WorkflowDefinition graph, String json) {
        if (!PlannerContextRuntime.injectAgents(graph)) {
            return "[]";
        }
        return PlannerContextRuntime.selectedAgentIds(graph)
                .map(selected -> filterAgentJson(json, selected))
                .orElse(json);
    }

    private static String filterAgentJson(String json, Set<String> selectedAgentIds) {
        if (selectedAgentIds.isEmpty()) {
            return "[]";
        }
        try {
            var root = MAPPER.readTree(json);
            if (!root.isArray()) {
                return "[]";
            }
            var filtered = MAPPER.createArrayNode();
            for (var entry : root) {
                if (!entry.isObject()) {
                    continue;
                }
                String agentId = entry.has("agentId") ? entry.get("agentId").asText() : null;
                if (agentId != null && selectedAgentIds.contains(agentId)) {
                    filtered.add(entry);
                }
            }
            return MAPPER.writeValueAsString(filtered);
        } catch (Exception e) {
            return "[]";
        }
    }

    public static Set<String> resolveAgentIds(WorkflowDefinition graph, String plannerNodeId) {
        Set<String> agentIds = new LinkedHashSet<>();
        for (EdgeDefinition edge : graph.getEdges()) {
            if (!plannerNodeId.equals(edge.getTargetNodeId()) || !"agentPlug".equals(edge.getTargetPortId())) {
                continue;
            }
            resolveAgentEntry(graph, edge.getSourceNodeId())
                    .map(entry -> String.valueOf(entry.get("agentId")))
                    .filter(id -> id != null && !id.isBlank())
                    .ifPresent(agentIds::add);
        }
        if (!PlannerContextRuntime.injectAgents(graph)) {
            return Set.of();
        }
        LinkedHashSet<String> filtered = PlannerContextRuntime.selectedAgentIds(graph)
                .map(selected -> {
                    LinkedHashSet<String> allowed = new LinkedHashSet<>();
                    for (String agentId : agentIds) {
                        if (selected.contains(agentId)) {
                            allowed.add(agentId);
                        }
                    }
                    return allowed;
                })
                .orElseGet(() -> new LinkedHashSet<>(agentIds));
        return Set.copyOf(filtered);
    }

    public static boolean isAllowedAgent(WorkflowDefinition graph, String plannerNodeId, String agentId) {
        if (agentId == null || agentId.isBlank()) {
            return false;
        }
        return resolveAgentIds(graph, plannerNodeId).contains(agentId);
    }

    private static Optional<Map<String, Object>> resolveAgentEntry(WorkflowDefinition graph, String sourceNodeId) {
        Optional<NodeDefinition> sourceNode = graph.getNodes().stream()
                .filter(node -> sourceNodeId.equals(node.getId()))
                .findFirst();
        if (sourceNode.isEmpty() || !NodeType.AGENT.name().equals(sourceNode.get().getType())) {
            return Optional.empty();
        }
        String agentId = readDelegateAgentId(sourceNode.get());
        if (agentId == null || agentId.isBlank()) {
            return Optional.empty();
        }
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("agentId", agentId);
        String label = sourceNode.get().getLabel();
        if (label != null && !label.isBlank()) {
            entry.put("name", label);
        }
        graph.getChildWorkflows().stream()
                .filter(child -> agentId.equals(child.getWorkflowId()))
                .findFirst()
                .ifPresent(child -> entry.put("workflowVersion", child.getWorkflowVersion()));
        return Optional.of(entry);
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
            return workflow.getWorkflowId();
        }
        return null;
    }
}
