package org.olo.kernel.agent.client.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.olo.kernel.exception.KernelException;
import org.olo.kernel.agent.client.LlmClient;
import org.olo.kernel.agent.model.ResolvedModelCall;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Calls a local Ollama-compatible {@code /api/chat} endpoint.
 */
public final class OllamaLlmClient implements LlmClient {

    private static final Duration TIMEOUT = Duration.ofMinutes(5);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient httpClient;

    public OllamaLlmClient() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build());
    }

    OllamaLlmClient(HttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
    }

    @Override
    public String complete(ResolvedModelCall modelCall, String prompt) {
        Objects.requireNonNull(modelCall, "modelCall");
        Objects.requireNonNull(prompt, "prompt");
        if (prompt.isBlank()) {
            throw new KernelException("LLM prompt is blank");
        }

        try {
            URI uri = URI.create(normalizeBaseUrl(modelCall.baseUrl()) + "/api/chat");
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", modelCall.model());
            body.put("stream", false);
            body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
            body.put("options", Map.of("temperature", modelCall.temperature()));

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new KernelException("LLM request failed with HTTP "
                        + response.statusCode()
                        + " from "
                        + uri
                        + ": "
                        + truncate(response.body()));
            }

            JsonNode root = MAPPER.readTree(response.body());
            JsonNode content = root.path("message").path("content");
            if (content.isTextual() && !content.asText().isBlank()) {
                return content.asText().trim();
            }
            throw new KernelException("LLM response missing message.content from " + uri);
        } catch (KernelException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KernelException("LLM request interrupted for model " + modelCall.model(), e);
        } catch (Exception e) {
            String detail = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            throw new KernelException(
                    "LLM request failed for model "
                            + modelCall.model()
                            + " at "
                            + modelCall.baseUrl()
                            + ": "
                            + detail
                            + " (is Ollama running? try: ollama serve)",
                    e);
        }
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

    private static String truncate(String body) {
        if (body == null) {
            return "";
        }
        if (body.length() <= 300) {
            return body;
        }
        return body.substring(0, 300) + "...";
    }
}
