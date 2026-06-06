package org.olo.definition.port;

import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.serializer.JsonWorkflowSerializer;
import org.olo.definition.capability.CapabilityDefinition;
import org.olo.definition.validation.ValidationTestFixtures;
import org.olo.definition.workflow.WorkflowDefinition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PortDefinitionSerializationTest {

    private final JsonWorkflowSerializer json = new JsonWorkflowSerializer();

    @Test
    void roundTripsPortsOnNode() throws Exception {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("ports")
                .capability(CapabilityDefinition.builder()
                        .name("Ports")
                        .description("Port round-trip test")
                        .addInput("stocks")
                        .addOutput("stockList")
                        .build())
                .addNode(ValidationTestFixtures.node("tool-b", NodeType.TOOL)
                        .addPort(PortDefinition.inputPort("stocks", "Stock[]"))
                        .addPort(PortDefinition.outputPort("stockList", "Stock[]"))
                        .build())
                .build();

        WorkflowDefinition restored = json.deserialize(json.serialize(workflow));
        NodeDefinition tool = restored.getNodes().get(0);

        assertThat(tool.getPorts()).hasSize(4);
        assertThat(tool.getInputs()).extracting(PortDefinition::getId).contains("stocks", "in");
        assertThat(tool.getOutputs()).extracting(PortDefinition::getId).contains("stockList", "out");
        assertThat(tool.getInputs().stream().filter(p -> "stocks".equals(p.getId())).findFirst())
                .isPresent()
                .get()
                .extracting(PortDefinition::getSchema)
                .isEqualTo("Stock[]");
    }
}
