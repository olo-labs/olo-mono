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
      assertThat(ids).contains("http-tool", "calculator");
      tools.forEach(
          tool ->
              assertThat(tool.get("emoji").asText())
                  .as("tool %s should have emoji for Studio UI", tool.get("id").asText())
                  .isNotBlank());
    }
  }
}
