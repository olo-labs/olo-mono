/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation;

/**
 * Hook lifecycle phases for {@link OloHook}.
 */
public enum OloHookPhase {
    PRE,
    ON_ERROR,
    FINALLY
}
