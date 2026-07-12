/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.context;

import org.junit.jupiter.api.Test;
import org.olo.bootstrap.OloBootstrap;
import org.olo.bootstrap.registry.WorkflowDefinitionRegistry;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.input.model.WorkflowInput;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class KernelContextBuilderWorkflowInputTest {

    @Test
    void buildsFromDeserializedWorkflowInputObject() throws Exception {
        Path presets = Paths.get("../olo-definition/olo-configuration/default").toAbsolutePath().normalize();
        if (!Files.exists(presets)) {
            throw new org.opentest4j.TestAbortedException("olo-configuration presets not found");
        }

        WorkflowDefinitionRegistry registry = OloBootstrap.load(presets, false);
        WorkflowDefinition source = registry.findById("agent").orElseThrow();
        String payload = Files.readString(
                Paths.get("../olo-workflow-input/samples/agent-execution/workflow-input.json")
                        .toAbsolutePath()
                        .normalize());
        WorkflowInput input = WorkflowInput.fromJson(payload);

        KernelRuntimeContext context = KernelContextBuilder.build(
                KernelContextBuildRequest.of("oloQueue2", input, source));

        assertThat(context.getInput()).isSameAs(input);
        assertThat(context.isGraphReady()).isTrue();
    }
}
