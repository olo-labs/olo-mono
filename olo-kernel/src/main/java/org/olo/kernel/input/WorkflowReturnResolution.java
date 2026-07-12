/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.input;

import java.util.Objects;

/**
 * Resolved workflow return message and the variable it came from (if any).
 */
public record WorkflowReturnResolution(
        String returnVariableName,
        Object returnVariableValue,
        String message,
        boolean usedAdminFallback) {

    public WorkflowReturnResolution {
        Objects.requireNonNull(message, "message");
    }
}
