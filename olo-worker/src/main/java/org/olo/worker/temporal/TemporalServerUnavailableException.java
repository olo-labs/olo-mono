/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.worker.temporal;

/**
 * Raised when the worker cannot reach the configured Temporal server.
 */
public final class TemporalServerUnavailableException extends RuntimeException {

    private final String target;
    private final String namespace;

    public TemporalServerUnavailableException(String target, String namespace, Throwable cause) {
        super(formatMessage(target, namespace), cause);
        this.target = target;
        this.namespace = namespace;
    }

    public String getTarget() {
        return target;
    }

    public String getNamespace() {
        return namespace;
    }

    private static String formatMessage(String target, String namespace) {
        return "Temporal server not available at "
                + target
                + " (namespace="
                + namespace
                + "). Start Temporal at the configured target or update temporal.target in worker config.";
    }
}
