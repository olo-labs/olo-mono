package org.olo.kernel.context.output;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Named node outputs for a workflow execution. Keys are stable output slots (node id or configured alias).
 * Each key is written at most once per execution unless explicitly replaced by the same slot.
 */
public final class ExecutionOutputs {

    private final Map<String, ExecutionOutput> byKey = new LinkedHashMap<>();

    public void put(String key, ExecutionOutput output) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(output, "output");
        if (key.isBlank()) {
            throw new IllegalArgumentException("execution output key is blank");
        }
        byKey.put(key.trim(), output);
    }

    public boolean has(String key) {
        return key != null && !key.isBlank() && byKey.containsKey(key.trim());
    }

    public ExecutionOutput get(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return byKey.get(key.trim());
    }

    public Optional<String> lastKey() {
        if (byKey.isEmpty()) {
            return Optional.empty();
        }
        String last = null;
        for (String key : byKey.keySet()) {
            last = key;
        }
        return Optional.ofNullable(last);
    }

    public Map<String, ExecutionOutput> toMap() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(byKey));
    }
}
