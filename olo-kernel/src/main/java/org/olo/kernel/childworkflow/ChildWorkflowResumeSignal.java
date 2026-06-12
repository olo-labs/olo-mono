package org.olo.kernel.childworkflow;

import java.util.Map;
import java.util.Objects;

/**
 * Parent workflow resume input after a child run completes or signals.
 */
public record ChildWorkflowResumeSignal(String childRunId, Map<String, Object> payload) {

    public ChildWorkflowResumeSignal {
        Objects.requireNonNull(childRunId, "childRunId");
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}
