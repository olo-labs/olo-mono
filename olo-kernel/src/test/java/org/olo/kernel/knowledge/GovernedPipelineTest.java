// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.kernel.knowledge;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.kernel.agent.prompt.impl.WorkflowPromptRenderer;
import org.olo.kernel.context.variables.WorkflowRuntimeVariables;

import java.nio.file.*;
import java.util.*;

class GovernedPipelineTest {
    @Test
    void freshPipelineUsesNoMemoryHooksOrAgentSelectedKnowledgeToolsAndIncludesInputOnce()
            throws Exception {
        var graph =
                new ObjectMapper()
                        .readValue(
                                Files.readString(
                                        Path.of(
                                                "../olo-definition/olo-configuration/governed/governed-chat.json")),
                                WorkflowDefinition.class);
        assertEquals("governed-chat", graph.getId());
        assertEquals(3, graph.getNodes().size());
        assertTrue(graph.getTools().isEmpty());
        assertTrue(graph.getHooks().isEmpty());
        var node =
                graph.getNodes().stream()
                        .filter(n -> n.getId().equals("answer"))
                        .findFirst()
                        .orElseThrow();
        var prompt =
                new WorkflowPromptRenderer()
                        .renderForNode(
                                graph,
                                node,
                                WorkflowRuntimeVariables.fromMap(
                                        Map.of("message", "unique accepted message")));
        assertEquals(1, prompt.split("unique accepted message", -1).length - 1);
        assertEquals(
                org.olo.definition.execution.ExecutionModel.ACTIVITY,
                node.getExecution().getExecutionModel());
    }
}
