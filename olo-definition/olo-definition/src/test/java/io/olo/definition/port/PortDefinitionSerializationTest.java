package io.olo.definition.port;

import io.olo.definition.node.NodeDefinition;
import io.olo.definition.node.NodeType;
import io.olo.definition.serializer.JsonWorkflowSerializer;
import io.olo.definition.capability.CapabilityDefinition;
import io.olo.definition.workflow.WorkflowDefinition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PortDefinitionSerializationTest {

    private final JsonWorkflowSerializer json = new JsonWorkflowSerializer();

    @Test
    void roundTripsInputsAndOutputsOnNode() throws Exception {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("ports")
                .capability(CapabilityDefinition.builder()
                        .name("Ports")
                        .description("Port round-trip test")
                        .addInput("stocks")
                        .addOutput("stockList")
                        .build())
                .addNode(NodeDefinition.builder()
                        .id("tool-b")
                        .type(NodeType.TOOL)
                        .addInput(PortDefinition.builder().name("stocks").schema("Stock[]").build())
                        .addOutput(PortDefinition.builder().name("stockList").schema("Stock[]").build())
                        .build())
                .build();

        WorkflowDefinition restored = json.deserialize(json.serialize(workflow));
        NodeDefinition tool = restored.getNodes().get(0);

        assertThat(tool.getInputs()).hasSize(1);
        assertThat(tool.getInputs().get(0).getName()).isEqualTo("stocks");
        assertThat(tool.getInputs().get(0).getSchema()).isEqualTo("Stock[]");
        assertThat(tool.getOutputs()).hasSize(1);
        assertThat(tool.getOutputs().get(0).getName()).isEqualTo("stockList");
    }
}
