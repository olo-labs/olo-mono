/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agent.executor;

import org.junit.jupiter.api.Test;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.definition.workflow.WorkflowReferenceDefinition;
import org.olo.kernel.agent.executor.impl.AgentCallDispatchExecutor;
import org.olo.kernel.agent.executor.impl.ChildWorkflowAgentExecutor;
import org.olo.kernel.agent.executor.impl.HumanAgentExecutor;
import org.olo.kernel.agent.executor.impl.LocalLlmAgentExecutor;
import org.olo.kernel.agent.executor.impl.RemoteAgentExecutor;
import org.olo.kernel.agent.client.FakeLlmClient;
import org.olo.kernel.agent.client.LlmClient;
import org.olo.kernel.agent.impl.DefaultLlmInvocationService;
import org.olo.kernel.agent.model.impl.WorkflowModelProviderResolver;
import org.olo.kernel.agent.prompt.impl.WorkflowPromptRenderer;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.context.variables.WorkflowRuntimeVariables;
import org.olo.input.model.WorkflowInput;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AgentExecutorRegistryTest {

    @Test
    void selectsChildWorkflowForExternalAgentReference() {
        AgentExecutorRegistry registry = defaultRegistry();
        NodeDefinition agent = NodeDefinition.builder()
                .id("agent")
                .type(NodeType.AGENT.name())
                .executionKind(org.olo.definition.execution.ExecutionKind.SUBWORKFLOW)
                .executionModel(ExecutionModel.CHILD_WORKFLOW)
                .workflow(WorkflowReferenceDefinition.builder()
                        .workflowId("literature-agent")
                        .version("1.0.0")
                        .build())
                .build();

        assertThat(registry.resolve(orchestratorContext(), agent)).isInstanceOf(ChildWorkflowAgentExecutor.class);
    }

    @Test
    void selectsLocalLlmForSelfReferencingAgentNode() {
        AgentExecutorRegistry registry = defaultRegistry();
        NodeDefinition agent = NodeDefinition.builder()
                .id("agent")
                .type(NodeType.AGENT.name())
                .executionKind(org.olo.definition.execution.ExecutionKind.SUBWORKFLOW)
                .executionModel(ExecutionModel.CHILD_WORKFLOW)
                .workflow(WorkflowReferenceDefinition.builder()
                        .workflowId("literature-agent")
                        .version("1.0.0")
                        .build())
                .build();

        assertThat(registry.resolve(literatureAgentContext(), agent)).isInstanceOf(LocalLlmAgentExecutor.class);
    }

    @Test
    void registryIncludesFutureExecutors() {
        AgentExecutorRegistry registry = defaultRegistry();

        assertThat(registry.resolve(null, NodeDefinition.builder().id("a").type(NodeType.AGENT.name()).build()))
                .isInstanceOf(LocalLlmAgentExecutor.class);
    }

    private static KernelRuntimeContext orchestratorContext() {
        WorkflowDefinition graph = WorkflowBuilder.create("Research Orchestrator")
                .id("research-orchestrator")
                .queue("oloQueue2")
                .build();
        return runtimeContext(graph);
    }

    private static KernelRuntimeContext literatureAgentContext() {
        WorkflowDefinition graph = WorkflowBuilder.create("Literature Agent")
                .id("literature-agent")
                .queue("oloQueue2")
                .build();
        return runtimeContext(graph);
    }

    private static KernelRuntimeContext runtimeContext(WorkflowDefinition graph) {
        return new KernelRuntimeContext(
                graph.getQueue() != null ? graph.getQueue() : "oloQueue2",
                new WorkflowInput("1.0", List.of(), null, null, null, null),
                graph,
                true,
                WorkflowRuntimeVariables.fromDefinition(graph));
    }

    private static AgentExecutorRegistry defaultRegistry() {
        return new AgentExecutorRegistry(List.of(
                new RemoteAgentExecutor(),
                new HumanAgentExecutor(),
                new AgentCallDispatchExecutor(),
                new ChildWorkflowAgentExecutor(new org.olo.kernel.childworkflow.DefaultChildWorkflowCoordinator()),
                new LocalLlmAgentExecutor(llmService(new FakeLlmClient()))));
    }

    private static org.olo.kernel.agent.LlmInvocationService llmService(LlmClient client) {
        return new DefaultLlmInvocationService(
                new WorkflowPromptRenderer(), new WorkflowModelProviderResolver(), client);
    }
}
