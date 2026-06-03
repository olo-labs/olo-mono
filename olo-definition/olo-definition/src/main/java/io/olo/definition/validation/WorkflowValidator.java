package io.olo.definition.validation;

import io.olo.definition.edge.EdgeDefinition;
import io.olo.definition.extension.ExtensionDefinition;
import io.olo.definition.model.ModelProviderDefinition;
import io.olo.definition.model.ModelRoutingDefinition;
import io.olo.definition.node.NodeDefinition;
import io.olo.definition.node.NodeRouterDefinition;
import io.olo.definition.variable.VariableDefinition;
import io.olo.definition.workflow.WorkflowDefinition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Structural validation for workflow definitions (no runtime or execution checks).
 */
public final class WorkflowValidator {

    private WorkflowValidator() {
    }

    public static ValidationResult validate(WorkflowDefinition workflow) {
        List<String> errors = new ArrayList<>();
        if (workflow == null) {
            return ValidationResult.failure(List.of("workflow must not be null"));
        }
        if (isBlank(workflow.getId())) {
            errors.add("workflow id is required");
        }

        Set<String> nodeIds = new HashSet<>();
        for (NodeDefinition node : workflow.getNodes()) {
            if (node == null) {
                errors.add("node entry must not be null");
                continue;
            }
            if (isBlank(node.getId())) {
                errors.add("node id is required");
            } else if (!nodeIds.add(node.getId())) {
                errors.add("duplicate node id: " + node.getId());
            }
            if (isBlank(node.getType())) {
                errors.add("node type is required for node: " + node.getId());
            }
        }
        for (NodeDefinition node : workflow.getNodes()) {
            if (node != null) {
                validateNodeRouters(node, nodeIds, errors);
            }
        }

        Set<String> variableNames = new HashSet<>();
        for (VariableDefinition variable : workflow.getVariables()) {
            if (variable == null) {
                errors.add("variable entry must not be null");
                continue;
            }
            if (isBlank(variable.getName())) {
                errors.add("variable name is required");
            } else if (!variableNames.add(variable.getName())) {
                errors.add("duplicate variable name: " + variable.getName());
            }
        }

        Set<String> providerIds = new HashSet<>();
        for (ModelProviderDefinition provider : workflow.getModelProviders()) {
            if (provider == null) {
                errors.add("model provider entry must not be null");
                continue;
            }
            if (isBlank(provider.getId())) {
                errors.add("model provider id is required");
            } else if (!providerIds.add(provider.getId())) {
                errors.add("duplicate model provider id: " + provider.getId());
            }
        }

        for (ModelRoutingDefinition routing : workflow.getModelRouting()) {
            if (routing != null && !isBlank(routing.getDefaultProviderId())) {
                String defaultId = routing.getDefaultProviderId();
                if (!providerIds.isEmpty() && !providerIds.contains(defaultId)) {
                    errors.add("model routing references unknown provider: " + defaultId);
                }
            }
        }

        Set<String> extensionIds = new HashSet<>();
        for (ExtensionDefinition extension : workflow.getExtensions()) {
            if (extension == null) {
                errors.add("extension entry must not be null");
                continue;
            }
            if (isBlank(extension.getId())) {
                errors.add("extension id is required");
            } else if (!extensionIds.add(extension.getId())) {
                errors.add("duplicate extension id: " + extension.getId());
            }
        }

        int edgeIndex = 0;
        for (EdgeDefinition edge : workflow.getEdges()) {
            if (edge == null) {
                errors.add("edge entry must not be null");
                edgeIndex++;
                continue;
            }
            String prefix = "edge[" + edgeIndex + "]: ";
            if (isBlank(edge.getSourceNodeId())) {
                errors.add(prefix + "sourceNodeId is required");
            } else if (!nodeIds.contains(edge.getSourceNodeId())) {
                errors.add(prefix + "unknown source node: " + edge.getSourceNodeId());
            }
            if (isBlank(edge.getTargetNodeId())) {
                errors.add(prefix + "targetNodeId is required");
            } else if (!nodeIds.contains(edge.getTargetNodeId())) {
                errors.add(prefix + "unknown target node: " + edge.getTargetNodeId());
            }
            if (edge.getSourceNodeId() != null
                    && edge.getSourceNodeId().equals(edge.getTargetNodeId())) {
                errors.add(prefix + "self-loop on node: " + edge.getSourceNodeId());
            }
            edgeIndex++;
        }

        return errors.isEmpty() ? ValidationResult.ok() : ValidationResult.failure(errors);
    }

    public static void validateOrThrow(WorkflowDefinition workflow) {
        ValidationResult result = validate(workflow);
        if (!result.valid()) {
            throw new WorkflowValidationException(result.errors());
        }
    }

    private static void validateNodeRouters(
            NodeDefinition node, Set<String> nodeIds, List<String> errors) {
        Set<String> routerIds = new HashSet<>();
        for (NodeRouterDefinition router : node.getRouters()) {
            if (router == null) {
                errors.add("router entry must not be null on node: " + node.getId());
                continue;
            }
            if (!isBlank(router.getId()) && !routerIds.add(router.getId())) {
                errors.add("duplicate router id '" + router.getId() + "' on node: " + node.getId());
            }
            if (!isBlank(router.getTargetNodeId()) && !nodeIds.contains(router.getTargetNodeId())) {
                errors.add(
                        "router on node "
                                + node.getId()
                                + " references unknown target node: "
                                + router.getTargetNodeId());
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
