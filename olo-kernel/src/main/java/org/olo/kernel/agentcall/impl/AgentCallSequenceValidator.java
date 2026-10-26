/* Copyright (c) 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
package org.olo.kernel.agentcall.impl;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.olo.kernel.agentcall.model.*;
import java.util.*;

/** Agent-only planner protocol. An empty or malformed catalog never grants delegation. */
public final class AgentCallSequenceValidator {
    private static final ObjectMapper JSON = JsonMapper.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    private AgentCallSequenceValidator() {}
    public static AgentCallValidationResult validate(String raw, String catalog) {
        if (raw == null || raw.isBlank()) return invalid("agentCalls JSON is blank");
        try {
            JsonNode root = JSON.readTree(raw);
            if (!root.isObject()) return invalid("agentCalls JSON must be one object");
            var fields = root.fieldNames();
            while (fields.hasNext()) {
                String field = fields.next();
                if (!Set.of("agentCalls", "directResponse").contains(field))
                    return invalid(field.equals("toolCalls")
                            ? "Unsupported planner field: toolCalls" : "Unsupported planner field");
            }
            JsonNode calls = root.get("agentCalls"), response = root.get("directResponse");
            if (calls == null || !calls.isArray() || calls.size() > 16)
                return invalid("agentCalls must be an array with at most 16 entries");
            if (response == null || !(response.isNull() || response.isTextual()))
                return invalid("directResponse must be a string or null");
            String text = response.isNull() ? null : response.textValue();
            if (calls.isEmpty()) {
                if (text == null || text.isBlank()) return invalid("Empty agentCalls requires directResponse");
                return AgentCallValidationResult.directResponse(JSON.writeValueAsString(root), text);
            }
            if (text != null) return invalid("directResponse must be null when delegating");
            JsonNode allowed = catalog == null ? null : JSON.readTree(catalog);
            if (allowed == null || !allowed.isArray()) return invalid("availableAgentsJson must be an array");
            var ids = new HashSet<String>();
            for (JsonNode entry : allowed) {
                JsonNode id = entry.isObject() ? entry.get("agentId") : null;
                if (id == null || !id.isTextual() || id.textValue().isBlank() || !ids.add(id.textValue()))
                    return invalid("Invalid availableAgentsJson catalog");
            }
            var selected = new HashSet<String>();
            var result = new ArrayList<ParsedAgentCall>();
            for (JsonNode call : calls) {
                if (!call.isObject()) return invalid("Each agentCalls entry must be an object");
                var names = call.fieldNames();
                while (names.hasNext()) {
                    String name = names.next();
                    if (!Set.of("agentId", "message").contains(name))
                        return invalid("Unsupported agentCalls field");
                }
                JsonNode id = call.get("agentId"), message = call.get("message");
                if (id == null || !id.isTextual() || !ids.contains(id.textValue()))
                    return invalid("agentId is not in availableAgentsJson allow-list");
                if (!selected.add(id.textValue())) return invalid("Duplicate agentId in agentCalls");
                if (message != null && !message.isNull() && !message.isTextual())
                    return invalid("agentCalls.message must be a string or null");
                result.add(new ParsedAgentCall(id.textValue(), message == null || message.isNull() ? null : message.textValue()));
            }
            return AgentCallValidationResult.calls(JSON.writeValueAsString(root), result);
        } catch (java.io.IOException | IllegalArgumentException invalid) {
            // Parser exception messages can include private model output.
            return invalid("Invalid agentCalls JSON or catalog");
        }
    }
    private static AgentCallValidationResult invalid(String reason) {
        return AgentCallValidationResult.invalid(reason);
    }
}
