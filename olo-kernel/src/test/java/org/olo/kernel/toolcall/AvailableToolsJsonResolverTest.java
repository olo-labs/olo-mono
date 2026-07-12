/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.toolcall;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.olo.definition.toolcall.ToolCallPlannerSupport;
import org.olo.kernel.context.variables.WorkflowRuntimeVariables;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AvailableToolsJsonResolverTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void appendToolResultStoresStructuredOutputForSynthesis() throws Exception {
        WorkflowRuntimeVariables variables = WorkflowRuntimeVariables.fromMap(Map.of());
        Map<String, Object> output = Map.of(
                "destinations",
                List.of(Map.of("city", "Paris", "country", "France")),
                "offers",
                List.of(Map.of("packageId", "paris-weekend-001", "totalUsd", 465)));

        AvailableToolsJsonResolver.appendToolResult(
                variables,
                "olo-core:travel-destinations",
                "travel-destinations",
                true,
                "Travel destination catalog (1 entries)",
                output);

        JsonNode results = MAPPER.readTree(variables.getString(ToolCallPlannerSupport.DEFAULT_TOOL_RESULTS_VARIABLE));
        assertThat(results).hasSize(1);
        JsonNode entry = results.get(0);
        assertThat(entry.get("toolId").asText()).isEqualTo("olo-core:travel-destinations");
        assertThat(entry.get("response").asText()).contains("1 entries");
        assertThat(entry.get("output").get("destinations").get(0).get("city").asText()).isEqualTo("Paris");
        assertThat(entry.get("output").get("offers").get(0).get("packageId").asText())
                .isEqualTo("paris-weekend-001");
    }
}
