package io.olo.definition.parallel;

import io.olo.definition.node.NodeDefinition;
import io.olo.definition.node.NodeType;
import io.olo.definition.serializer.JsonWorkflowSerializer;
import io.olo.definition.validation.ValidationTestFixtures;
import io.olo.definition.validation.WorkflowValidator;
import io.olo.definition.workflow.WorkflowDefinition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JoinDefinitionTest {

    private final JsonWorkflowSerializer json = new JsonWorkflowSerializer();

    @Test
    void roundTripsJoinOnParallelNode() throws Exception {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("parallel")
                .capability(ValidationTestFixtures.minimalCapability())
                .addNode(NodeDefinition.builder()
                        .id("fan-out")
                        .type(NodeType.PARALLEL)
                        .join(JoinDefinition.builder().strategy(JoinStrategy.ALL).build())
                        .build())
                .build();

        WorkflowValidator.validateOrThrow(workflow);
        WorkflowDefinition restored = json.deserialize(json.serialize(workflow));
        assertThat(restored.getNodes().get(0).getJoin().getStrategy()).isEqualTo(JoinStrategy.ALL);
    }

    @Test
    void rejectsParallelWithoutJoin() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("bad")
                .capability(ValidationTestFixtures.minimalCapability())
                .addNode(NodeDefinition.builder().id("fan-out").type(NodeType.PARALLEL).build())
                .build();

        assertThat(WorkflowValidator.validate(workflow).valid()).isFalse();
    }
}
