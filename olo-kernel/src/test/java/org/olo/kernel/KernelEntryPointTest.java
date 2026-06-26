package org.olo.kernel;

import org.junit.jupiter.api.Test;
import org.olo.bootstrap.OloBootstrap;
import org.olo.bootstrap.registry.WorkflowDefinitionRegistry;
import org.olo.kernel.input.WorkflowInputMessages;

import org.olo.input.model.WorkflowInput;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class KernelEntryPointTest {

    @Test
    void executesQueueThroughContextBuilder() throws Exception {
        Path presets = Paths.get("../olo-definition/olo-configuration/default").toAbsolutePath().normalize();
        if (!Files.exists(presets)) {
            throw new org.opentest4j.TestAbortedException("olo-configuration presets not found");
        }

        WorkflowDefinitionRegistry registry = OloBootstrap.load(presets, false);
        WorkflowInput baseInput = WorkflowInput.fromJson(Files.readString(
                Paths.get("../olo-workflow-input/samples/minimal-local/workflow-input.json")
                        .toAbsolutePath()
                        .normalize()));
        WorkflowInput input = baseInput.toBuilder()
                .routing(new org.olo.input.model.Routing(
                        "agent",
                        baseInput.getRouting().getTransactionType(),
                        baseInput.getRouting().getTransactionId(),
                        baseInput.getRouting().getConfigVersion()))
                .build();

        String result = KernelEntryPoint.execute("oloQueue2", input, registry);

        assertThat(result).doesNotContain("child workflow dispatch pending");
        assertThat(result).isNotEqualTo(WorkflowInputMessages.MISSING_MESSAGE_RESPONSE);
    }
}
