package org.olo.kernel.traversal.output;

import org.junit.jupiter.api.Test;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.variable.VariableDefinition;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.input.model.WorkflowInput;
import org.olo.kernel.context.KernelContextBuildRequest;
import org.olo.kernel.context.KernelContextBuilder;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.context.output.WorkflowReturnOutput;
import org.olo.kernel.context.variables.WorkflowReturnVariable;
import org.olo.kernel.traversal.input.impl.MessageVariableInputBinder;
import org.olo.kernel.traversal.output.impl.ExecutionOutputApplier;
import org.olo.spi.node.NodeResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionOutputApplierTest {

    private final ExecutionOutputApplier applier = new ExecutionOutputApplier();

    @Test
    void accumulatesOutputsWithoutOverwritingEarlierSlots() {
        KernelRuntimeContext context = contextFor(graphWithReturnVariable(null));
        context.getVariables().set(MessageVariableInputBinder.MESSAGE_VARIABLE, "question");

        applier.apply(context, node("planner", NodeType.PLANNER), NodeResult.completed("plan", Map.of("response", "plan")));
        applier.apply(context, node("writer", NodeType.AGENT), NodeResult.completed("final text", Map.of("response", "final text")));

        assertThat(context.getOutputs().get("planner").asReturnMessage()).isEqualTo("plan");
        assertThat(context.getOutputs().get("writer").asReturnMessage()).isEqualTo("final text");
        assertThat(context.getVariables().getString(WorkflowReturnVariable.DEFAULT_RETURN_VARIABLE_NAME))
                .isEqualTo("final text");
    }

    @Test
    void mirrorsOnlyDesignatedReturnOutputKey() {
        KernelRuntimeContext context = contextFor(graphWithReturnVariable("planner"));
        context.getVariables().set(MessageVariableInputBinder.MESSAGE_VARIABLE, "question");

        applier.apply(context, node("planner", NodeType.PLANNER), NodeResult.completed("plan", Map.of("response", "plan")));
        applier.apply(context, node("writer", NodeType.AGENT), NodeResult.completed("final", Map.of("response", "final")));

        assertThat(context.getOutputs().get("planner").asReturnMessage()).isEqualTo("plan");
        assertThat(context.getOutputs().get("writer").asReturnMessage()).isEqualTo("final");
        assertThat(context.getVariables().getString(WorkflowReturnVariable.DEFAULT_RETURN_VARIABLE_NAME))
                .isEqualTo("plan");
    }

    @Test
    void usesConfiguredOutputAliasAsSlotKey() {
        WorkflowDefinition graph = WorkflowDefinition.builder()
                .id("aliases")
                .variables(List.of(VariableDefinition.builder().name("message").type("string").build()))
                .nodes(List.of(NodeDefinition.builder()
                        .id("research-node")
                        .type(NodeType.AGENT.name())
                        .configuration(Map.of(WorkflowReturnOutput.NODE_OUTPUT_KEY, "research"))
                        .build()))
                .build();
        KernelRuntimeContext context = contextFor(graph);
        context.getVariables().set(MessageVariableInputBinder.MESSAGE_VARIABLE, "question");

        applier.apply(
                context,
                graph.getNodes().getFirst(),
                NodeResult.completed("findings", Map.of("response", "findings")));

        assertThat(context.getOutputs().has("research")).isTrue();
        assertThat(context.getOutputs().has("research-node")).isFalse();
    }

    private static KernelRuntimeContext contextFor(WorkflowDefinition graph) {
        return KernelContextBuilder.build(
                KernelContextBuildRequest.of(graph.getId(), new WorkflowInput("1.0", List.of(), null, null, null, null), graph));
    }

    private static WorkflowDefinition graphWithReturnVariable(String returnOutputKey) {
        WorkflowDefinition.Builder builder = WorkflowDefinition.builder()
                .id("multi")
                .metadata(Map.of(WorkflowReturnVariable.WORKFLOW_METADATA_KEY, "ReturnValue"))
                .variables(List.of(
                        VariableDefinition.builder().name("message").type("string").build(),
                        VariableDefinition.builder().name("ReturnValue").type("string").build()));
        if (returnOutputKey != null) {
            builder.metadata(Map.of(
                    WorkflowReturnVariable.WORKFLOW_METADATA_KEY, "ReturnValue",
                    WorkflowReturnOutput.WORKFLOW_METADATA_KEY, returnOutputKey));
        }
        return builder.build();
    }

    private static NodeDefinition node(String id, NodeType type) {
        return NodeDefinition.builder().id(id).type(type.name()).build();
    }
}
