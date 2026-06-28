package org.olo.kernel.dynamicgraph;

import org.junit.jupiter.api.Test;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicNodeLabelsTest {

    @Test
    void prefixesToolAndAgentLabels() {
        assertThat(DynamicNodeLabels.prefixedTool("Calculator")).isEqualTo("Dyn-Tool Calculator");
        assertThat(DynamicNodeLabels.prefixedAgent("Agent dispatch")).isEqualTo("Dyn-Agent Agent dispatch");
    }

    @Test
    void withDynamicLabelUsesCategoryPrefix() {
        NodeDefinition tool = NodeDefinition.builder()
                .id("dyn-tool")
                .type(NodeType.TOOL.name())
                .label("Research Literature")
                .build();
        NodeDefinition agent = NodeDefinition.builder()
                .id("dyn-agent")
                .type(NodeType.AGENT.name())
                .label("Tool synthesis")
                .build();

        assertThat(DynamicNodeLabels.withDynamicLabel(tool, null).getLabel())
                .isEqualTo("Dyn-Tool Research Literature");
        assertThat(DynamicNodeLabels.withDynamicLabel(agent, null).getLabel())
                .isEqualTo("Dyn-Agent Tool synthesis");
    }
}
