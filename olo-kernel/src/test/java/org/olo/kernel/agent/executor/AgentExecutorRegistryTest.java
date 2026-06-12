package org.olo.kernel.agent.executor;

import org.junit.jupiter.api.Test;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.workflow.WorkflowReferenceDefinition;
import org.olo.kernel.agent.executor.impl.ChildWorkflowAgentExecutor;
import org.olo.kernel.agent.executor.impl.HumanAgentExecutor;
import org.olo.kernel.agent.executor.impl.LocalLlmAgentExecutor;
import org.olo.kernel.agent.executor.impl.RemoteAgentExecutor;
import org.olo.kernel.agent.client.FakeLlmClient;
import org.olo.kernel.agent.client.LlmClient;
import org.olo.kernel.agent.impl.DefaultLlmInvocationService;
import org.olo.kernel.agent.model.impl.WorkflowModelProviderResolver;
import org.olo.kernel.agent.prompt.impl.WorkflowPromptRenderer;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AgentExecutorRegistryTest {

    @Test
    void selectsLocalLlmForAgentNode() {
        AgentExecutorRegistry registry = defaultRegistry();
        NodeDefinition agent = NodeDefinition.builder()
                .id("agent")
                .type(NodeType.AGENT.name())
                .executionKind(org.olo.definition.execution.ExecutionKind.SUBWORKFLOW)
                .executionModel(ExecutionModel.CHILD_WORKFLOW)
                .workflow(WorkflowReferenceDefinition.builder()
                        .workflowId("agent")
                        .version("1.0.0")
                        .build())
                .build();

        assertThat(registry.resolve(agent)).isInstanceOf(LocalLlmAgentExecutor.class);
    }

    @Test
    void registryIncludesFutureExecutors() {
        AgentExecutorRegistry registry = defaultRegistry();

        assertThat(registry.resolve(NodeDefinition.builder().id("a").type(NodeType.AGENT.name()).build()))
                .isInstanceOf(LocalLlmAgentExecutor.class);
    }

    private static AgentExecutorRegistry defaultRegistry() {
        return new AgentExecutorRegistry(List.of(
                new RemoteAgentExecutor(),
                new HumanAgentExecutor(),
                new ChildWorkflowAgentExecutor(),
                new LocalLlmAgentExecutor(llmService(new FakeLlmClient()))));
    }

    private static org.olo.kernel.agent.LlmInvocationService llmService(LlmClient client) {
        return new DefaultLlmInvocationService(
                new WorkflowPromptRenderer(), new WorkflowModelProviderResolver(), client);
    }
}
