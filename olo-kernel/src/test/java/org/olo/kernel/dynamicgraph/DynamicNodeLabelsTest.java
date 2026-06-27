package org.olo.kernel.dynamicgraph;

import org.junit.jupiter.api.Test;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicNodeLabelsTest {

    @Test
    void prefixesLabelOnce() {
        assertThat(DynamicNodeLabels.prefixed("Calculator")).isEqualTo("Dyn-Calculator");
        assertThat(DynamicNodeLabels.prefixed("Dyn-Calculator")).isEqualTo("Dyn-Calculator");
    }

    @Test
    void appliesDynamicLabelToToolNodeWithoutLabel() {
        NodeDefinition node = NodeDefinition.builder()
                .id("dyn-tool")
                .type(NodeType.TOOL.name())
                .putConfiguration("toolId", "olo-core:cpu-usage")
                .build();

        NodeDefinition labeled = DynamicNodeLabels.withDynamicLabel(node, null);

        assertThat(labeled.getLabel()).isEqualTo("Dyn-cpu-usage");
    }
}
