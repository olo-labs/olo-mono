package org.olo.definition.serializer;

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
                ValidationTestFixtures.node("llm1", NodeType.MODEL)
                    .subtype("CHAT")
                    .build())
            .build();

    String json = serializer.serialize(original);
    assertThat(json).contains("\"id\" : \"stock-analysis\"");
    assertThat(json).contains("\"type\" : \"MODEL\"");

    WorkflowDefinition restored = serializer.deserialize(json);
    assertThat(restored).isEqualTo(original);
  }

  @Test
  void roundTripsWorkflowRoleAndDescriptions() throws Exception {
    WorkflowDefinition original =
        WorkflowBuilder.create("Research Agent")
            .id("research-agent")
            .role("agent")
            .shortDescription("Web and document research with citations")
            .longDescription(
                "Performs web, news and document research, summarizes findings and produces citations.")
            .capability(ValidationTestFixtures.minimalCapability())
            .inputNode("input")
            .outputNode("output")
            .connect("input", "output")
            .build();

    String json = serializer.serialize(original);
    assertThat(json).contains("\"role\" : \"agent\"");
    assertThat(json).contains("\"shortDescription\" : \"Web and document research with citations\"");

    WorkflowDefinition restored = serializer.deserialize(json);
    assertThat(restored.getRole()).isEqualTo("agent");
    assertThat(restored.getShortDescription()).isEqualTo("Web and document research with citations");
    assertThat(restored.getLongDescription())
        .isEqualTo("Performs web, news and document research, summarizes findings and produces citations.");
    assertThat(restored).isEqualTo(original);
  }
}
