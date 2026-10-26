/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package com.olo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Calls the olo chat/runtime backend to reload pipeline configuration from disk.
 */
@Component
public class OloRuntimeRefreshClient {

    private static final Logger log = LoggerFactory.getLogger(OloRuntimeRefreshClient.class);

    private final RestTemplate restTemplate;
    private final String runtimeBaseUrl;

    public OloRuntimeRefreshClient(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${olo.runtime.base-url:http://localhost:7080}") String runtimeBaseUrl) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(15))
                .build();
        this.runtimeBaseUrl = runtimeBaseUrl != null && !runtimeBaseUrl.isBlank()
                ? runtimeBaseUrl.trim().replaceAll("/+$", "")
                : "http://localhost:7080";
    }

    public RuntimeReloadResult reloadConfiguration() {
        String url = runtimeBaseUrl + "/api/admin/configuration/reload";
        try {
            RuntimeReloadResult result = restTemplate.postForObject(url, null, RuntimeReloadResult.class);
            if (result == null) {
                return new RuntimeReloadResult(false, 0, 0, "empty response from " + url);
            }
            if (!result.ok()) {
                return new RuntimeReloadResult(
                        false,
                        result.regionCount(),
                        result.workflowCount(),
                        result.message() != null ? result.message() : "runtime reload failed");
            }
            log.info(
                    "Olo runtime configuration reloaded: regions={} workflows={}",
                    result.regionCount(),
                    result.workflowCount());
            return result;
        } catch (RestClientException e) {
            log.warn("Olo runtime configuration reload failed at {}: {}", url, e.toString());
            return new RuntimeReloadResult(false, 0, 0, e.getMessage());
        }
    }

    public String runtimeBaseUrl() {
        return runtimeBaseUrl;
    }

    public record RuntimeReloadResult(boolean ok, int regionCount, int workflowCount, String message) {}
}
