/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.traversal.strategy;

import org.junit.jupiter.api.Test;
import org.olo.definition.edge.EdgeDefinition;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.input.model.WorkflowInput;
import org.olo.kernel.context.KernelContextBuildRequest;
import org.olo.kernel.context.KernelContextBuilder;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.graph.index.impl.DefaultGraphIndex;
import org.olo.kernel.traversal.strategy.impl.ChildWorkflowExecutionStrategy;
import org.olo.kernel.traversal.strategy.impl.ConditionalExecutionStrategy;
import org.olo.kernel.traversal.strategy.impl.LinearExecutionStrategy;
import org.olo.kernel.traversal.strategy.impl.ParallelExecutionStrategy;
import org.olo.spi.node.NodeResult;
import org.olo.spi.node.NodeStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionStrategyRegistryTest {

    private final ExecutionStrategyRegistry registry = new ExecutionStrategyRegistry(List.of(
            new ParallelExecutionStrategy(),
            new ConditionalExecutionStrategy(),
            new ChildWorkflowExecutionStrategy(),
            new LinearExecutionStrategy()));

    @Test
    void linearChainUsesLinearStrategy() {
        WorkflowDefinition graph = WorkflowDefinition.builder()
                .id("linear")
                .nodes(List.of(
                        node("start", NodeType.START),
                        node("planner", NodeType.PLANNER),
                        node("end", NodeType.END)))
                .edges(List.of(
                        edge("start", "planner"),
                        edge("planner", "end")))
                .build();
        KernelRuntimeContext context = contextFor(graph);
        DefaultGraphIndex index = new DefaultGraphIndex(graph);
        NodeDefinition planner = index.findNode("planner").orElseThrow();
        ExecutionDecision decision = registry.decide(new ExecutionStrategyRequest(
                context, index, planner, NodeResult.completed("ok", Map.of()), 1));

        assertThat(decision.kind()).isEqualTo(ExecutionDecision.Kind.LINEAR);
        assertThat(decision.strategyName()).isEqualTo(LinearExecutionStrategy.STRATEGY_NAME);
        assertThat(decision.nextNodeId()).contains("end");
    }

    @Test
    void parallelNodeProducesForkDecision() {
        WorkflowDefinition graph = WorkflowDefinition.builder()
                .id("parallel")
                .nodes(List.of(
                        node("parallel", NodeType.PARALLEL),
                        node("a", NodeType.TOOL),
                        node("b", NodeType.TOOL),
                        node("join", NodeType.AGENT),
                        node("end", NodeType.END)))
                .edges(List.of(
                        edge("parallel", "a"),
                        edge("parallel", "b"),
                        edge("a", "join"),
                        edge("b", "join"),
                        edge("join", "end")))
                .build();
        KernelRuntimeContext context = contextFor(graph);
        DefaultGraphIndex index = new DefaultGraphIndex(graph);
        NodeDefinition parallel = index.findNode("parallel").orElseThrow();

        ExecutionDecision decision = registry.decide(new ExecutionStrategyRequest(
                context, index, parallel, NodeResult.completed("ok", Map.of()), 1));

        assertThat(decision.kind()).isEqualTo(ExecutionDecision.Kind.PARALLEL_FORK);
        assertThat(decision.strategyName()).isEqualTo(ParallelExecutionStrategy.STRATEGY_NAME);
        assertThat(decision.branchEntryNodeIds()).containsExactly("a", "b");
        assertThat(decision.joinNodeId()).contains("join");
    }

    private static NodeDefinition node(String id, NodeType type) {
        return NodeDefinition.builder().id(id).type(type.name()).build();
    }

    private static EdgeDefinition edge(String source, String target) {
        return EdgeDefinition.builder().sourceNodeId(source).targetNodeId(target).build();
    }

    private static KernelRuntimeContext contextFor(WorkflowDefinition graph) {
        return KernelContextBuilder.build(
                KernelContextBuildRequest.of(graph.getId(), new WorkflowInput("1.0", List.of(), null, null, null, null), graph));
    }
}
