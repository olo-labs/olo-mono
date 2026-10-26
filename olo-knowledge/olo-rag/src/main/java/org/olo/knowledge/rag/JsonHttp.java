// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.knowledge.rag;

import com.fasterxml.jackson.databind.JsonNode;

import org.olo.knowledge.core.Json;

import java.net.*;
import java.net.http.*;
import java.time.Duration;

/**
 * Fixed operator-configured origins, no redirects, bounded responses, no URL supplied by a document
 * or user.
 */
final class JsonHttp {
    private final URI origin;
    private final String apiKey;
    private final HttpClient client =
            HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();

    JsonHttp(URI origin, String apiKey) {
        if (!java.util.Set.of("http", "https").contains(origin.getScheme())
                || origin.getHost() == null
                || origin.getUserInfo() != null
                || origin.getQuery() != null
                || origin.getFragment() != null
                || !java.util.Set.of("", "/").contains(origin.getPath()))
            throw new IllegalArgumentException("Fixed service origin required");
        this.origin = origin;
        this.apiKey = apiKey;
    }

    JsonNode send(String method, String path, Object body, int max) throws Exception {
        var b =
                HttpRequest.newBuilder(origin.resolve(path))
                        .timeout(Duration.ofSeconds(5))
                        .header("Content-Type", "application/json");
        if (apiKey != null && !apiKey.isBlank()) b.header("api-key", apiKey);
        b.method(
                method,
                method.equals("GET")
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(Json.encode(body)));
        var response = client.send(b.build(), HttpResponse.BodyHandlers.ofInputStream());
        try (var stream = response.body()) {
            byte[] data = stream.readNBytes(max + 1);
            if (response.statusCode() < 200 || response.statusCode() > 299 || data.length > max)
                throw new IllegalStateException("Dependency unavailable");
            return Json.MAPPER.readTree(data);
        }
    }
}
