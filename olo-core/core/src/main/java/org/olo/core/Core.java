package org.olo.core;

import org.olo.core.runtime.ExecutionEngine;
import org.olo.core.runtime.HookRegistry;
import org.olo.core.runtime.NodeRegistry;
import org.olo.core.runtime.ToolRegistry;

/**
 * Entry point for the {@code org.olo:olo-core} artifact.
 */
public final class Core {

    private Core() {
    }

    /**
     * Creates an {@link ExecutionEngine} with all built-in nodes, tools, and hooks registered.
     */
    public static ExecutionEngine defaultEngine() {
        return ExecutionEngine.withDefaults();
    }

    public static NodeRegistry defaultNodeRegistry() {
        return NodeRegistry.withDefaults();
    }

    public static ToolRegistry defaultToolRegistry() {
        return ToolRegistry.withDefaults();
    }

    public static HookRegistry defaultHookRegistry() {
        return HookRegistry.withDefaults();
    }
}
