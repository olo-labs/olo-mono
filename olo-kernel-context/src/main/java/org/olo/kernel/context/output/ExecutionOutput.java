package org.olo.kernel.context.output;

import java.util.Map;
import java.util.Objects;

/**
 * Captured output from a single graph node during workflow execution.
 */
public record ExecutionOutput(
        String nodeId,
        String nodeType,
        Object value,
        String message,
        Map<String, Object> payload) {

    public ExecutionOutput {
        Objects.requireNonNull(nodeId, "nodeId");
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }

    public String asReturnMessage() {
        if (value != null) {
            String text = String.valueOf(value);
            if (!text.isBlank()) {
                return text;
            }
        }
        if (message != null && !message.isBlank()) {
            return message;
        }
        return null;
    }
}
