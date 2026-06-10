package org.olo.kernel;

import org.junit.jupiter.api.Test;
import org.olo.bootstrap.OloBootstrap;
import org.olo.bootstrap.registry.WorkflowDefinitionRegistry;
import org.olo.kernel.input.WorkflowInputMessages;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class KernelEntryPointTest {

    @Test
    void executesQueueThroughContextBuilder() throws Exception {
        Path presets = Paths.get("../olo-configuration/default").toAbsolutePath().normalize();
        if (!Files.exists(presets)) {
            throw new org.opentest4j.TestAbortedException("olo-configuration presets not found");
        }

        WorkflowDefinitionRegistry registry = OloBootstrap.load(presets, false);
        String payload = Files.readString(
                Paths.get("../olo-workflow-input/samples/minimal-local/workflow-input.json")
                        .toAbsolutePath()
                        .normalize());

        String result = KernelEntryPoint.execute("agent", payload, registry);

        assertThat(result).isEqualTo(WorkflowInputMessages.MISSING_MESSAGE_RESPONSE);
    }
}
