/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.traversal;

import org.junit.jupiter.api.Test;
import org.olo.bootstrap.OloBootstrap;
import org.olo.bootstrap.registry.WorkflowDefinitionRegistry;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.input.model.Context;
import org.olo.input.model.InputItem;
import org.olo.input.model.InputType;
import org.olo.input.model.Storage;
import org.olo.input.model.StorageMode;
import org.olo.input.model.WorkflowInput;
import org.olo.kernel.context.KernelContextBuildRequest;
import org.olo.kernel.context.KernelContextBuilder;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.context.variables.WorkflowReturnVariable;
import org.olo.kernel.input.WorkflowInputMessages;
import org.olo.kernel.agent.client.FakeLlmClient;
import org.olo.kernel.traversal.factory.GraphTraverserFactory;
import org.olo.kernel.traversal.input.impl.MessageVariableInputBinder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GraphTraverserTest {

    private final GraphTraverser traverser = GraphTraverserFactory.withLlmClient(new FakeLlmClient());

    @Test
    void traversesFastPresetAndPopulatesReturnValue() throws Exception {
        KernelRuntimeContext context = buildContext("fast", withMessage("quick question"));
        TraversalResult result = traverser.traverse(context);

        assertThat(result.completed()).isTrue();
        assertThat(result.resolvedLastNodeId()).contains("end");
        assertThat(context.getVariables().getString(MessageVariableInputBinder.MESSAGE_VARIABLE))
                .isEqualTo("quick question");
        assertThat(context.getVariables().getString(WorkflowReturnVariable.DEFAULT_RETURN_VARIABLE_NAME))
                .startsWith("LLM response for:");
    }

    @Test
    void leavesReturnValueUnsetWhenInputMessageMissing() throws Exception {
        KernelRuntimeContext context = buildContext("fast", new WorkflowInput("1.0", List.of(), null, null, null, null));
        TraversalResult result = traverser.traverse(context);

        assertThat(result.completed()).isTrue();
        assertThat(context.getVariables().getString(WorkflowReturnVariable.DEFAULT_RETURN_VARIABLE_NAME))
                .isNull();
        assertThat(WorkflowInputMessages.workflowResult(context.getInput()))
                .isEqualTo(WorkflowInputMessages.MISSING_MESSAGE_RESPONSE);
    }

    private static KernelRuntimeContext buildContext(String queue, WorkflowInput input) throws Exception {
        Path presets = Paths.get("../olo-definition/olo-configuration/default").toAbsolutePath().normalize();
        if (!Files.exists(presets)) {
            throw new org.opentest4j.TestAbortedException("olo-configuration presets not found");
        }

        WorkflowDefinitionRegistry registry = OloBootstrap.load(presets, false);
        WorkflowDefinition source = registry.findById(queue).orElseThrow();
        return KernelContextBuilder.build(KernelContextBuildRequest.of(queue, input, source));
    }

    private static WorkflowInput withMessage(String message) {
        return new WorkflowInput(
                "1.0",
                List.of(new InputItem(
                        "userQuery",
                        "User query",
                        InputType.STRING,
                        new Storage(StorageMode.LOCAL, null, null),
                        message)),
                new Context("default", "", List.of(), List.of(), "session", "run-1", "http://localhost:47080", "corr"),
                null,
                null,
                null);
    }
}
