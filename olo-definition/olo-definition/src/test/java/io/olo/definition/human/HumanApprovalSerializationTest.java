package io.olo.definition.human;

import io.olo.definition.node.NodeDefinition;
import io.olo.definition.node.NodeType;
import io.olo.definition.serializer.JsonWorkflowSerializer;
import io.olo.definition.validation.ValidationTestFixtures;
import io.olo.definition.validation.WorkflowValidator;
import io.olo.definition.workflow.WorkflowBuilder;
import io.olo.definition.workflow.WorkflowDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HumanApprovalSerializationTest {

    private final JsonWorkflowSerializer json = new JsonWorkflowSerializer();

    @Test
    void roundTripsHumanApprovalOnNode() throws Exception {
        WorkflowDefinition workflow = WorkflowBuilder.create("HITL")
                .id("hitl")
                .capability(ValidationTestFixtures.minimalCapability())
                .inputNode("input")
                .humanNode(
                        "trade-approval",
                        HumanApprovalDefinition.builder()
                                .title("Approve trade?")
                                .approvers(List.of("trading-desk"))
                                .build())
                .outputNode("output")
                .connect("input", "trade-approval")
                .connect("trade-approval", "output")
                .build();

        WorkflowValidator.validateOrThrow(workflow);

        WorkflowDefinition restored = json.deserialize(json.serialize(workflow));
        NodeDefinition human = restored.getNodes().stream()
                .filter(n -> "trade-approval".equals(n.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(human.getType()).isEqualTo(NodeType.HUMAN.value());
        assertThat(human.getSubtype()).isEqualTo("APPROVAL");
        assertThat(human.getApproval().getTitle()).isEqualTo("Approve trade?");
        assertThat(human.getApproval().getApprovers()).containsExactly("trading-desk");
        assertThat(restored).isEqualTo(workflow);
    }
}
