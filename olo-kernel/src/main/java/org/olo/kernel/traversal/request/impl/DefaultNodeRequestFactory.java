package org.olo.kernel.traversal.request.impl;

import org.olo.definition.node.NodeDefinition;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.traversal.input.impl.MessageVariableInputBinder;
import org.olo.kernel.traversal.request.NodeRequestFactory;
import org.olo.spi.node.NodeRequest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DefaultNodeRequestFactory implements NodeRequestFactory {

    @Override
    public NodeRequest create(KernelRuntimeContext context, NodeDefinition node) {
        Map<String, Object> input = new LinkedHashMap<>();
        String message = context.getVariables().getString(MessageVariableInputBinder.MESSAGE_VARIABLE);
        if (message != null) {
            input.put("userQuery", message);
            input.put("text", message);
            input.put("message", message);
        }

        Map<String, Object> configuration = node.getConfiguration() == null
                ? Map.of()
                : Map.copyOf(node.getConfiguration());
        applyInputVariableMappings(context, configuration, input);

        return new NodeRequest(node.getId(), node.getType(), input, configuration);
    }

    @SuppressWarnings("unchecked")
    private static void applyInputVariableMappings(
            KernelRuntimeContext context,
            Map<String, Object> configuration,
            Map<String, Object> input) {
        Object mappings = configuration.get("inputVariableMappings");
        if (!(mappings instanceof List<?> variableNames)) {
            return;
        }
        for (Object variableNameObject : variableNames) {
            if (!(variableNameObject instanceof String variableName) || variableName.isBlank()) {
                continue;
            }
            Object value = context.getVariables().get(variableName);
            if (value != null) {
                input.put(variableName, value);
            }
        }
    }
}
