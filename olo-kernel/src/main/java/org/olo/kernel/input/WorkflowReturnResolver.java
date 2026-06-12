package org.olo.kernel.input;

import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.context.output.ExecutionOutput;
import org.olo.kernel.context.output.WorkflowReturnOutput;
import org.olo.kernel.context.variables.WorkflowReturnVariable;
import org.olo.kernel.context.variables.WorkflowRuntimeVariables;
import org.olo.kernel.traversal.log.TraversalDiagnostics;

import java.util.Objects;

/**
 * Resolves the workflow return message from a designated execution output slot and/or return variable.
 */
public final class WorkflowReturnResolver {

    private WorkflowReturnResolver() {
    }

    public static String resolve(KernelRuntimeContext context) {
        return resolveDetails(context).message();
    }

    /**
     * Returns the workflow return message using, in order:
     * <ol>
     *   <li>{@code metadata.returnOutputKey} → {@link org.olo.kernel.context.output.ExecutionOutputs}</li>
     *   <li>{@code metadata.returnVariable} → workflow variable map</li>
     *   <li>resolved return variable name / role / {@code ReturnValue}</li>
     *   <li>invocation input fallback</li>
     * </ol>
     */
    public static WorkflowReturnResolution resolveDetails(KernelRuntimeContext context) {
        Objects.requireNonNull(context, "context");
        WorkflowRuntimeVariables variables = context.getVariables();

        String configuredReturnOutputKey = WorkflowReturnOutput.readReturnOutputKey(context.getGraph());
        if (configuredReturnOutputKey != null) {
            WorkflowReturnResolution resolution = resolveFromExecutionOutput(configuredReturnOutputKey, context);
            logResolution("metadata.returnOutputKey", resolution);
            return resolution;
        }

        String configuredReturnVariable = WorkflowReturnVariable.readConfiguredName(context.getGraph());
        if (configuredReturnVariable != null) {
            WorkflowReturnResolution resolution = resolveFromReturnVariable(configuredReturnVariable, variables);
            logResolution("metadata.returnVariable", resolution);
            return resolution;
        }

        String returnOutputKey = context.getOutputs().lastKey().orElse(null);
        if (returnOutputKey != null) {
            WorkflowReturnResolution resolution = resolveFromExecutionOutput(returnOutputKey, context);
            if (resolution.message() != null
                    && !WorkflowInputMessages.MISSING_MESSAGE_RESPONSE.equals(resolution.message())) {
                logResolution("execution-output-last-key", resolution);
                return resolution;
            }
        }

        String returnVariableName = WorkflowReturnVariable.resolveName(context.getGraph());
        if (returnVariableName == null) {
            String message = WorkflowInputMessages.workflowResult(context.getInput());
            WorkflowReturnResolution resolution = new WorkflowReturnResolution(
                    null,
                    null,
                    message,
                    WorkflowInputMessages.MISSING_MESSAGE_RESPONSE.equals(message));
            logResolution("input-fallback-no-return-variable", resolution);
            return resolution;
        }

        Object rawValue = variables.get(returnVariableName);
        String returnValue = variables.getString(returnVariableName);
        if (returnValue != null) {
            WorkflowReturnResolution resolution =
                    new WorkflowReturnResolution(returnVariableName, rawValue, returnValue, false);
            logResolution("return-variable-value", resolution);
            return resolution;
        }

        String populated = WorkflowInputMessages.workflowResult(context.getInput());
        variables.set(returnVariableName, populated);
        WorkflowReturnResolution resolution = new WorkflowReturnResolution(
                returnVariableName,
                populated,
                populated,
                WorkflowInputMessages.MISSING_MESSAGE_RESPONSE.equals(populated));
        logResolution("populate-return-variable-from-input", resolution);
        return resolution;
    }

    private static WorkflowReturnResolution resolveFromExecutionOutput(
            String outputKey, KernelRuntimeContext context) {
        ExecutionOutput output = context.getOutputs().get(outputKey);
        if (output == null) {
            return adminFallback(outputKey, null, "execution output not present for key: " + outputKey);
        }
        String message = output.asReturnMessage();
        if (message != null) {
            return new WorkflowReturnResolution(outputKey, output.value(), message, false);
        }
        return adminFallback(outputKey, output.value(), "execution output present but blank for key: " + outputKey);
    }

    private static WorkflowReturnResolution resolveFromReturnVariable(
            String name, WorkflowRuntimeVariables variables) {
        if (!variables.has(name)) {
            return adminFallback(name, null, "return variable not present in runtime map");
        }
        Object rawValue = variables.get(name);
        String returnValue = variables.getString(name);
        if (returnValue != null) {
            return new WorkflowReturnResolution(name, rawValue, returnValue, false);
        }
        return adminFallback(name, rawValue, "return variable present but blank");
    }

    private static WorkflowReturnResolution adminFallback(String name, Object rawValue, String reason) {
        WorkflowReturnResolution resolution = new WorkflowReturnResolution(
                name,
                rawValue,
                WorkflowInputMessages.MISSING_MESSAGE_RESPONSE,
                true);
        logResolution("admin-fallback: " + reason, resolution);
        return resolution;
    }

    private static void logResolution(String path, WorkflowReturnResolution resolution) {
        TraversalDiagnostics.logReturnResolve(
                path,
                resolution.returnVariableName(),
                resolution.returnVariableValue(),
                resolution.message(),
                resolution.usedAdminFallback());
    }
}
