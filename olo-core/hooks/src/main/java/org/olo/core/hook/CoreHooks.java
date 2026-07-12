/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.hook;

import org.olo.spi.hook.Hook;

import java.util.List;

/**
 * Factory for built-in hook implementations.
 */
public final class CoreHooks {

    private CoreHooks() {
    }

    public static List<Hook> all() {
        return List.of(
                new LoggingHook(),
                new MetricsHook(),
                new TracingHook());
    }
}
