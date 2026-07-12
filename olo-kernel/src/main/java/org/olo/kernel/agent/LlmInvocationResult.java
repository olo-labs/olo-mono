/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agent;

import org.olo.kernel.agent.model.ResolvedModelCall;

/**
 * Outcome of rendering a workflow prompt and invoking the configured model provider.
 */
public record LlmInvocationResult(ResolvedModelCall modelCall, String renderedPrompt, String response) {
}
