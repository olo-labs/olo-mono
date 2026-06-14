package org.olo.kernel.traversal;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.input.model.WorkflowInput;
import org.olo.kernel.context.KernelContextBuildRequest;
import org.olo.kernel.context.KernelContextBuilder;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.spi.node.NodeStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class KernelExecutionSnapshotSerializationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    void roundTripsThroughJacksonLikeTemporalPayloadConverter() throws Exception {
        Path presets = Paths.get("../olo-definition/olo-configuration/default").toAbsolutePath().normalize();
        if (!Files.exists(presets)) {
            throw new org.opentest4j.TestAbortedException("olo-configuration presets not found");
        }

        WorkflowDefinition source = org.olo.bootstrap.OloBootstrap.load(presets, false)
                .findByQueue("fast")
                .orElseThrow();
        WorkflowInput input = WorkflowInput.fromJson(
                Files.readString(Paths.get("../olo-workflow-input/samples/minimal-local/workflow-input.json")
                        .toAbsolutePath()
                        .normalize()));
        KernelRuntimeContext context = KernelContextBuilder.build(
                KernelContextBuildRequest.of("fast", input, source));
        KernelExecutionSnapshot snapshot = KernelExecutionSnapshot.fromContext(
                context,
                "agent",
                2,
                KernelExecutionSnapshot.Status.RUNNING,
                "start",
                NodeStatus.COMPLETED,
                "step complete");

        String json = MAPPER.writeValueAsString(snapshot);
        KernelExecutionSnapshot restored = MAPPER.readValue(json, KernelExecutionSnapshot.class);

        assertThat(restored.getQueue()).isEqualTo(snapshot.getQueue());
        assertThat(restored.getNextNodeId()).isEqualTo("agent");
        assertThat(restored.getStep()).isEqualTo(2);
        assertThat(restored.getStatus()).isEqualTo(KernelExecutionSnapshot.Status.RUNNING);
        assertThat(restored.getLastNodeId()).isEqualTo("start");
        assertThat(restored.getLastStatus()).isEqualTo(NodeStatus.COMPLETED);
        assertThat(restored.isNextRequiresDedicatedActivity()).isTrue();
        assertThat(restored.getWorkflowActivityName()).isEqualTo(snapshot.getWorkflowActivityName());
        assertThat(restored.getNextActivityName()).isEqualTo(snapshot.getNextActivityName());
        assertThat(restored.getGraph().getId()).isEqualTo(snapshot.getGraph().getId());
        assertThat(restored.toContext().getVariableMap()).isEqualTo(snapshot.toContext().getVariableMap());
    }

    @Test
    void roundTripsDynamicGraphCreationPreset() throws Exception {
        Path workflowJson = Paths.get("../olo-definition/olo-configuration/current-active/dynamic-graph-creation.json")
                .toAbsolutePath()
                .normalize();
        if (!Files.exists(workflowJson)) {
            throw new org.opentest4j.TestAbortedException("dynamic-graph-creation preset not found");
        }

        WorkflowDefinition graph = new org.olo.definition.serializer.JsonWorkflowSerializer()
                .deserialize(Files.readString(workflowJson));
        WorkflowInput input = WorkflowInput.fromJson(
                Files.readString(Paths.get("../olo-workflow-input/samples/minimal-local/workflow-input.json")
                        .toAbsolutePath()
                        .normalize()));
        KernelRuntimeContext context = KernelContextBuilder.build(
                KernelContextBuildRequest.of("dynamic-graph-creation", input, graph));
        KernelExecutionSnapshot snapshot = KernelExecutionSnapshot.fromContext(context);

        String json = MAPPER.writeValueAsString(snapshot);
        KernelExecutionSnapshot restored = MAPPER.readValue(json, KernelExecutionSnapshot.class);

        assertThat(restored.getGraph().getId()).isEqualTo("dynamic-graph-creation");
        assertThat(restored.getGraphJson()).contains("tool_requirements");
        assertThat(restored.isNextRequiresDedicatedActivity()).isTrue();
        assertThat(restored.getWorkflowActivityName())
                .isEqualTo("dynamic-graph-creation:Dynamic Graph Creation");
        assertThat(restored.getNextActivityName()).isEqualTo("start:Start");
    }
}
