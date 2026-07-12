/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.exception;

public class KernelException extends RuntimeException {

    public KernelException(String message) {
        super(message);
    }

    public KernelException(String message, Throwable cause) {
        super(message, cause);
    }
}
