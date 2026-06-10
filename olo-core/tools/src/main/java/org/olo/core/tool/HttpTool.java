package org.olo.core.tool;

import org.olo.annotation.OloProperty;
import org.olo.annotation.OloPropertyType;
import org.olo.annotation.OloTool;
import org.olo.spi.annotation.ImplementationId;
import org.olo.spi.annotation.ToolId;
import org.olo.spi.context.ExecutionContext;
import org.olo.spi.tool.Tool;
import org.olo.spi.tool.ToolRequest;
import org.olo.spi.tool.ToolResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@OloTool(
        id = CoreToolIds.HTTP,
        name = "HTTP",
        description = "Basic HTTP GET/POST client",
        featured = true,
        category = "integration",
        emoji = "🌐",
        tags = {"http", "core"},
        examples = {
            "Call REST API",
            "Fetch weather data"
        },
        arguments = {
            @OloProperty(
                    name = "url",
                    label = "URL",
                    type = OloPropertyType.STRING,
                    description = "Request target URL",
                    help = "Full URL including https://",
                    required = true,
                    placeholder = "https://api.example.com/data",
                    group = "General",
                    order = 0),
            @OloProperty(
                    name = "method",
                    label = "HTTP Method",
                    type = OloPropertyType.ENUM,
                    defaultValue = "GET",
                    enumValues = {"GET", "POST"},
                    group = "General",
                    order = 1),
            @OloProperty(
                    name = "body",
                    label = "Request Body",
                    type = OloPropertyType.JSON,
                    description = "Optional HTTP request body",
                    help = "JSON body for POST requests; leave empty for GET.",
                    placeholder = "{\"key\": \"value\"}",
                    group = "Advanced",
                    order = 2)
        },
        capabilityInputs = {"url"},
        capabilityOutputs = {"body"})
@ToolId(CoreToolIds.HTTP)
@ImplementationId(CoreToolIds.HTTP)
public final class HttpTool implements Tool {

    private final HttpClient httpClient;

    public HttpTool() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    HttpTool(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public String toolId() {
        return CoreToolIds.HTTP;
    }

    @Override
    public ToolResult invoke(ToolRequest request, ExecutionContext context) {
        String url = ToolArgs.string(request.arguments(), "url",
                ToolArgs.string(request.configuration(), "url", ""));
        if (url.isBlank()) {
            return ToolResult.failure("HTTP tool requires url argument", null);
        }

        String method = ToolArgs.string(request.arguments(), "method",
                ToolArgs.string(request.configuration(), "method", "GET")).toUpperCase();
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30));
            if ("POST".equals(method)) {
                String body = ToolArgs.string(request.arguments(), "body", "");
                builder.POST(HttpRequest.BodyPublishers.ofString(body));
            } else {
                builder.GET();
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return ToolResult.success(Map.of(
                    "statusCode", response.statusCode(),
                    "body", response.body()));
        } catch (Exception e) {
            return ToolResult.failure("HTTP request failed: " + e.getMessage(), e);
        }
    }
}
