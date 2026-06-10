package org.olo.kernel.input;

import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.context.variables.WorkflowReturnVariable;
import org.olo.kernel.context.variables.WorkflowRuntimeVariables;

import java.util.Objects;

/**
 * Resolves the workflow return message from the graph-designated return variable.
 */
public final class WorkflowReturnResolver {

    private WorkflowReturnResolver() {
    }

    public static String resolve(KernelRuntimeContext context) {
        return resolveDetails(context).message();
    }

    /**
     * Returns the workflow return message from the designated return variable.
     * When {@code metadata.returnVariable} is set, the variable must appear in the runtime variable map
     * with a non-blank value; otherwise {@link WorkflowInputMessages#MISSING_MESSAGE_RESPONSE} is returned.
     * When no return variable is configured, falls back to the invocation input message.
     */
    public static WorkflowReturnResolution resolveDetails(KernelRuntimeContext context) {
        Objects.requireNonNull(context, "context");
        WorkflowRuntimeVariables variables = context.getVariables();

        String configuredReturnVariable = WorkflowReturnVariable.readConfiguredName(context.getGraph());
        if (configuredReturnVariable != null) {
            return resolveFromReturnVariable(configuredReturnVariable, variables);
        }

        String returnVariableName = WorkflowReturnVariable.resolveName(context.getGraph());
        if (returnVariableName == null) {
            String message = WorkflowInputMessages.workflowResult(context.getInput());
            return new WorkflowReturnResolution(
                    null,
                    null,
                    message,
                    WorkflowInputMessages.MISSING_MESSAGE_RESPONSE.equals(message));
        }

        Object rawValue = variables.get(returnVariableName);
        String returnValue = variables.getString(returnVariableName);
        if (returnValue != null) {
            return new WorkflowReturnResolution(returnVariableName, rawValue, returnValue, false);
        }

        String populated = WorkflowInputMessages.workflowResult(context.getInput());
        variables.set(returnVariableName, populated);
        return new WorkflowReturnResolution(
                returnVariableName,
                populated,
                populated,
                WorkflowInputMessages.MISSING_MESSAGE_RESPONSE.equals(populated));
    }

    private static WorkflowReturnResolution resolveFromReturnVariable(
            String name, WorkflowRuntimeVariables variables) {
        if (!variables.has(name)) {
            return adminFallback(name, null);
        }
        Object rawValue = variables.get(name);
        String returnValue = variables.getString(name);
        if (returnValue != null) {
            return new WorkflowReturnResolution(name, rawValue, returnValue, false);
        }
        return adminFallback(name, rawValue);
    }

    private static WorkflowReturnResolution adminFallback(String name, Object rawValue) {
        return new WorkflowReturnResolution(
                name,
                rawValue,
                WorkflowInputMessages.MISSING_MESSAGE_RESPONSE,
                true);
    }
}
