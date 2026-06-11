package org.olo.core.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.olo.annotation.OloCatalogLocations;

import java.io.InputStream;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

class ToolCatalogGenerationTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  void compilesAuthoritativeToolsCatalog() throws Exception {
    try (InputStream in =
        getClass().getClassLoader().getResourceAsStream(OloCatalogLocations.TOOLS_CATALOG)) {
      assertThat(in).as("annotation processor should emit %s", OloCatalogLocations.TOOLS_CATALOG)
          .isNotNull();

      JsonNode root = MAPPER.readTree(in);
      assertThat(root.get("schemaVersion").asText()).isEqualTo("1.0");
      assertThat(root.get("moduleId").asText()).isEqualTo("olo-core-tools");
      assertThat(root.get("catalogType").asText()).isEqualTo("tools");

      var tools = StreamSupport.stream(root.get("tools").spliterator(), false).toList();
      var ids = tools.stream().map(n -> n.get("id").asText()).toList();
      assertThat(ids).contains("olo-core:http-tool", "olo-core:calculator");
      tools.forEach(
          tool ->
              assertThat(tool.has("implementationClass"))
                  .as("catalog must not embed JVM bindings")
                  .isFalse());

      var webSearch =
          tools.stream()
              .filter(n -> "olo-core:web-search".equals(n.get("id").asText()))
              .findFirst()
              .orElseThrow();
      assertThat(webSearch.has("capability")).isFalse();
      assertThat(webSearch.get("contract").get("inputSchema").get("type").asText()).isEqualTo("object");
      assertThat(webSearch.get("contract").get("outputSchema").get("type").asText()).isEqualTo("object");

      var http =
          tools.stream()
              .filter(t -> "olo-core:http-tool".equals(t.get("id").asText()))
              .findFirst()
              .orElseThrow();
      assertThat(http.get("runtime").get("contractVersion").asText()).isEqualTo("1.0");
      assertThat(http.get("runtime").has("apiVersion")).isFalse();
      assertThat(http.get("runtime").get("executionModel").asText()).isEqualTo("ACTIVITY");
      var httpCapabilities = http.get("runtime").get("capabilities");
      assertThat(httpCapabilities.isArray()).isTrue();
      assertThat(httpCapabilities)
          .extracting(JsonNode::asText)
          .containsExactly("CHECKPOINT", "RETRY", "TIMEOUT");
      assertThat(http.get("runtime").has("retryable")).isFalse();
      assertThat(http.get("runtime").has("deterministic")).isFalse();
      assertThat(http.get("runtime").has("longRunning")).isFalse();
      assertThat(http.get("runtime").get("defaultTimeout").asText()).isEqualTo("PT30S");
      assertThat(http.get("runtime").get("defaultRetryPolicy").asText()).isEqualTo("STANDARD");
      tools.forEach(
          tool ->
              assertThat(tool.get("emoji").asText())
                  .as("tool %s should have emoji for Studio UI", tool.get("id").asText())
                  .isNotBlank());

      var parameters = http.get("parameters");
      assertThat(parameters.isArray()).isTrue();

      var bodyField =
          StreamSupport.stream(parameters.spliterator(), false)
              .filter(n -> "body".equals(n.get("id").asText()))
              .findFirst()
              .orElseThrow();
      assertThat(bodyField.get("visibleWhen").get("method").asText()).isEqualTo("POST");

      var urlField =
          StreamSupport.stream(parameters.spliterator(), false)
              .filter(n -> "url".equals(n.get("id").asText()))
              .findFirst()
              .orElseThrow();
      assertThat(urlField.get("required").asBoolean()).isTrue();
      assertThat(urlField.get("validation").get("minLength").asInt()).isEqualTo(8);
      assertThat(urlField.get("validation").get("maxLength").asInt()).isEqualTo(2048);

      var methodField =
          StreamSupport.stream(parameters.spliterator(), false)
              .filter(n -> "method".equals(n.get("id").asText()))
              .findFirst()
              .orElseThrow();
      assertThat(methodField.get("type").asText()).isEqualTo("enum");
      assertThat(methodField.get("values"))
          .extracting(JsonNode::asText)
          .containsExactly("GET", "POST");
      assertThat(methodField.has("enumValues")).isFalse();
      assertThat(methodField.get("required").asBoolean()).isFalse();
      assertThat(bodyField.get("required").asBoolean()).isFalse();
    }
  }
}
