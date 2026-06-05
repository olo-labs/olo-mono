package org.olo.definition.serializer;

import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.validation.ValidationTestFixtures;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowDefinition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsonWorkflowSerializerTest {

  private final JsonWorkflowSerializer serializer = new JsonWorkflowSerializer();

  @Test
  void roundTripsWorkflow() throws Exception {
    WorkflowDefinition original =
        WorkflowBuilder.create("StockAnalysis")
            .id("stock-analysis")
            .capability(ValidationTestFixtures.minimalCapability())
            .addNode(
                NodeDefinition.builder()
                    .id("llm1")
                    .type(NodeType.MODEL)
                    .subtype("CHAT")
                    .build())
            .build();

    String json = serializer.serialize(original);
    assertThat(json).contains("\"id\" : \"stock-analysis\"");
    assertThat(json).contains("\"type\" : \"MODEL\"");

    WorkflowDefinition restored = serializer.deserialize(json);
    assertThat(restored).isEqualTo(original);
  }
}
