package org.olo.spi.node;

import java.util.Map;
import java.util.Objects;

/**
 * Inputs and configuration passed to a {@link Node} at execution time.
 */
public record NodeRequest(
        String nodeId,
        String nodeType,
        Map<String, Object> input,
        Map<String, Object> configuration) {

    public NodeRequest {
        Objects.requireNonNull(nodeId, "nodeId");
        Objects.requireNonNull(nodeType, "nodeType");
        input = input == null ? Map.of() : Map.copyOf(input);
        configuration = configuration == null ? Map.of() : Map.copyOf(configuration);
    }
}
