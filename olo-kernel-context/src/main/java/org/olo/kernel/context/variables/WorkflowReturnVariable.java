package org.olo.kernel.context.variables;

import org.olo.definition.variable.VariableDefinition;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.kernel.context.exception.KernelContextException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Resolves which workflow variable holds the return message when multiple variables are declared.
 */
public final class WorkflowReturnVariable {

    /** Workflow {@link WorkflowDefinition#getMetadata()} key naming the return variable. */
    public static final String WORKFLOW_METADATA_KEY = "returnVariable";

    /** Variable {@link VariableDefinition#getMetadata()} key used to mark a return variable. */
    public static final String VARIABLE_ROLE_METADATA_KEY = "role";

    /** Variable metadata role value that marks the workflow return variable. */
    public static final String VARIABLE_RETURN_ROLE = "return";

    /** Legacy default variable name when no explicit return variable is configured. */
    public static final String DEFAULT_RETURN_VARIABLE_NAME = "ReturnValue";

    private WorkflowReturnVariable() {
    }

    /**
     * Resolves the return variable name using, in order:
     * <ol>
     *   <li>{@code metadata.returnVariable} on the workflow</li>
     *   <li>exactly one variable with {@code metadata.role = "return"}</li>
     *   <li>legacy fallback {@link #DEFAULT_RETURN_VARIABLE_NAME} when declared on the graph</li>
     * </ol>
     *
     * @return the variable name, or {@code null} when the graph does not designate a return variable
     */
    /**
     * Reads {@code metadata.returnVariable} when set, without validating that the variable is declared.
     */
    public static String readConfiguredName(WorkflowDefinition graph) {
        Objects.requireNonNull(graph, "graph");
        return readWorkflowMetadata(graph.getMetadata());
    }

    public static String resolveName(WorkflowDefinition graph) {
        Objects.requireNonNull(graph, "graph");

        String fromWorkflowMetadata = readWorkflowMetadata(graph.getMetadata());
        if (fromWorkflowMetadata != null) {
            return fromWorkflowMetadata;
        }

        List<String> roleMarked = new ArrayList<>();
        for (VariableDefinition variable : graph.getVariables()) {
            if (variable == null || variable.getName() == null || variable.getName().isBlank()) {
                continue;
            }
            if (isReturnRole(variable.getMetadata())) {
                roleMarked.add(variable.getName());
            }
        }
        if (roleMarked.size() == 1) {
            return roleMarked.get(0);
        }
        if (roleMarked.size() > 1) {
            throw new KernelContextException(
                    "multiple variables marked as return (metadata.role=return): " + roleMarked
                            + "; set workflow metadata." + WORKFLOW_METADATA_KEY + " explicitly");
        }

        if (hasVariable(graph, DEFAULT_RETURN_VARIABLE_NAME)) {
            return DEFAULT_RETURN_VARIABLE_NAME;
        }
        return null;
    }

    private static String readWorkflowMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        Object value = metadata.get(WORKFLOW_METADATA_KEY);
        if (value instanceof String name && !name.isBlank()) {
            return name.trim();
        }
        return null;
    }

    private static boolean isReturnRole(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return false;
        }
        Object role = metadata.get(VARIABLE_ROLE_METADATA_KEY);
        return role instanceof String text && VARIABLE_RETURN_ROLE.equalsIgnoreCase(text.trim());
    }

    private static boolean hasVariable(WorkflowDefinition graph, String variableName) {
        for (VariableDefinition variable : graph.getVariables()) {
            if (variable != null && variableName.equals(variable.getName())) {
                return true;
            }
        }
        return false;
    }
}
