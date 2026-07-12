/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.runtime;

import org.olo.core.tool.CoreTools;
import org.olo.spi.tool.Tool;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry of {@link Tool} implementations keyed by {@link Tool#toolId()}.
 */
public final class ToolRegistry {

    private final Map<String, Tool> byId = new LinkedHashMap<>();

    public void register(Tool tool) {
        if (tool == null) {
            throw new IllegalArgumentException("tool must not be null");
        }
        byId.put(tool.toolId(), tool);
    }

    public void registerAll(Collection<Tool> tools) {
        if (tools != null) {
            tools.forEach(this::register);
        }
    }

    public Optional<Tool> find(String toolId) {
        if (toolId == null || toolId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(byId.get(toolId));
    }

    public Map<String, Tool> snapshot() {
        return Map.copyOf(byId);
    }

    public static ToolRegistry withDefaults() {
        ToolRegistry registry = new ToolRegistry();
        registry.registerAll(CoreTools.all());
        return registry;
    }
}
