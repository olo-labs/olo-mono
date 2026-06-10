package org.olo.spi.tool;

import org.olo.spi.context.ExecutionContext;

/**
 * Runtime contract for a callable tool referenced from a workflow graph.
 */
public interface Tool {

    /**
     * Stable tool identifier matching {@code ToolDefinition.id} or registry {@code implementationId}.
     */
    String toolId();

    /**
     * Invokes the tool with the given request within the shared execution context.
     */
    ToolResult invoke(ToolRequest request, ExecutionContext context);
}
