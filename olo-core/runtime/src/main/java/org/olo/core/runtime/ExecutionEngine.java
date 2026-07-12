/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.runtime;

import org.olo.spi.context.ExecutionContext;
import org.olo.spi.hook.HookRequest;
import org.olo.spi.hook.HookResult;
import org.olo.spi.node.Node;
import org.olo.spi.node.NodeRequest;
import org.olo.spi.node.NodeResult;
import org.olo.spi.tool.Tool;
import org.olo.spi.tool.ToolRequest;
import org.olo.spi.tool.ToolResult;

import java.util.Objects;

/**
 * Coordinates node, tool, and hook execution through registries.
 * <p>
 * Full graph traversal will be added when integrated with {@code olo-runtime}; this engine executes
 * individual steps invoked by the caller.
 */
public final class ExecutionEngine {

    private final NodeRegistry nodeRegistry;
    private final ToolRegistry toolRegistry;
    private final HookRegistry hookRegistry;

    public ExecutionEngine(NodeRegistry nodeRegistry, ToolRegistry toolRegistry, HookRegistry hookRegistry) {
        this.nodeRegistry = Objects.requireNonNull(nodeRegistry, "nodeRegistry");
        this.toolRegistry = Objects.requireNonNull(toolRegistry, "toolRegistry");
        this.hookRegistry = Objects.requireNonNull(hookRegistry, "hookRegistry");
    }

    public static ExecutionEngine withDefaults() {
        return new ExecutionEngine(
                NodeRegistry.withDefaults(),
                ToolRegistry.withDefaults(),
                HookRegistry.withDefaults());
    }

    public NodeResult executeNode(NodeRequest request, ExecutionContext context) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(context, "context");
        if (context instanceof DefaultExecutionContext defaultContext) {
            defaultContext.setNodeId(request.nodeId());
        }
        Node node = nodeRegistry.find(request.nodeType())
                .orElseThrow(() -> new IllegalStateException("no node registered for type: " + request.nodeType()));
        return node.execute(request, context);
    }

    public ToolResult invokeTool(ToolRequest request, ExecutionContext context) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(context, "context");
        Tool tool = toolRegistry.find(request.toolId())
                .orElseThrow(() -> new IllegalStateException("no tool registered for id: " + request.toolId()));
        return tool.invoke(request, context);
    }

    public HookResult runHook(HookRequest request, ExecutionContext context) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(context, "context");
        return hookRegistry.find(request.hookId())
                .map(hook -> hook.run(request, context))
                .orElseThrow(() -> new IllegalStateException("no hook registered for id: " + request.hookId()));
    }

    public NodeRegistry nodeRegistry() {
        return nodeRegistry;
    }

    public ToolRegistry toolRegistry() {
        return toolRegistry;
    }

    public HookRegistry hookRegistry() {
        return hookRegistry;
    }
}
