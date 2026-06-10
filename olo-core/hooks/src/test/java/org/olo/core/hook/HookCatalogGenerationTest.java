package org.olo.core.hook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.olo.annotation.OloCatalogLocations;

import java.io.InputStream;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

class HookCatalogGenerationTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  void compilesAuthoritativeHooksCatalog() throws Exception {
    try (InputStream in =
        getClass().getClassLoader().getResourceAsStream(OloCatalogLocations.HOOKS_CATALOG)) {
      assertThat(in).as("annotation processor should emit %s", OloCatalogLocations.HOOKS_CATALOG)
          .isNotNull();

      JsonNode root = MAPPER.readTree(in);
      assertThat(root.get("schemaVersion").asText()).isEqualTo("1.0");
      assertThat(root.get("moduleId").asText()).isEqualTo("olo-core-hooks");
      assertThat(root.get("catalogType").asText()).isEqualTo("hooks");

      var hooks = StreamSupport.stream(root.get("hooks").spliterator(), false).toList();
      var ids = hooks.stream().map(n -> n.get("id").asText()).toList();
      assertThat(ids).contains("logging-hook", "metrics-hook", "tracing-hook");
      hooks.forEach(
          hook ->
              assertThat(hook.get("emoji").asText())
                  .as("hook %s should have emoji for Studio UI", hook.get("id").asText())
                  .isNotBlank());
    }
  }
}
