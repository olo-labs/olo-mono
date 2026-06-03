package io.olo.definition.workflow;

import io.olo.definition.node.NodeDefinition;
import io.olo.definition.node.NodeType;
import io.olo.definition.validation.WorkflowValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowBuilderTest {

    @Test
    void buildsStockAnalysisWorkflow() {
        WorkflowDefinition workflow = WorkflowBuilder.create("Stock Workflow")
                .id("stock-analysis")
                .version("1.0.0")
                .inputNode("request")
                .modelNode("analysis", "CHAT")
                .toolNode("screener")
                .outputNode("response")
                .connect("request", "analysis")
                .connect("analysis", "screener")
                .connect("screener", "response")
                .build();

        assertThat(workflow.getId()).isEqualTo("stock-analysis");
        assertThat(workflow.getName()).isEqualTo("Stock Workflow");
        assertThat(workflow.getNodes()).hasSize(4);
        assertThat(workflow.getEdges()).hasSize(3);
        assertThat(WorkflowValidator.validate(workflow).valid()).isTrue();
    }

    @Test
    void extendsExistingWorkflow() {
        WorkflowDefinition base = WorkflowBuilder.create("Analysis")
                .id("analysis")
                .inputNode("input")
                .modelNode("llm1", "CHAT")
                .outputNode("output")
                .connect("input", "llm1")
                .connect("llm1", "output")
                .build();

        NodeDefinition ragNode = NodeDefinition.builder()
                .id("rag1")
                .type(NodeType.VECTOR_SEARCH)
                .build();

        WorkflowDefinition enhanced = WorkflowBuilder.from(base)
                .addNode(ragNode)
                .connect("input", "rag1")
                .connect("rag1", "llm1")
                .build();

        assertThat(enhanced.getNodes()).hasSize(4);
        assertThat(enhanced.getEdges()).hasSize(4);
        assertThat(enhanced.getId()).isEqualTo("analysis");
    }

    @Test
    void rejectsDuplicateNodeIds() {
        assertThatThrownBy(() -> WorkflowBuilder.create("Dup")
                .inputNode("same")
                .addNode(NodeDefinition.builder().id("same").type(NodeType.TOOL).build())
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate node id");
    }
}
