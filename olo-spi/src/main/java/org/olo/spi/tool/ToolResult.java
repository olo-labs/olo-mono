package org.olo.spi.tool;

import java.util.Map;
import java.util.Objects;

/**
 * Outcome of a {@link Tool} invocation.
 */
public record ToolResult(
        ToolStatus status,
        Map<String, Object> output,
        String message,
        Throwable error) {

    public ToolResult {
        Objects.requireNonNull(status, "status");
        output = output == null ? Map.of() : Map.copyOf(output);
    }

    public static ToolResult success(Map<String, Object> output) {
        return new ToolResult(ToolStatus.SUCCESS, output, null, null);
    }

    public static ToolResult success(String message, Map<String, Object> output) {
        return new ToolResult(ToolStatus.SUCCESS, output, message, null);
    }

    public static ToolResult failure(String message, Throwable error) {
        return new ToolResult(ToolStatus.FAILURE, Map.of(), message, error);
    }
}
