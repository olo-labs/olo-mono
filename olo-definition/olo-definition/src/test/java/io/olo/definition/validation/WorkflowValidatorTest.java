package io.olo.definition.validation;

import io.olo.definition.edge.EdgeDefinition;
import io.olo.definition.workflow.WorkflowBuilder;
import io.olo.definition.workflow.WorkflowDefinition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowValidatorTest {

    @Test
    void acceptsValidWorkflow() {
        WorkflowDefinition workflow = WorkflowBuilder.create("Valid")
                .inputNode("in")
                .outputNode("out")
                .connect("in", "out")
                .build();

        assertThat(WorkflowValidator.validate(workflow).valid()).isTrue();
        WorkflowValidator.validateOrThrow(workflow);
    }

    @Test
    void rejectsUnknownEdgeEndpoints() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("broken")
                .addEdge(EdgeDefinition.builder().sourceNodeId("a").targetNodeId("b").build())
                .build();

        ValidationResult result = WorkflowValidator.validate(workflow);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("unknown source node"));
        assertThat(result.errors()).anyMatch(e -> e.contains("unknown target node"));
    }

    @Test
    void validateOrThrowThrows() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("broken")
                .addEdge(EdgeDefinition.builder().sourceNodeId("a").targetNodeId("b").build())
                .build();

        assertThatThrownBy(() -> WorkflowValidator.validateOrThrow(workflow))
                .isInstanceOf(WorkflowValidationException.class);
    }
}
