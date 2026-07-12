/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation;

/**
 * Declarative canvas port profile for catalog components that appear on the workflow graph.
 * <p>
 * Expanded at compile time by the annotation processor into concrete {@link OloPort} metadata.
 * Use explicit {@code inputs}/{@code outputs} on {@link OloNode} when a type needs a custom layout.
 */
public enum OloCanvasPorts {

    /** Registry-only; no graph ports. */
    NONE,

    /**
     * Message flow plus a {@code capabilities} output for tools and hooks.
     */
    CAPABILITY_PLUGIN,

    /**
     * Message flow plus an {@code agentPlug} output for delegate workflow presets.
     */
    AGENT_PLUGIN,

    /**
     * Planner/agent host: message flow plus {@code capabilities} and {@code agentPlug} inputs (0+ each).
     */
    PLANNER_HOST
}
