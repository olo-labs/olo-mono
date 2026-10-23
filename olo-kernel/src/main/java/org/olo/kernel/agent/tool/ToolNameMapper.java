/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agent.tool;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Maps ToolGate tool names (for example {@code incidents.host-status}) to model-safe function names
 * ({@code incidents_host-status}) and back. Function-calling APIs only accept {@code [A-Za-z0-9_-]}.
 */
final class ToolNameMapper {

    private static final int MAX_LENGTH = 64;

    private final Map<String, String> toolGateByModelName = new LinkedHashMap<>();
    private final Map<String, String> modelByToolGateName = new LinkedHashMap<>();

    ToolNameMapper(Collection<String> toolGateNames) {
        for (String toolGateName : toolGateNames) {
            String base = sanitize(toolGateName);
            String candidate = base;
            for (int suffix = 2; toolGateByModelName.containsKey(candidate); suffix++) {
                String tail = "_" + suffix;
                candidate = base.substring(0, Math.min(base.length(), MAX_LENGTH - tail.length())) + tail;
            }
            toolGateByModelName.put(candidate, toolGateName);
            modelByToolGateName.put(toolGateName, candidate);
        }
    }

    String modelName(String toolGateName) {
        return modelByToolGateName.get(toolGateName);
    }

    /** Accepts the model-facing name, and also the original ToolGate name if the model echoes that instead. */
    Optional<String> toolGateName(String modelName) {
        String direct = toolGateByModelName.get(modelName);
        if (direct != null) {
            return Optional.of(direct);
        }
        return modelByToolGateName.containsKey(modelName) ? Optional.of(modelName) : Optional.empty();
    }

    static String sanitize(String name) {
        String safe = name.replaceAll("[^A-Za-z0-9_-]", "_");
        if (safe.isEmpty()) {
            safe = "tool";
        }
        return safe.length() <= MAX_LENGTH ? safe : safe.substring(0, MAX_LENGTH);
    }
}
