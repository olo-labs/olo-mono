package io.olo.definition.error;

import io.olo.definition.node.NodeDefinition;
import io.olo.definition.node.NodeType;
import io.olo.definition.serializer.JsonWorkflowSerializer;
import io.olo.definition.validation.ValidationTestFixtures;
import io.olo.definition.validation.WorkflowValidator;
import io.olo.definition.workflow.WorkflowDefinition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OnFailureSerializationTest {

    private final JsonWorkflowSerializer json = new JsonWorkflowSerializer();

    @Test
    void roundTripsOnFailureOnNode() throws Exception {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("retry-fallback")
                .capability(ValidationTestFixtures.minimalCapability())
                .addNode(NodeDefinition.builder()
                        .id("openai")
                        .type(NodeType.MODEL)
                        .onFailure(OnFailureDefinition.builder()
                                .retry(RetryPolicy.builder().attempts(3).build())
                                .route(ErrorRoute.builder().targetNodeId("fallback-model").build())
                                .build())
                        .build())
                .addNode(NodeDefinition.builder()
                        .id("fallback-model")
                        .type(NodeType.MODEL)
                        .build())
                .build();

        WorkflowValidator.validateOrThrow(workflow);

        WorkflowDefinition restored = json.deserialize(json.serialize(workflow));
        NodeDefinition openai = restored.getNodes().stream()
                .filter(n -> "openai".equals(n.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(openai.getOnFailure()).isNotNull();
        assertThat(openai.getOnFailure().getRetry().getAttempts()).isEqualTo(3);
        assertThat(openai.getOnFailure().getRoute().getTargetNodeId()).isEqualTo("fallback-model");
        assertThat(restored).isEqualTo(workflow);
    }
}
