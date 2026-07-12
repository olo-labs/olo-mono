/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.spi.hook;

import java.util.Map;
import java.util.Objects;

/**
 * Outcome of a {@link Hook} execution. Hooks are typically non-blocking; failures may be logged but not always propagated.
 */
public record HookResult(
        HookStatus status,
        Map<String, Object> attributes,
        String message,
        Throwable error) {

    public HookResult {
        Objects.requireNonNull(status, "status");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public static HookResult ok() {
        return new HookResult(HookStatus.OK, Map.of(), null, null);
    }

    public static HookResult ok(Map<String, Object> attributes) {
        return new HookResult(HookStatus.OK, attributes, null, null);
    }

    public static HookResult failed(String message, Throwable error) {
        return new HookResult(HookStatus.FAILED, Map.of(), message, error);
    }
}
