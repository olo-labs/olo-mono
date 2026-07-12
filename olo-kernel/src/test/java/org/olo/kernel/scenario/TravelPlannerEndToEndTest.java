/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.scenario;

import org.junit.jupiter.api.Test;
import org.olo.bootstrap.OloBootstrap;
import org.olo.bootstrap.registry.WorkflowDefinitionRegistry;
import org.olo.core.runtime.ExecutionEngine;
import org.olo.core.tool.CoreToolIds;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.validation.WorkflowValidator;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.context.variables.WorkflowRuntimeVariables;
import org.olo.kernel.toolcall.ToolCallFactories;
import org.olo.kernel.traversal.context.impl.KernelExecutionContextFactory;
import org.olo.kernel.traversal.step.handler.impl.ToolNodeTypeHandler;
import org.olo.input.model.WorkflowInput;
import org.olo.spi.node.NodeStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TravelPlannerEndToEndTest {

    @Test
    void loadsTravelPlannerCollectionAndExecutesScenarioTools() throws Exception {
        Path configurationRoot = resolveConfigurationRoot("travel-planner");
        if (!Files.isDirectory(configurationRoot)) {
            throw new org.opentest4j.TestAbortedException("travel-planner configuration not found: " + configurationRoot);
        }

        WorkflowDefinitionRegistry registry = OloBootstrap.load(configurationRoot, false);
        assertThat(registry.findById("travel-orchestrator")).isPresent();
        assertThat(registry.findById("destination-agent")).isPresent();
        assertThat(registry.findById("itinerary-agent")).isPresent();

        WorkflowDefinition orchestrator = registry.findById("travel-orchestrator").orElseThrow();
        assertThat(WorkflowValidator.validate(orchestrator).valid()).isTrue();

        ExecutionEngine engine = ExecutionEngine.withDefaults();
        ToolNodeTypeHandler handler = new ToolNodeTypeHandler(engine, new KernelExecutionContextFactory());
        KernelRuntimeContext context = new KernelRuntimeContext(
                "travel-orchestrator",
                new WorkflowInput("1.0", List.of(), null, null, null, null),
                orchestrator,
                true,
                WorkflowRuntimeVariables.fromDefinition(orchestrator));
        context.getVariables().set("message", "Plan a weekend trip from London to Paris");

        assertToolSuccess(
                handler,
                context,
                "travel-destinations",
                CoreToolIds.TRAVEL_DESTINATIONS,
                Map.of("region", "europe"));
        assertToolSuccess(
                handler,
                context,
                "travel-offers",
                CoreToolIds.TRAVEL_OFFERS,
                Map.of("origin", "London", "destination", "Paris"));

        var mergeValidation = ToolCallFactories.defaultToolCallSubgraphMerger().validate(
                """
                {
                  "toolCalls": [
                    { "toolId": "olo-core:travel-destinations", "arguments": { "region": "europe" } },
                    { "toolId": "olo-core:travel-offers", "arguments": { "origin": "London", "destination": "Paris" } }
                  ],
                  "directResponse": null
                }
                """,
                """
                [{"toolId":"olo-core:travel-destinations"},{"toolId":"olo-core:travel-offers"}]
                """);
        assertThat(mergeValidation.valid()).isTrue();
        var mergeResult = ToolCallFactories.defaultToolCallSubgraphMerger().merge(
                orchestrator,
                "agent",
                "end",
                mergeValidation.toolCalls());
        assertThat(mergeResult.graph().getNodes().stream().map(node -> node.getType())).contains("TOOL");
    }

    private static void assertToolSuccess(
            ToolNodeTypeHandler handler,
            KernelRuntimeContext context,
            String nodeId,
            String toolId,
            Map<String, Object> arguments) {
        NodeDefinition toolNode = NodeDefinition.builder()
                .id(nodeId)
                .type(NodeType.TOOL.name())
                .putConfiguration("toolId", toolId)
                .putConfiguration("arguments", arguments)
                .build();
        var toolResult = handler.execute(context, toolNode);
        assertThat(toolResult.status()).isEqualTo(NodeStatus.COMPLETED);
        assertThat(toolResult.output()).containsEntry("toolId", toolId);
    }

    private static Path resolveConfigurationRoot(String folder) {
        for (String candidate :
                new String[] {
                    "../olo-definition/olo-configuration/" + folder,
                    "olo-definition/olo-configuration/" + folder,
                    "../olo-configuration/" + folder
                }) {
            Path path = Paths.get(candidate).toAbsolutePath().normalize();
            if (Files.isDirectory(path)) {
                return path;
            }
        }
        return Paths.get("olo-configuration/" + folder).toAbsolutePath();
    }
}
