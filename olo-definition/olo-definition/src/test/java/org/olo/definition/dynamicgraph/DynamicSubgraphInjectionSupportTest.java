package org.olo.definition.dynamicgraph;

import org.junit.jupiter.api.Test;
import org.olo.definition.validation.WorkflowValidator;
import org.olo.definition.workflow.WorkflowDefinition;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicSubgraphInjectionSupportTest {

    @Test
    void loadsToolCallInjectionDocumentForBuilder() throws Exception {
        Path source = Path.of("../olo-configuration/current-active/tool-call-agent-agent.json")
                .toAbsolutePath()
                .normalize();
        if (!Files.exists(source)) {
            throw new org.opentest4j.TestAbortedException("tool-call injection sample not found");
        }

        String raw = Files.readString(source);
        assertThat(DynamicSubgraphInjectionSupport.isInjectionDocument(raw)).isTrue();

        WorkflowDefinition workflow = DynamicSubgraphInjectionSupport.loadBuilderWorkflow(raw);

        assertThat(workflow.getId()).isEqualTo("tool-call-agent");
        assertThat(workflow.getQueue()).isEqualTo("tool-call-agent");
        assertThat(workflow.isDefault()).isFalse();
        assertThat(workflow.getNodes().stream().map(node -> node.getId()))
                .contains("tool-dyn-9943577398400-step-0", "tool-dyn-9943577398400-tool-synthesis");
        assertThat(WorkflowValidator.validate(workflow).valid()).isTrue();
        assertThat(workflow.getMetadata()).containsKey(DynamicSubgraphInjectionSupport.METADATA_INJECTED_SUBGRAPH);
        assertThat(workflow.getNodes().stream()
                        .filter(node -> node.getId().endsWith("tool-synthesis"))
                        .findFirst()
                        .orElseThrow()
                        .getConfiguration())
                .containsEntry(ToolSynthesisSupport.CONFIG_TOOL_SYNTHESIS, true);
    }
}
