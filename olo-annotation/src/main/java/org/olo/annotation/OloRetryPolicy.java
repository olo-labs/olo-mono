/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation;

/**
 * Named retry policy hint for orchestrators (e.g. Temporal activity retry).
 * <p>
 * Emitted as {@code runtime.defaultRetryPolicy} when not {@link #NONE}.
 */
public enum OloRetryPolicy {

    /** No default retry policy — omit from catalog. */
    NONE,

    /** Platform standard retry (balanced backoff). */
    STANDARD,

    /** Aggressive retry for transient external calls. */
    AGGRESSIVE
}
