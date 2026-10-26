/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core;

import org.olo.core.runtime.ExecutionEngine;
import org.olo.core.runtime.HookRegistry;
import org.olo.core.runtime.NodeRegistry;

/**
 * Entry point for the {@code org.olo:olo-core} artifact.
 */
public final class Core {

    private Core() {
    }

    /**
     * Creates an {@link ExecutionEngine} with all built-in nodes and hooks registered.
     */
    public static ExecutionEngine defaultEngine() {
        return ExecutionEngine.withDefaults();
    }

    public static NodeRegistry defaultNodeRegistry() {
        return NodeRegistry.withDefaults();
    }



    public static HookRegistry defaultHookRegistry() {
        return HookRegistry.withDefaults();
    }
}
