/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.worker.llm;

/**
 * Raised when the worker cannot reach the configured LLM (Ollama) endpoint at bootstrap.
 */
public final class LlmServerUnavailableException extends RuntimeException {

    public LlmServerUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public LlmServerUnavailableException(String message) {
        super(message);
    }
}
