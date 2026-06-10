package org.olo.core.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.olo.annotation.OloCatalogLocations;

import java.io.InputStream;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

class NodeCatalogGenerationTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  void compilesAuthoritativeNodesCatalog() throws Exception {
    try (InputStream in =
        getClass().getClassLoader().getResourceAsStream(OloCatalogLocations.NODES_CATALOG)) {
      assertThat(in).as("annotation processor should emit %s", OloCatalogLocations.NODES_CATALOG)
          .isNotNull();

      JsonNode root = MAPPER.readTree(in);
      assertThat(root.get("schemaVersion").asText()).isEqualTo("1.0");
      assertThat(root.get("moduleId").asText()).isEqualTo("olo-core-nodes");
      assertThat(root.get("catalogType").asText()).isEqualTo("nodes");

      var nodes = StreamSupport.stream(root.get("nodes").spliterator(), false).toList();
      var ids = nodes.stream().map(n -> n.get("id").asText()).toList();
      assertThat(ids).contains("PROMPT", "AGENT", "APPROVAL");
      nodes.forEach(
          node ->
              assertThat(node.get("emoji").asText())
                  .as("node %s should have emoji for Studio UI", node.get("id").asText())
                  .isNotBlank());
    }
  }
}
