/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.definition.validation.impl;

import org.olo.definition.edge.EdgeDefinition;
import org.olo.definition.hook.HookCatalog;
import org.olo.definition.hook.HookValidator;
import org.olo.definition.model.ModelProviderDefinition;
import org.olo.definition.model.ModelRoutingDefinition;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.state.EffectiveStateFields;
import org.olo.definition.variable.VariableDefinition;
import org.olo.definition.workflow.WorkflowDefinition;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates workflow structure: identity, nodes, edges, inputs, state, variables, and providers.
 */
final class WorkflowStructureValidator {

    private WorkflowStructureValidator() {
    }

    /** Runs the structural validation pass and populates {@code state} for downstream checks. */
    static void validate(WorkflowDefinition workflow, WorkflowValidationState state, List<String> errors) {
        if (ValidationUtils.isBlank(workflow.getId())) {
            errors.add("workflow id is required");
        }

        state.hookImplementationIds.addAll(HookCatalog.implementationIds(workflow));
        validateNodes(workflow, state, errors);
        populateFieldNames(workflow, state, errors);
        validateNodeSemanticsPass(workflow, state, errors);
        validateInputs(workflow, errors);
        validateStateFields(workflow, errors);
        validateVariables(workflow, errors);
        validateModelProviders(workflow, state, errors);
        validateModelRouting(workflow, state, errors);
        WorkflowRegistryValidator.validate(workflow, errors);
        validateEdges(workflow, state, errors);
        validatePortConnectionCounts(workflow, state, errors);
    }

    private static void validateNodes(WorkflowDefinition workflow, WorkflowValidationState state, List<String> errors) {
        for (NodeDefinition node : workflow.getNodes()) {
            if (node == null) {
                errors.add("node entry must not be null");
                continue;
            }
            if (ValidationUtils.isBlank(node.getId())) {
                errors.add("node id is required");
            } else if (!state.nodeIds.add(node.getId())) {
                errors.add("duplicate node id: " + node.getId());
            } else {
                state.nodesById.put(node.getId(), node);
            }
            if (ValidationUtils.isBlank(node.getType())) {
                errors.add("node type is required for node: " + node.getId());
            }
            if (!ValidationUtils.isBlank(node.getId())) {
                WorkflowPortValidator.validateNodePorts(node, errors);
                WorkflowCapabilityValidator.validateNode(node, errors);
                WorkflowRuntimeValidator.validateNodeBinding(node, errors);
                HookValidator.validateNodeHooks(
                        "workflow " + workflow.getId(),
                        node.getId(),
                        node.getHooks(),
                        state.hookImplementationIds,
                        errors);
            }
        }
    }

    private static void populateFieldNames(
            WorkflowDefinition workflow, WorkflowValidationState state, List<String> errors) {
        state.stateFieldNames.addAll(EffectiveStateFields.names(workflow));
        state.inputFieldNames.addAll(workflow.getInputs().keySet());
        state.parameterFieldNames.addAll(workflow.getParameters().keySet());
        EffectiveStateFields.validateDeclarations(workflow, errors);
    }

    private static void validateNodeSemanticsPass(
            WorkflowDefinition workflow, WorkflowValidationState state, List<String> errors) {
        for (NodeDefinition node : workflow.getNodes()) {
            if (node != null) {
                WorkflowNodeValidator.validateNodeSemantics(
                        workflow.getId(), node, state.nodeIds, state, errors);
            }
        }
    }

    private static void validateInputs(WorkflowDefinition workflow, List<String> errors) {
        for (var entry : workflow.getInputs().entrySet()) {
            if (ValidationUtils.isBlank(entry.getKey())) {
                errors.add("input name is required");
            } else if (entry.getValue() == null) {
                errors.add("input definition must not be null for: " + entry.getKey());
            }
        }
    }

    private static void validateStateFields(WorkflowDefinition workflow, List<String> errors) {
        for (var entry : workflow.getState().entrySet()) {
            if (ValidationUtils.isBlank(entry.getKey())) {
                errors.add("state field name is required");
            } else if (entry.getValue() == null) {
                errors.add("state field definition must not be null for: " + entry.getKey());
            }
        }
    }

    private static void validateVariables(WorkflowDefinition workflow, List<String> errors) {
        Set<String> variableNames = new HashSet<>();
        for (VariableDefinition variable : workflow.getVariables()) {
            if (variable == null) {
                errors.add("variable entry must not be null");
                continue;
            }
            if (ValidationUtils.isBlank(variable.getName())) {
                errors.add("variable name is required");
            } else if (!variableNames.add(variable.getName())) {
                errors.add("duplicate variable name: " + variable.getName());
            }
            if (variable.getScope() == null) {
                errors.add("variable scope is required for: " + variable.getName());
            }
        }
    }

    private static void validateModelProviders(
            WorkflowDefinition workflow, WorkflowValidationState state, List<String> errors) {
        for (ModelProviderDefinition provider : workflow.getModelProviders()) {
            if (provider == null) {
                errors.add("model provider entry must not be null");
                continue;
            }
            if (ValidationUtils.isBlank(provider.getId())) {
                errors.add("model provider id is required");
            } else if (!state.providerIds.add(provider.getId())) {
                errors.add("duplicate model provider id: " + provider.getId());
            }
        }
    }

    private static void validateModelRouting(
            WorkflowDefinition workflow, WorkflowValidationState state, List<String> errors) {
        for (ModelRoutingDefinition routing : workflow.getModelRouting()) {
            if (routing != null && !ValidationUtils.isBlank(routing.getDefaultProviderId())) {
                String defaultId = routing.getDefaultProviderId();
                if (!state.providerIds.isEmpty() && !state.providerIds.contains(defaultId)) {
                    errors.add("model routing references unknown provider: " + defaultId);
                }
            }
        }
    }

    private static void validateEdges(WorkflowDefinition workflow, WorkflowValidationState state, List<String> errors) {
        int edgeIndex = 0;
        for (EdgeDefinition edge : workflow.getEdges()) {
            if (edge == null) {
                errors.add("edge entry must not be null");
                edgeIndex++;
                continue;
            }
            String prefix = "edge[" + edgeIndex + "]: ";
            if (ValidationUtils.isBlank(edge.getSourceNodeId())) {
                errors.add(prefix + "sourceNodeId is required");
            } else if (!state.nodeIds.contains(edge.getSourceNodeId())) {
                errors.add(prefix + "unknown source node: " + edge.getSourceNodeId());
            }
            if (ValidationUtils.isBlank(edge.getTargetNodeId())) {
                errors.add(prefix + "targetNodeId is required");
            } else if (!state.nodeIds.contains(edge.getTargetNodeId())) {
                errors.add(prefix + "unknown target node: " + edge.getTargetNodeId());
            }
            if (edge.getSourceNodeId() != null
                    && edge.getSourceNodeId().equals(edge.getTargetNodeId())) {
                errors.add(prefix + "self-loop on node: " + edge.getSourceNodeId());
            }
            WorkflowPortValidator.validateEdgePorts(edge, state.nodesById, prefix, state, errors);
            edgeIndex++;
        }
    }

    private static void validatePortConnectionCounts(
            WorkflowDefinition workflow, WorkflowValidationState state, List<String> errors) {
        for (NodeDefinition node : workflow.getNodes()) {
            if (node != null) {
                WorkflowPortValidator.validatePortConnectionCounts(node, state, errors);
            }
        }
    }
}
