package org.olo.kernel.agent.model;

/**
 * Resolved model provider settings for a single LLM invocation.
 */
public record ResolvedModelCall(
        String providerId,
        String providerType,
        String model,
        String baseUrl,
        double temperature) {
}
