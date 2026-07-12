/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.worker.llm;

import org.olo.bootstrap.registry.WorkflowDefinitionRegistry;
import org.olo.definition.model.ModelProviderDefinition;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.kernel.agent.client.impl.OllamaHealthCheck;
import org.olo.kernel.exception.KernelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Resolves LLM base URLs from worker env and loaded workflows, then probes Ollama health.
 */
public final class WorkerLlmHealthCheck {

    private static final Logger log = LoggerFactory.getLogger(WorkerLlmHealthCheck.class);
    private static final String DEFAULT_BASE_URL = "http://localhost:11435";

    private WorkerLlmHealthCheck() {
    }

    public static void verify(WorkflowDefinitionRegistry registry) {
        if (Boolean.getBoolean("olo.worker.skipLlmHealthCheck")) {
            log.warn("LLM health check skipped (olo.worker.skipLlmHealthCheck=true)");
            return;
        }
        Objects.requireNonNull(registry, "registry");

        Map<String, Set<String>> modelsByBaseUrl = collectModelsByBaseUrl(registry);
        OllamaHealthCheck healthCheck = new OllamaHealthCheck();
        for (Map.Entry<String, Set<String>> entry : modelsByBaseUrl.entrySet()) {
            String baseUrl = entry.getKey();
            log.info("Probing LLM endpoint {}", baseUrl);
            try {
                healthCheck.verifyReachable(baseUrl);
                for (String model : entry.getValue()) {
                    healthCheck.verifyModelPresent(baseUrl, model);
                }
            } catch (KernelException e) {
                throw new LlmServerUnavailableException(e.getMessage(), e);
            }
        }
    }

    static Map<String, Set<String>> collectModelsByBaseUrl(WorkflowDefinitionRegistry registry) {
        String override = System.getenv("OLO_LLM_BASE_URL");
        Map<String, Set<String>> modelsByBaseUrl = new LinkedHashMap<>();
        if (override != null && !override.isBlank()) {
            modelsByBaseUrl.put(override.trim(), collectAllModels(registry));
            return modelsByBaseUrl;
        }

        for (WorkflowDefinition workflow : registry.getWorkflowsById().values()) {
            if (workflow.getModelProviders() == null) {
                continue;
            }
            for (ModelProviderDefinition provider : workflow.getModelProviders()) {
                if (!isLocalProvider(provider)) {
                    continue;
                }
                String baseUrl = readBaseUrl(provider);
                modelsByBaseUrl.computeIfAbsent(baseUrl, ignored -> new LinkedHashSet<>());
                if (provider.getModel() != null && !provider.getModel().isBlank()) {
                    modelsByBaseUrl.get(baseUrl).add(provider.getModel().trim());
                }
            }
        }

        if (modelsByBaseUrl.isEmpty()) {
            modelsByBaseUrl.put(DEFAULT_BASE_URL, collectAllModels(registry));
        }
        return modelsByBaseUrl;
    }

    private static Set<String> collectAllModels(WorkflowDefinitionRegistry registry) {
        Set<String> models = new LinkedHashSet<>();
        for (WorkflowDefinition workflow : registry.getWorkflowsById().values()) {
            if (workflow.getModelProviders() == null) {
                continue;
            }
            for (ModelProviderDefinition provider : workflow.getModelProviders()) {
                if (provider.getModel() != null && !provider.getModel().isBlank()) {
                    models.add(provider.getModel().trim());
                }
            }
        }
        return models;
    }

    private static boolean isLocalProvider(ModelProviderDefinition provider) {
        String name = provider.getProvider();
        return name == null || name.isBlank() || "local".equalsIgnoreCase(name) || "ollama".equalsIgnoreCase(name);
    }

    private static String readBaseUrl(ModelProviderDefinition provider) {
        Map<String, Object> configuration = provider.getConfiguration();
        if (configuration != null && configuration.get("baseUrl") instanceof String text && !text.isBlank()) {
            return text.trim();
        }
        return DEFAULT_BASE_URL;
    }
}
