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

class KernelContextBuilderTest {

    @Test
    void buildsRuntimeContextWithDeserializedInputAndIsolatedGraph() throws Exception {
        Path presets = Paths.get("../olo-definition/olo-configuration/governed").toAbsolutePath().normalize();
        if (!Files.exists(presets)) {
            throw new org.opentest4j.TestAbortedException("olo-configuration presets not found");
        }

        WorkflowDefinitionRegistry registry = OloBootstrap.load(presets, false);
        WorkflowDefinition source = registry.findById("governed-chat").orElseThrow();
        String payload = Files.readString(
                Paths.get("../olo-workflow-input/samples/agent-execution/workflow-input.json")
                        .toAbsolutePath()
                        .normalize());

        KernelRuntimeContext context = KernelContextBuilder.build(
                KernelContextBuildRequest.of("olo-governed", payload, source));

        assertThat(context.getQueue()).isEqualTo("olo-governed");
        assertThat(context.isGraphReady()).isTrue();
        assertThat(context.getInput()).isInstanceOf(WorkflowInput.class);
        assertThat(context.getInput().getRouting().getTransactionType().name()).isEqualTo("AGENT_EXECUTION");
        assertThat(context.getGraph()).isNotSameAs(source);
        assertThat(context.getGraph()).isEqualTo(source);
        assertThat(context.getVariables().has("ReturnValue")).isTrue();
        assertThat(context.getVariables().get("ReturnValue")).isNull();
        assertThat(context.getVariableMap()).containsKey("ReturnValue");
        assertThat(context.getVariableMap().get("ReturnValue")).isNull();
    }
}
