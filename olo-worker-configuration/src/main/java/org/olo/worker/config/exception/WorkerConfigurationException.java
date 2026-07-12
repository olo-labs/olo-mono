/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.worker.config.exception;

/**
 * Raised when worker configuration cannot be loaded or validated.
 */
public class WorkerConfigurationException extends RuntimeException {

    public WorkerConfigurationException(String message) {
        super(message);
    }

    public WorkerConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
