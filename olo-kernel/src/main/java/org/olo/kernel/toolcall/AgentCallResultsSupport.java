/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.toolcall;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.olo.definition.toolcall.ToolCallPlannerSupport;
import org.olo.kernel.context.variables.WorkflowRuntimeVariables;
import org.olo.kernel.toolcall.model.ParsedAgentCall;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tracks child-agent delegation results to prevent re-dispatch loops.
 */
public final class AgentCallResultsSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AgentCallResultsSupport() {
    }

    public static Set<String> completedAgentIds(WorkflowRuntimeVariables variables) {
        return readResults(variables).stream()
                .map(entry -> String.valueOf(entry.get("agentId")))
                .filter(id -> id != null && !id.isBlank() && !"null".equals(id))
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }

    public static List<ParsedAgentCall> filterPending(
            List<ParsedAgentCall> agentCalls, WorkflowRuntimeVariables variables) {
        if (agentCalls == null || agentCalls.isEmpty()) {
            return List.of();
        }
        Set<String> completed = completedAgentIds(variables);
        List<ParsedAgentCall> pending = new ArrayList<>();
        for (ParsedAgentCall call : agentCalls) {
            if (!completed.contains(call.agentId())) {
                pending.add(call);
            }
        }
        return List.copyOf(pending);
    }

    public static void appendResult(
            WorkflowRuntimeVariables variables,
            String agentId,
            String message,
            String response) {
        List<Map<String, Object>> results = new ArrayList<>(readResults(variables));
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("agentId", agentId);
        entry.put("message", message);
        entry.put("response", response);
        results.removeIf(existing -> agentId.equals(String.valueOf(existing.get("agentId"))));
        results.add(entry);
        writeResults(variables, results);
        variables.set("childWorkflowResult:" + agentId, response);
    }

    public static List<Map<String, Object>> readResults(WorkflowRuntimeVariables variables) {
        String raw = variables.getString(ToolCallPlannerSupport.DEFAULT_AGENT_RESULTS_VARIABLE);
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = MAPPER.readTree(raw.trim());
            if (!root.isArray()) {
                return List.of();
            }
            List<Map<String, Object>> results = new ArrayList<>();
            for (JsonNode entry : root) {
                if (entry.isObject()) {
                    results.add(MAPPER.convertValue(entry, Map.class));
                }
            }
            return results;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private static void writeResults(WorkflowRuntimeVariables variables, List<Map<String, Object>> results) {
        try {
            variables.set(ToolCallPlannerSupport.DEFAULT_AGENT_RESULTS_VARIABLE, MAPPER.writeValueAsString(results));
        } catch (Exception e) {
            throw new IllegalStateException("failed to serialize agentResultsJson", e);
        }
    }
}
