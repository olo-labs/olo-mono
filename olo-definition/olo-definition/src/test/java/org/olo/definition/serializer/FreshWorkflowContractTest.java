/* Copyright (c) 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
package org.olo.definition.serializer;

import org.junit.jupiter.api.Test;
import org.olo.definition.workflow.WorkflowDefinition;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class FreshWorkflowContractTest {
    @Test void rejectsRetiredFieldsEvenWhenEmptyThroughEveryJsonBoundary() {
        for (var field : new String[]{"tools", "toolCalls", "tool_requirements", "toolCallPlanner"}) {
            String json = "{\"id\":\"fresh\",\"metadata\":{\"" + field + "\":[]}}";
            var serializer = new JsonWorkflowSerializer();
            assertThrows(IllegalArgumentException.class, () -> serializer.deserialize(json));
            assertThrows(IllegalArgumentException.class, () -> serializer.deserialize(json.getBytes(StandardCharsets.UTF_8)));
            assertThrows(IllegalArgumentException.class, () -> serializer.deserialize(
                    new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))));
            assertThrows(IllegalArgumentException.class, () -> serializer.getMapper().readValue(json, WorkflowDefinition.class));
        }
    }

    @Test void customMapperCannotBypass() {
        assertThrows(IllegalArgumentException.class, () -> new JsonWorkflowSerializer(new com.fasterxml.jackson.databind.ObjectMapper())
                .deserialize("{\"id\":\"fresh\",\"tools\":[]}"));
    }

    @Test void yamlCannotBypassTheFreshContract() {
        var failure = assertThrows(IllegalArgumentException.class, () -> new YamlWorkflowSerializer()
                .deserialize("id: fresh\nnodes:\n  - type: TOOL\n"));
        assertTrue(failure.getMessage().contains("TOOL"));
    }

    @Test void namesTheFieldWithoutEchoingPrivateValues() {
        var failure = assertThrows(IllegalArgumentException.class, () -> new JsonWorkflowSerializer()
                .deserialize("{\"id\":\"private-id\",\"tools\":[\"private-secret\"]}"));
        assertEquals("Unsupported workflow field: tools", failure.getMessage());
    }

    @Test void rejectsRetiredMacroButPreservesNewAgentConfiguration() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> new JsonWorkflowSerializer()
                .deserialize("{\"id\":\"fresh\",\"metadata\":{\"prompt\":\"{availableToolsJson}\"}}"));
        var serializer = new JsonWorkflowSerializer();
        var workflow = serializer.deserialize("{\"id\":\"fresh\",\"nodes\":[{\"id\":\"a\",\"type\":\"AGENT\","
                + "\"configuration\":{\"agentCallPlanner\":true,\"promptTemplate\":\"{availableAgentsJson}\",\"maxToolRounds\":0}}]}");
        assertEquals("fresh", serializer.deserialize(serializer.serialize(workflow)).getId());
    }
}
