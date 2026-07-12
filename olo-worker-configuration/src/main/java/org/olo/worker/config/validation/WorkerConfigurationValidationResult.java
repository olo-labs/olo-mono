/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.worker.config.validation;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class WorkerConfigurationValidationResult {

    private final boolean valid;
    private final List<String> errors;

    private WorkerConfigurationValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = List.copyOf(errors);
    }

    public static WorkerConfigurationValidationResult success() {
        return new WorkerConfigurationValidationResult(true, List.of());
    }

    public static WorkerConfigurationValidationResult failure(List<String> errors) {
        Objects.requireNonNull(errors, "errors");
        return new WorkerConfigurationValidationResult(false, errors);
    }

    public boolean isValid() {
        return valid;
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }
}
