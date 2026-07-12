/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.workflow;

import org.olo.definition.agent.AgentDefinition;
import org.olo.definition.capability.CapabilityDefinition;
import org.olo.definition.designer.DesignerDefinition;
import org.olo.definition.edge.EdgeDefinition;
import org.olo.definition.extension.ExtensionDefinition;
import org.olo.definition.hook.HookDefinition;
import org.olo.definition.input.WorkflowInputDefinition;
import org.olo.definition.model.ModelProviderDefinition;
import org.olo.definition.model.ModelRoutingDefinition;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.parameter.WorkflowParameterDefinition;
import org.olo.definition.planner.AgentReferenceDefinition;
import org.olo.definition.runtime.RuntimeBindingDefinition;
import org.olo.definition.runtime.WorkflowRuntimeDefinition;
import org.olo.definition.state.StateFieldDefinition;
import org.olo.definition.tool.ToolDefinition;
import org.olo.definition.variable.VariableDefinition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable field bundle materialized from {@link WorkflowDefinitionBuilder}.
 */
record WorkflowDefinitionFields(
        String id,
        Boolean enabled,
        Boolean isDefault,
        String label,
        String role,
        String shortDescription,
        String emoji,
        DesignerDefinition designer,
        String queue,
        String workflowType,
        Boolean runAgain,
        String longDescription,
        Boolean isExternalWorkflow,
        Boolean isChildWorkflow,
        List<ChildWorkflowDefinition> childWorkflows,
        List<AgentReferenceDefinition> availableAgents,
        String version,
        List<NodeDefinition> nodes,
        List<EdgeDefinition> edges,
        Map<String, WorkflowInputDefinition> inputs,
        Map<String, StateFieldDefinition> state,
        Map<String, WorkflowParameterDefinition> parameters,
        List<VariableDefinition> variables,
        List<ModelProviderDefinition> modelProviders,
        List<ModelRoutingDefinition> modelRouting,
        List<ExtensionDefinition> extensions,
        Map<String, Object> metadata,
        CapabilityDefinition capability,
        WorkflowRuntimeDefinition runtime,
        RuntimeBindingDefinition runtimeBinding,
        List<ToolDefinition> tools,
        List<AgentDefinition> agents,
        List<HookDefinition> hooks) {

    static WorkflowDefinitionFields from(WorkflowDefinitionBuilder builder) {
        return new WorkflowDefinitionFields(
                builder.id,
                builder.enabled,
                builder.isDefault,
                builder.label,
                builder.role,
                builder.shortDescription,
                builder.emoji,
                builder.designer,
                builder.queue,
                builder.workflowType,
                builder.runAgain,
                builder.longDescription,
                builder.isExternalWorkflow,
                builder.isChildWorkflow,
                copyList(builder.childWorkflows),
                copyList(builder.availableAgents),
                builder.version,
                copyList(builder.nodes),
                copyList(builder.edges),
                copyMap(builder.inputs),
                copyMap(builder.state),
                copyMap(builder.parameters),
                copyList(builder.variables),
                copyList(builder.modelProviders),
                copyList(builder.modelRouting),
                copyList(builder.extensions),
                copyMetadata(builder.metadata),
                builder.capability,
                builder.runtime,
                builder.runtimeBinding,
                copyList(builder.tools),
                copyList(builder.agents),
                copyList(builder.hooks));
    }

    private static <T> List<T> copyList(List<T> source) {
        return source == null ? List.of() : List.copyOf(source);
    }

    private static <V> Map<String, V> copyMap(Map<String, V> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    private static Map<String, Object> copyMetadata(Map<String, Object> source) {
        if (source == null) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
