package org.olo.kernel.traversal.factory;

import org.olo.core.runtime.ExecutionEngine;
import org.olo.kernel.graph.start.StartNodeResolver;
import org.olo.kernel.graph.start.impl.TypeStartNodeResolver;
import org.olo.kernel.graph.validate.GraphReadinessValidator;
import org.olo.kernel.graph.validate.impl.DefaultGraphReadinessValidator;
import org.olo.kernel.traversal.strategy.ExecutionStrategyRegistry;
import org.olo.kernel.traversal.strategy.impl.ChildWorkflowExecutionStrategy;
import org.olo.kernel.traversal.strategy.impl.ConditionalExecutionStrategy;
import org.olo.kernel.traversal.strategy.impl.LinearExecutionStrategy;
import org.olo.kernel.traversal.strategy.impl.ParallelExecutionStrategy;
import org.olo.kernel.agent.LlmInvocationService;
import org.olo.kernel.agent.client.LlmClient;
import org.olo.kernel.agent.client.impl.OllamaLlmClient;
import org.olo.kernel.agent.executor.AgentExecutor;
import org.olo.kernel.agent.executor.AgentExecutorRegistry;
import org.olo.kernel.agent.executor.impl.ChildWorkflowAgentExecutor;
import org.olo.kernel.agent.executor.impl.HumanAgentExecutor;
import org.olo.kernel.agent.executor.impl.LocalLlmAgentExecutor;
import org.olo.kernel.agent.executor.impl.RemoteAgentExecutor;
import org.olo.kernel.agent.impl.DefaultLlmInvocationService;
import org.olo.kernel.agent.model.ModelProviderResolver;
import org.olo.kernel.agent.model.impl.WorkflowModelProviderResolver;
import org.olo.kernel.agent.prompt.PromptRenderer;
import org.olo.kernel.agent.prompt.impl.WorkflowPromptRenderer;
import org.olo.kernel.traversal.GraphTraverser;
import org.olo.kernel.traversal.context.ExecutionContextFactory;
import org.olo.kernel.traversal.context.impl.KernelExecutionContextFactory;
import org.olo.kernel.traversal.impl.DefaultGraphTraverser;
import org.olo.kernel.traversal.input.WorkflowInputBinder;
import org.olo.kernel.traversal.input.impl.MessageVariableInputBinder;
import org.olo.kernel.traversal.output.NodeOutputApplier;
import org.olo.kernel.traversal.output.impl.ExecutionOutputApplier;
import org.olo.kernel.traversal.request.NodeRequestFactory;
import org.olo.kernel.traversal.request.impl.DefaultNodeRequestFactory;
import org.olo.kernel.traversal.spi.NodeTypeResolver;
import org.olo.kernel.traversal.spi.impl.CoreNodeTypeResolver;
import org.olo.kernel.traversal.step.TraversalStepExecutor;
import org.olo.kernel.traversal.step.handler.NodeTypeHandler;
import org.olo.kernel.traversal.step.handler.impl.AgentNodeTypeHandler;
import org.olo.kernel.traversal.step.handler.impl.EndNodeTypeHandler;
import org.olo.kernel.traversal.step.handler.impl.NodeTypeHandlerRegistry;
import org.olo.kernel.traversal.step.handler.impl.SpiNodeTypeHandler;
import org.olo.kernel.traversal.step.handler.impl.StartNodeTypeHandler;
import org.olo.kernel.traversal.step.impl.DefaultTraversalStepExecutor;

import java.util.List;

public final class GraphTraverserFactory {

    private GraphTraverserFactory() {
    }

    public static GraphTraverser withDefaults() {
        return withLlmClient(new OllamaLlmClient());
    }

    public static GraphTraverser withLlmClient(LlmClient llmClient) {
        PromptRenderer promptRenderer = new WorkflowPromptRenderer();
        ModelProviderResolver modelProviderResolver = new WorkflowModelProviderResolver();
        LlmInvocationService llmInvocationService =
                new DefaultLlmInvocationService(promptRenderer, modelProviderResolver, llmClient);
        return build(llmInvocationService);
    }

    private static GraphTraverser build(LlmInvocationService llmInvocationService) {
        ExecutionEngine executionEngine = ExecutionEngine.withDefaults();
        WorkflowInputBinder inputBinder = new MessageVariableInputBinder();
        StartNodeResolver startNodeResolver = new TypeStartNodeResolver();
        GraphReadinessValidator readinessValidator = new DefaultGraphReadinessValidator(startNodeResolver);
        ExecutionStrategyRegistry executionStrategyRegistry = new ExecutionStrategyRegistry(List.of(
                new ParallelExecutionStrategy(),
                new ConditionalExecutionStrategy(),
                new ChildWorkflowExecutionStrategy(),
                new LinearExecutionStrategy()));
        ExecutionContextFactory executionContextFactory = new KernelExecutionContextFactory();
        NodeRequestFactory nodeRequestFactory = new DefaultNodeRequestFactory();
        NodeTypeResolver nodeTypeResolver = new CoreNodeTypeResolver();
        NodeOutputApplier outputApplier = new ExecutionOutputApplier();
        AgentExecutorRegistry agentExecutorRegistry = defaultAgentExecutorRegistry(llmInvocationService);

        List<NodeTypeHandler> handlers = List.of(
                new StartNodeTypeHandler(inputBinder),
                new AgentNodeTypeHandler(agentExecutorRegistry),
                new EndNodeTypeHandler(),
                new SpiNodeTypeHandler(
                        executionEngine,
                        executionContextFactory,
                        nodeRequestFactory,
                        nodeTypeResolver));
        NodeTypeHandlerRegistry handlerRegistry = new NodeTypeHandlerRegistry(handlers);
        TraversalStepExecutor stepExecutor = new DefaultTraversalStepExecutor(handlerRegistry, outputApplier);

        return new DefaultGraphTraverser(
                startNodeResolver,
                readinessValidator,
                stepExecutor,
                executionStrategyRegistry);
    }

    /**
     * Specific executors first; {@link LocalLlmAgentExecutor} last as fallback for AGENT nodes.
     */
    static AgentExecutorRegistry defaultAgentExecutorRegistry(LlmInvocationService llmInvocationService) {
        List<AgentExecutor> executors = List.of(
                new RemoteAgentExecutor(),
                new HumanAgentExecutor(),
                new ChildWorkflowAgentExecutor(),
                new LocalLlmAgentExecutor(llmInvocationService));
        return new AgentExecutorRegistry(executors);
    }
}
