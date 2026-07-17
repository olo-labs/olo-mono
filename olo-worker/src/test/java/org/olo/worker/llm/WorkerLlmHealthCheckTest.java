/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.worker.llm;

import org.junit.jupiter.api.Test;
import org.olo.bootstrap.model.CachedWorkflowDefinition;
import org.olo.bootstrap.registry.WorkflowDefinitionRegistry;
import org.olo.definition.model.ModelProviderDefinition;
import org.olo.definition.workflow.WorkflowDefinition;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class WorkerLlmHealthCheckTest {

    @Test
    void collectsLocalModelBaseUrlsFromRegistry() {
        Map<String, Set<String>> urls = WorkerLlmHealthCheck.collectModelsByBaseUrl(registryWithLocalProvider(
                "http://localhost:11435", "llama3.2:latest"));
        assertThat(urls).containsKey("http://localhost:11435");
        assertThat(urls.get("http://localhost:11435")).contains("llama3.2:latest");
    }

    private static WorkflowDefinitionRegistry registryWithLocalProvider(String baseUrl, String model) {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("wf1")
                .modelProviders(List.of(ModelProviderDefinition.builder()
                        .id("model-provider")
                        .provider("local")
                        .model(model)
                        .putConfiguration("baseUrl", baseUrl)
                        .build()))
                .build();
        return WorkflowDefinitionRegistry.of(
                Path.of("test"),
                List.of(new CachedWorkflowDefinition("wf1.json", workflow)));
    }
}
