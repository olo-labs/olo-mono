/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.traversal.step.handler.impl;

import org.junit.jupiter.api.Test;
import org.olo.core.runtime.ExecutionEngine;
import org.olo.core.tool.CoreToolIds;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.context.variables.WorkflowRuntimeVariables;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.input.model.InputItem;
import org.olo.input.model.InputType;
import org.olo.input.model.Metadata;
import org.olo.input.model.Storage;
import org.olo.input.model.StorageMode;
import org.olo.input.model.WorkflowInput;
import org.olo.kernel.traversal.context.impl.KernelExecutionContextFactory;
import org.olo.kernel.traversal.input.impl.MessageVariableInputBinder;
import org.olo.spi.node.NodeStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ToolNodeTypeHandlerTest {

    @Test
    void resolvesCommonToolIdAliases() {
        assertThat(ToolNodeTypeHandler.resolveToolId("vector-search")).isEqualTo(CoreToolIds.WEB_SEARCH);
        assertThat(ToolNodeTypeHandler.resolveToolId("olo-core:log-reader")).isEqualTo(CoreToolIds.LOG_READER);
        assertThat(ToolNodeTypeHandler.resolveToolId("log-reader")).isEqualTo(CoreToolIds.LOG_READER);
    }

    @Test
    void invokesRegisteredToolWithMessageAsQuery() {
        ExecutionEngine engine = ExecutionEngine.withDefaults();
        ToolNodeTypeHandler handler = new ToolNodeTypeHandler(engine, new KernelExecutionContextFactory());
        WorkflowDefinition graph = WorkflowBuilder.create("test").id("test").queue("test").build();
        KernelRuntimeContext context = new KernelRuntimeContext(
                "test",
                new WorkflowInput("1.0", List.of(), null, null, null, null),
                graph,
                true,
                WorkflowRuntimeVariables.fromDefinition(graph));
        context.getVariables().set(MessageVariableInputBinder.MESSAGE_VARIABLE, "latest outage news");

        NodeDefinition node = NodeDefinition.builder()
                .id("search")
                .type(NodeType.TOOL.name())
                .putConfiguration(ToolNodeTypeHandler.CONFIG_TOOL_ID, "vector-search")
                .build();

        var result = handler.execute(context, node);

        assertThat(result.status()).isEqualTo(NodeStatus.COMPLETED);
        assertThat(result.output()).containsEntry("toolId", CoreToolIds.WEB_SEARCH);
        assertThat(result.output()).containsKey("response");
    }

    @Test
    void enrichObservabilityArgumentsUsesDemoWindow() {
        Map<String, Object> args = new java.util.LinkedHashMap<>();
        ToolNodeTypeHandler.enrichObservabilityArguments(CoreToolIds.LOG_READER, args);
        assertThat(args).containsEntry("startTime", "2026-06-14T14:30:00Z");
        assertThat(args).containsEntry("endTime", "2026-06-14T14:31:00Z");
    }

    @Test
    void passesCapabilitySourceFromWorkflowMetadataRagTag() {
        ExecutionEngine engine = ExecutionEngine.withDefaults();
        ToolNodeTypeHandler handler = new ToolNodeTypeHandler(engine, new KernelExecutionContextFactory());
        WorkflowDefinition graph = WorkflowBuilder.create("test").id("test").queue("test").build();
        WorkflowInput input = new WorkflowInput(
                "1.0",
                List.of(new InputItem(
                        "userQuery",
                        "RAG ingest request",
                        InputType.STRING,
                        new Storage(StorageMode.LOCAL, null, null),
                        "{}")),
                null,
                null,
                new Metadata("finance-rag", System.currentTimeMillis()),
                null);
        KernelRuntimeContext context = new KernelRuntimeContext(
                "test", input, graph, true, WorkflowRuntimeVariables.fromDefinition(graph));

        NodeDefinition node = NodeDefinition.builder()
                .id("rag-ingest")
                .type(NodeType.TOOL.name())
                .putConfiguration(ToolNodeTypeHandler.CONFIG_TOOL_ID, CoreToolIds.RAG_INGEST)
                .build();

        var result = handler.execute(context, node);

        assertThat(result.message()).doesNotContain("capabilitySource is required");
    }
}
