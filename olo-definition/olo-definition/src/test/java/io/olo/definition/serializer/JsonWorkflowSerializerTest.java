package io.olo.definition.serializer;

import io.olo.definition.node.NodeDefinition;
import io.olo.definition.node.NodeType;
import io.olo.definition.workflow.WorkflowBuilder;
import io.olo.definition.workflow.WorkflowDefinition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsonWorkflowSerializerTest {

  private final JsonWorkflowSerializer serializer = new JsonWorkflowSerializer();

  @Test
  void roundTripsWorkflow() throws Exception {
    WorkflowDefinition original =
        WorkflowBuilder.create("StockAnalysis")
            .id("stock-analysis")
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
