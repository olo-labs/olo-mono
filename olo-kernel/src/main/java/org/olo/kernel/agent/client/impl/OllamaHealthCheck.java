/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agent.client.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.olo.kernel.exception.KernelException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;

/**
 * Fast reachability probe for an Ollama-compatible server ({@code GET /api/tags}).
 */
public final class OllamaHealthCheck {

    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(10);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient httpClient;

    public OllamaHealthCheck() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    }

    OllamaHealthCheck(HttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
    }

    /**
     * Verifies the server responds to {@code /api/tags} within a short timeout.
     *
     * @throws KernelException when the endpoint is unreachable or returns a non-success status
     */
    public void verifyReachable(String baseUrl) {
        Objects.requireNonNull(baseUrl, "baseUrl");
        String normalized = normalizeBaseUrl(baseUrl);
        URI uri = URI.create(normalized + "/api/tags");
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(PROBE_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new KernelException(
                        "LLM server at "
                                + normalized
                                + " returned HTTP "
                                + response.statusCode()
                                + " from /api/tags");
            }
        } catch (KernelException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KernelException("LLM health check interrupted for " + normalized, e);
        } catch (Exception e) {
            String detail = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            throw new KernelException(
                    "LLM server not available at "
                            + normalized
                            + ": "
                            + detail
                            + ". Start Ollama (e.g. docker compose up ollama) and pull required models.",
                    e);
        }
    }

    /**
     * Verifies a model name appears in {@code /api/tags} (optional {@code :latest} suffix).
     */
    public void verifyModelPresent(String baseUrl, String model) {
        verifyReachable(baseUrl);
        if (model == null || model.isBlank()) {
            return;
        }
        String normalized = normalizeBaseUrl(baseUrl);
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(normalized + "/api/tags"))
                    .timeout(PROBE_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode models = MAPPER.readTree(response.body()).path("models");
            if (!models.isArray()) {
                throw new KernelException("LLM server at " + normalized + " returned unexpected /api/tags payload");
            }
            for (JsonNode entry : models) {
                String name = entry.path("name").asText("");
                if (model.equals(name) || modelWithoutTag(model).equals(modelWithoutTag(name))) {
                    return;
                }
            }
            throw new KernelException(
                    "LLM model '"
                            + model
                            + "' is not installed on "
                            + normalized
                            + ". Run: ollama pull "
                            + modelWithoutTag(model));
        } catch (KernelException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KernelException("LLM model check interrupted for " + normalized, e);
        } catch (Exception e) {
            String detail = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            throw new KernelException(
                    "LLM model check failed for " + model + " at " + normalized + ": " + detail, e);
        }
    }

    private static String modelWithoutTag(String model) {
        int colon = model.indexOf(':');
        return colon < 0 ? model : model.substring(0, colon);
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }
}
