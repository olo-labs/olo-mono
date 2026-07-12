/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.worker.temporal;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.net.ConnectException;

/**
 * Detects Temporal connectivity failures from gRPC and network exceptions.
 */
public final class TemporalConnectionErrors {

    private TemporalConnectionErrors() {
    }

    public static boolean isServerUnavailable(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof StatusRuntimeException statusRuntimeException) {
                Status.Code code = statusRuntimeException.getStatus().getCode();
                if (code == Status.Code.UNAVAILABLE || code == Status.Code.DEADLINE_EXCEEDED) {
                    return true;
                }
            }
            if (current instanceof ConnectException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains("connection refused")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
