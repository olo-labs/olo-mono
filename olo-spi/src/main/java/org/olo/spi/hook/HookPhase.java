/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.spi.hook;

/**
 * Lifecycle phase for a {@link Hook}, aligned with workflow hook definitions.
 */
public enum HookPhase {
    PRE,
    ON_ERROR,
    FINALLY
}
