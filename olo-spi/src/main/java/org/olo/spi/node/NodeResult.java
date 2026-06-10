package org.olo.spi.node;

import java.util.Map;
import java.util.Objects;

/**
 * Outcome of a {@link Node} execution.
 */
public record NodeResult(
        NodeStatus status,
        Map<String, Object> output,
        String message,
        Throwable error) {

    public NodeResult {
        Objects.requireNonNull(status, "status");
        output = output == null ? Map.of() : Map.copyOf(output);
    }

    public static NodeResult completed(Map<String, Object> output) {
        return new NodeResult(NodeStatus.COMPLETED, output, null, null);
    }

    public static NodeResult completed(String message, Map<String, Object> output) {
        return new NodeResult(NodeStatus.COMPLETED, output, message, null);
    }

    public static NodeResult waiting(String message, Map<String, Object> output) {
        return new NodeResult(NodeStatus.WAITING, output, message, null);
    }

    public static NodeResult failed(String message, Throwable error) {
        return new NodeResult(NodeStatus.FAILED, Map.of(), message, error);
    }
}
