package io.olo.definition.node;

import io.olo.definition.serializer.JsonWorkflowSerializer;
import io.olo.definition.validation.WorkflowValidator;
import io.olo.definition.workflow.WorkflowBuilder;
import io.olo.definition.workflow.WorkflowDefinition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NodeDefinitionSerializationTest {

    private final JsonWorkflowSerializer json = new JsonWorkflowSerializer();

    @Test
    void roundTripsVersionAndRoutersOnNode() throws Exception {
        WorkflowDefinition workflow = WorkflowBuilder.create("Router Test")
                .id("router-test")
                .inputNode("input")
                .addNode(NodeDefinition.builder()
                        .id("router")
                        .type(NodeType.ROUTER)
                        .version("2.1.0")
                        .addRouter(NodeRouterDefinition.builder()
                                .id("route-a")
                                .targetPort("a")
                                .targetNodeId("output")
                                .providerId("openai-default")
                                .match(java.util.Map.of("priority", 1))
                                .build())
                        .build())
                .outputNode("output")
                .connect("input", "router")
                .connect("router", "output")
                .build();

        WorkflowValidator.validateOrThrow(workflow);

        WorkflowDefinition restored = json.deserialize(json.serialize(workflow));
        assertThat(restored).isEqualTo(workflow);

        NodeDefinition router = restored.getNodes().stream()
                .filter(n -> "router".equals(n.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(router.getVersion()).isEqualTo("2.1.0");
        assertThat(router.getRouters()).hasSize(1);
        assertThat(router.getRouters().get(0).getTargetPort()).isEqualTo("a");
    }
}
