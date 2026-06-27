package org.olo.definition.configuration.agenttool;

import org.junit.jupiter.api.Test;
import org.olo.definition.serializer.JsonWorkflowSerializer;
import org.olo.definition.toolcall.ToolCallPlannerSupport;
import org.olo.definition.validation.WorkflowValidator;
import org.olo.definition.workflow.WorkflowDefinition;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AgentToolExecutionConfigurationTest {

    private final JsonWorkflowSerializer json = new JsonWorkflowSerializer();

    @Test
    void generatedAgentPresetMatchesProgrammaticDefinition() throws Exception {
        WorkflowDefinition expected = AgentToolExecutionDefinitions.agent();
        Path preset = Path.of("olo-configuration/default/agent.json").toAbsolutePath().normalize();
        if (!preset.toFile().exists()) {
            preset = Path.of("../olo-configuration/default/agent.json").toAbsolutePath().normalize();
        }
        WorkflowDefinition onDisk = json.deserialize(Files.readString(preset));
        assertThat(WorkflowValidator.validate(onDisk).valid()).isTrue();
        assertThat(onDisk.getId()).isEqualTo(expected.getId());
        assertThat(onDisk.getMetadata()).containsKey(ToolCallPlannerSupport.METADATA_DYNAMIC_TOOL_EXECUTION);
        assertThat(onDisk.getVariables().stream().map(v -> v.getName()))
                .contains(
                        ToolCallPlannerSupport.DEFAULT_AVAILABLE_TOOLS_VARIABLE,
                        ToolCallPlannerSupport.DEFAULT_OUTPUT_VARIABLE,
                        ToolCallPlannerSupport.DEFAULT_TOOL_RESULTS_VARIABLE);
    }
}
