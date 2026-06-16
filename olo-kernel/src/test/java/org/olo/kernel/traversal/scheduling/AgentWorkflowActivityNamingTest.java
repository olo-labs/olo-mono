package org.olo.kernel.traversal.scheduling;

import org.junit.jupiter.api.Test;
import org.olo.bootstrap.OloBootstrap;
import org.olo.bootstrap.registry.WorkflowDefinitionRegistry;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.input.model.WorkflowInput;
import org.olo.kernel.context.KernelContextBuildRequest;
import org.olo.kernel.context.KernelContextBuilder;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.traversal.KernelExecutionSnapshot;
import org.olo.spi.node.NodeStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class AgentWorkflowActivityNamingTest {

    @Test
    void agentPresetUsesNodeLabelForDedicatedActivityName() throws Exception {
        Path presets = Paths.get("../olo-definition/olo-configuration/current-active").toAbsolutePath().normalize();
        if (!Files.exists(presets)) {
            throw new org.opentest4j.TestAbortedException("current-active presets not found");
        }

        WorkflowDefinitionRegistry registry = OloBootstrap.load(presets, false, true);
        WorkflowDefinition source = registry.findByQueue("agent").orElseThrow();

        NodeDefinition agentNode = source.getNodes().stream()
                .filter(node -> "agent".equals(node.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(agentNode.getLabel()).isEqualTo("Agent1");
        assertThat(NodeActivityNaming.formatNode(agentNode)).isEqualTo("agent:Agent1");

        WorkflowInput input = WorkflowInput.fromJson(
                Files.readString(Paths.get("../olo-workflow-input/samples/minimal-local/workflow-input.json")
                        .toAbsolutePath()
                        .normalize()));
        KernelRuntimeContext context = KernelContextBuilder.build(
                KernelContextBuildRequest.of("agent", input, source));

        KernelExecutionSnapshot afterStart = KernelExecutionSnapshot.fromContext(
                context,
                "agent",
                1,
                KernelExecutionSnapshot.Status.RUNNING,
                "start",
                NodeStatus.COMPLETED,
                "bound input");

        assertThat(afterStart.getNextActivityName()).isEqualTo("agent:Agent1");
    }
}
