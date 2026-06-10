package org.olo.spi.hook;

import java.util.Map;
import java.util.Objects;

/**
 * Context passed to a {@link Hook} when a lifecycle phase fires.
 */
public record HookRequest(
        HookPhase phase,
        String hookId,
        String nodeId,
        String nodeType,
        NodeOutcome nodeOutcome,
        Map<String, Object> attributes) {

    public HookRequest {
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(hookId, "hookId");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    /**
     * Summary of the node execution that triggered the hook (when applicable).
     */
    public record NodeOutcome(String status, String message, Throwable error) {
    }
}
