/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.simplechatconversations;

import org.junit.jupiter.api.Test;
import org.olo.definition.configuration.scenario.ScenarioConfigurationTestSupport;
import org.olo.definition.designer.StudioDesignerAssertions;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.preset.WorkflowConversationPluginSupport;
import org.olo.definition.serializer.JsonWorkflowSerializer;
import org.olo.definition.workflow.WorkflowDefinition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleChatConversationsConfigurationTest {

    private final JsonWorkflowSerializer json = new JsonWorkflowSerializer();

    @Test
    void onDiskCollectionMatchesDefinitions() throws IOException {
        Path configurationRoot = SimpleChatConversationsPaths.resolveConfigurationRoot();
        for (String workflowId : SimpleChatConversationsDefinitions.workflowIds()) {
            ScenarioConfigurationTestSupport.assertPreset(
                    configurationRoot,
                    workflowId,
                    SimpleChatConversationsDefinitions.workflow(workflowId));
        }
    }

    @Test
    void eachPipelineLoadsChatsStoresChatAndHasNoHumanStep() throws IOException {
        Path configurationRoot = SimpleChatConversationsPaths.resolveConfigurationRoot();
        for (String workflowId : SimpleChatConversationsDefinitions.workflowIds()) {
            WorkflowDefinition workflow = json.deserialize(Files.readString(configurationRoot.resolve(workflowId + ".json")));

            assertThat(workflow.getQueue()).isEqualTo(SimpleChatConversationsDefinitions.QUEUE);
            assertThat(workflow.getNodes().stream().map(NodeDefinition::getType))
                    .containsExactlyInAnyOrder(
                            NodeType.START.name(),
                            NodeType.TOOL.name(),
                            NodeType.AGENT.name(),
                            NodeType.TOOL.name(),
                            NodeType.END.name())
                    .doesNotContain(NodeType.HUMAN.name());
            assertThat(workflow.getNodes().stream().map(NodeDefinition::getId))
                    .contains(
                            WorkflowConversationPluginSupport.CONVERSATION_LOAD_NODE_ID,
                            "agent",
                            WorkflowConversationPluginSupport.CONVERSATION_STORE_NODE_ID);
            assertConversationChain(workflow);
            assertThat(workflow.getTools().stream().map(tool -> tool.getRuntimeBinding().getImplementationId()))
                    .contains(
                            WorkflowConversationPluginSupport.CONVERSATION_LOAD_TOOL_ID,
                            WorkflowConversationPluginSupport.CONVERSATION_STORE_TOOL_ID);
            assertThat(workflow.getVariables().stream().map(variable -> variable.getName()))
                    .contains(
                            WorkflowConversationPluginSupport.CONVERSATION_SUMMARY_VARIABLE,
                            WorkflowConversationPluginSupport.CONVERSATION_HISTORY_VARIABLE);
            assertThat(workflow.getMetadata()).containsEntry("collection", "simple-chat-conversations");
            StudioDesignerAssertions.assertStudioBuildReady(workflow);
        }
    }

    private static void assertConversationChain(WorkflowDefinition workflow) {
        List<String> chain = List.of(
                "start",
                WorkflowConversationPluginSupport.CONVERSATION_LOAD_NODE_ID,
                "agent",
                WorkflowConversationPluginSupport.CONVERSATION_STORE_NODE_ID,
                "end");
        for (int i = 0; i < chain.size() - 1; i++) {
            String source = chain.get(i);
            String target = chain.get(i + 1);
            assertThat(workflow.getEdges()).anyMatch(edge ->
                    source.equals(edge.getSourceNodeId()) && target.equals(edge.getTargetNodeId()));
        }
    }
}
