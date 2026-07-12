/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.hook;

/**
 * Implementation ids for built-in {@link org.olo.spi.hook.Hook} implementations.
 * <p>
 * Format: {@code {provider}:{localId}} — globally unique across community plugins.
 */
public final class CoreHookIds {

    public static final String PROVIDER = "olo-core";

    public static final String LOGGING = "olo-core:logging-hook";
    public static final String METRICS = "olo-core:metrics-hook";
    public static final String TRACING = "olo-core:tracing-hook";

    private CoreHookIds() {
    }
}
