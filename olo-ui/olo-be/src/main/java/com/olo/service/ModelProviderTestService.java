/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package com.olo.service;

import com.olo.dto.ModelProviderTestRequest;
import com.olo.dto.ModelProviderTestResponse;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ModelProviderTestService {

    private static final Pattern ENV_REF = Pattern.compile("^\\$\\{env:([^}]+)}$");
    private static final Duration TIMEOUT = Duration.ofSeconds(12);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

    public ModelProviderTestResponse test(ModelProviderTestRequest request) {
        if (request.provider() == null || request.provider().isBlank()) {
            return ModelProviderTestResponse.failure("Provider type is required");
        }

        String kind = request.provider().trim().toLowerCase(Locale.ROOT);
        Map<String, Object> config = request.configuration() == null ? Map.of() : request.configuration();
        String baseUrl = stringValue(config.get("baseUrl"));
        String apiKey = resolveSecret(stringValue(config.get("apiKey")), stringValue(config.get("apiKeyRef")));
        String model = request.model() == null ? "" : request.model().trim();

        long started = System.currentTimeMillis();
        try {
            return switch (kind) {
                case "local", "ollama" -> testOllama(baseUrl, model, started);
                case "openai" -> testOpenAiCompatible(
                        baseUrl.isBlank() ? "https://api.openai.com/v1" : baseUrl,
                        apiKey,
                        model,
                        started,
                        "OpenAI");
                case "url" -> {
                    if (baseUrl.isBlank()) {
                        yield ModelProviderTestResponse.failure("Base URL is required for URL providers");
                    }
                    yield testOpenAiCompatible(baseUrl, apiKey, model, started, "endpoint");
                }
                default -> ModelProviderTestResponse.failure("Unsupported provider type: " + kind);
            };
        } catch (Exception e) {
            return ModelProviderTestResponse.failure(e.getMessage() == null ? "Connection failed" : e.getMessage());
        }
    }

    private ModelProviderTestResponse testOllama(String baseUrl, String model, long started) throws Exception {
        String root = normalizeOllamaRoot(baseUrl);
        URI uri = URI.create(root + "/api/tags");
        HttpResponse<String> response = sendGet(uri, null);
        long latency = System.currentTimeMillis() - started;
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return ModelProviderTestResponse.failure(
                    "Ollama returned HTTP " + response.statusCode() + " at " + uri);
        }
        String message = "Connected to local provider at " + root;
        if (!model.isBlank() && !response.body().contains(model)) {
            message += " (model '" + model + "' not listed — it may still work if pulled on first use)";
        } else if (!model.isBlank()) {
            message += " (model '" + model + "' found)";
        }
        return ModelProviderTestResponse.success(message, latency, model.isBlank() ? null : model);
    }

    private ModelProviderTestResponse testOpenAiCompatible(
            String baseUrl,
            String apiKey,
            String model,
            long started,
            String label) throws Exception {
        String root = normalizeRoot(baseUrl, null);
        URI uri = URI.create(root + "/models");
        HttpResponse<String> response = sendGet(uri, apiKey);
        long latency = System.currentTimeMillis() - started;
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            return ModelProviderTestResponse.failure(
                    "Authentication failed (HTTP " + response.statusCode() + "). Check API key.");
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return ModelProviderTestResponse.failure(
                    label + " returned HTTP " + response.statusCode() + " at " + uri);
        }
        String message = "Connected to " + label + " at " + root;
        if (!model.isBlank() && !response.body().contains(model)) {
            message += " (model '" + model + "' not listed in /v1/models)";
        } else if (!model.isBlank()) {
            message += " (model '" + model + "' available)";
        }
        return ModelProviderTestResponse.success(message, latency, model.isBlank() ? null : model);
    }

    private HttpResponse<String> sendGet(URI uri, String apiKey) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(TIMEOUT)
                .GET();
        if (apiKey != null && !apiKey.isBlank()) {
            builder.header("Authorization", "Bearer " + apiKey.trim());
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String normalizeOllamaRoot(String baseUrl) {
        String value = baseUrl == null || baseUrl.isBlank() ? "http://localhost:51435" : baseUrl.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        if (value.endsWith("/v1")) {
            value = value.substring(0, value.length() - 3);
            while (value.endsWith("/")) {
                value = value.substring(0, value.length() - 1);
            }
        }
        return value;
    }

    private static String normalizeRoot(String baseUrl, String defaultUrl) {
        String value = baseUrl == null || baseUrl.isBlank()
                ? (defaultUrl == null ? "" : defaultUrl)
                : baseUrl.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        if (value.endsWith("/v1")) {
            return value;
        }
        return value + "/v1";
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String resolveSecret(String apiKey, String apiKeyRef) {
        if (apiKey != null && !apiKey.isBlank()) {
            return apiKey;
        }
        if (apiKeyRef == null || apiKeyRef.isBlank()) {
            return "";
        }
        Matcher matcher = ENV_REF.matcher(apiKeyRef.trim());
        if (matcher.matches()) {
            String env = System.getenv(matcher.group(1));
            return env == null ? "" : env;
        }
        return apiKeyRef;
    }
}
