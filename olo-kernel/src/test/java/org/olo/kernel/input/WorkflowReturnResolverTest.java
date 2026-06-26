package org.olo.kernel.input;

import org.junit.jupiter.api.Test;
import org.olo.bootstrap.OloBootstrap;
import org.olo.bootstrap.registry.WorkflowDefinitionRegistry;
import org.olo.definition.variable.VariableDefinition;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowReturnResolverTest {

    @Test
    void usesAdminMessageWhenConfiguredReturnVariableIsUnset() throws Exception {
        KernelRuntimeContext context = buildAgentContext(withUserQuery("hello agent"));

        String result = WorkflowReturnResolver.resolve(context);

        assertThat(result).isEqualTo(WorkflowInputMessages.MISSING_MESSAGE_RESPONSE);
        assertThat(context.getVariables().getString(WorkflowReturnVariable.DEFAULT_RETURN_VARIABLE_NAME))
                .isNull();
    }

    @Test
    void resolvesReturnValueAfterGraphTraversal() throws Exception {
        KernelRuntimeContext context = buildAgentContext(withUserQuery("hello agent"));
        org.olo.kernel.traversal.factory.GraphTraverserFactory
                .withLlmClient(new org.olo.kernel.agent.client.FakeLlmClient())
                .traverse(context);

        String result = WorkflowReturnResolver.resolve(context);

        assertThat(result).startsWith("LLM response for:");
        assertThat(context.getVariables().getString(WorkflowReturnVariable.DEFAULT_RETURN_VARIABLE_NAME))
                .startsWith("LLM response for:");
    }

    @Test
    void usesAdminMessageWhenConfiguredReturnVariableAndInputAreMissing() throws Exception {
        KernelRuntimeContext context = buildAgentContext(new WorkflowInput("1.0", List.of(), null, null, null, null));

        String result = WorkflowReturnResolver.resolve(context);

        assertThat(result).isEqualTo(WorkflowInputMessages.MISSING_MESSAGE_RESPONSE);
        assertThat(context.getVariables().getString(WorkflowReturnVariable.DEFAULT_RETURN_VARIABLE_NAME))
                .isNull();
    }

    @Test
    void usesAdminMessageWhenConfiguredReturnVariableNotInRuntimeMap() {
        WorkflowDefinition graph = WorkflowDefinition.builder()
                .id("misconfigured")
                .queue("misconfigured")
                .metadata(Map.of(WorkflowReturnVariable.WORKFLOW_METADATA_KEY, "ReturnValue"))
                .variables(List.of(VariableDefinition.builder().name("OtherValue").type("string").build()))
                .build();

        KernelRuntimeContext context = KernelContextBuilder.build(
                KernelContextBuildRequest.of("misconfigured", withUserQuery("hello"), graph));

        assertThat(context.getVariableMap()).doesNotContainKey("ReturnValue");
        assertThat(WorkflowReturnResolver.resolve(context))
                .isEqualTo(WorkflowInputMessages.MISSING_MESSAGE_RESPONSE);
    }

    @Test
    void keepsPresetReturnValueWhenAlreadySet() throws Exception {
        KernelRuntimeContext context = buildAgentContext(withUserQuery("ignored"));
        context.getVariables().set(WorkflowReturnVariable.DEFAULT_RETURN_VARIABLE_NAME, "preset response");

        assertThat(WorkflowReturnResolver.resolve(context)).isEqualTo("preset response");
    }

    private static KernelRuntimeContext buildAgentContext(WorkflowInput input) throws Exception {
        Path presets = Paths.get("../olo-definition/olo-configuration/default").toAbsolutePath().normalize();
        if (!Files.exists(presets)) {
            throw new org.opentest4j.TestAbortedException("olo-configuration presets not found");
        }

        WorkflowDefinitionRegistry registry = OloBootstrap.load(presets, false);
        WorkflowDefinition source = registry.findById("agent").orElseThrow();
        return KernelContextBuilder.build(KernelContextBuildRequest.of("oloQueue2", input, source));
    }

    private static WorkflowInput withUserQuery(String message) {
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
