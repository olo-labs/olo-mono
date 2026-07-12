/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.definition.workflow.impl;

import org.olo.definition.agent.AgentDefinition;
import org.olo.definition.designer.DesignerDefinition;
import org.olo.definition.extension.ExtensionDefinition;
import org.olo.definition.hook.HookDefinition;
import org.olo.definition.tool.ToolDefinition;
import org.olo.definition.workflow.ChildWorkflowDefinition;
import org.olo.definition.workflow.WorkflowBuilder;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Identity, metadata, version, queue, and catalog attachment helpers for {@link WorkflowBuilder}.
 */
public final class WorkflowBuilderCore {

    private final WorkflowBuilderState state;
    private final WorkflowBuilder owner;

    public WorkflowBuilderCore(WorkflowBuilderState state, WorkflowBuilder owner) {
        this.state = state;
        this.owner = owner;
    }

    // --- identity and display metadata ---

    public WorkflowBuilder id(String id) {
        state.delegate.id(id);
        return owner;
    }

    public WorkflowBuilder enabled(Boolean enabled) {
        state.delegate.enabled(enabled);
        return owner;
    }

    public WorkflowBuilder isDefault(Boolean isDefault) {
        state.delegate.isDefault(isDefault);
        return owner;
    }

    public WorkflowBuilder label(String label) {
        state.delegate.label(label);
        return owner;
    }

    public WorkflowBuilder role(String role) {
        state.delegate.role(role);
        return owner;
    }

    public WorkflowBuilder shortDescription(String shortDescription) {
        state.delegate.shortDescription(shortDescription);
        return owner;
    }

    public WorkflowBuilder emoji(String emoji) {
        state.delegate.emoji(emoji);
        return owner;
    }

    // --- queue, type, and lifecycle flags ---

    public WorkflowBuilder queue(String queue) {
        state.delegate.queue(queue);
        return owner;
    }

    public WorkflowBuilder workflowType(String workflowType) {
        state.delegate.workflowType(workflowType);
        return owner;
    }

    public WorkflowBuilder runAgain(Boolean runAgain) {
        state.delegate.runAgain(runAgain);
        return owner;
    }

    public WorkflowBuilder longDescription(String longDescription) {
        state.delegate.longDescription(longDescription);
        return owner;
    }

    public WorkflowBuilder isExternalWorkflow(Boolean isExternalWorkflow) {
        state.delegate.isExternalWorkflow(isExternalWorkflow);
        return owner;
    }

    public WorkflowBuilder isChildWorkflow(Boolean isChildWorkflow) {
        state.delegate.isChildWorkflow(isChildWorkflow);
        return owner;
    }

    public WorkflowBuilder childWorkflow(ChildWorkflowDefinition childWorkflow) {
        Objects.requireNonNull(childWorkflow, "childWorkflow is required");
        state.childWorkflows.add(childWorkflow);
        return owner;
    }

    public WorkflowBuilder childWorkflowRef(String workflowId) {
        return childWorkflow(ChildWorkflowDefinition.builder().workflowId(workflowId).build());
    }

    public WorkflowBuilder version(String version) {
        state.delegate.version(version);
        return owner;
    }

    // --- arbitrary metadata and designer ---

    public WorkflowBuilder metadata(String key, Object value) {
        state.metadata.put(key, value);
        return owner;
    }

    public WorkflowBuilder metadata(Map<String, Object> metadata) {
        state.metadata.putAll(metadata);
        return owner;
    }

    public WorkflowBuilder designer(DesignerDefinition designer) {
        state.delegate.designer(designer);
        return owner;
    }

    // --- catalog attachments (extensions, tools, agents, hooks) ---

    public WorkflowBuilder extension(ExtensionDefinition extension) {
        Objects.requireNonNull(extension, "extension is required");
        state.extensions.add(extension);
        return owner;
    }

    public WorkflowBuilder tool(ToolDefinition tool) {
        Objects.requireNonNull(tool, "tool is required");
        state.tools.add(tool);
        return owner;
    }

    public WorkflowBuilder agent(AgentDefinition agent) {
        Objects.requireNonNull(agent, "agent is required");
        state.agents.add(agent);
        return owner;
    }

    public WorkflowBuilder hook(HookDefinition hook) {
        Objects.requireNonNull(hook, "hook is required");
        state.hooks.add(hook);
        return owner;
    }

    public static String slugify(String name) {
        return name.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}
