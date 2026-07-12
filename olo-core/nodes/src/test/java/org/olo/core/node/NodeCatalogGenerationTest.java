/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.olo.annotation.OloCatalogLocations;
import org.olo.spi.runtime.RuntimeCapabilities;

import java.io.InputStream;
import java.util.List;
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
      assertThat(ids).contains("olo-core:PROMPT", "olo-core:AGENT", "olo-core:APPROVAL");
      nodes.forEach(
          node ->
              assertThat(node.has("implementationClass"))
                  .as("catalog must not embed JVM bindings")
                  .isFalse());

      var prompt =
          nodes.stream()
              .filter(n -> "olo-core:PROMPT".equals(n.get("id").asText()))
              .findFirst()
              .orElseThrow();
      assertThat(prompt.get("runtime").get("executionModel").asText()).isEqualTo("INLINE");
      assertThat(prompt.get("runtime").has("capabilities")).isFalse();

      var agent =
          nodes.stream()
              .filter(n -> "olo-core:AGENT".equals(n.get("id").asText()))
              .findFirst()
              .orElseThrow();
      assertThat(agent.get("designer").get("paletteGroup").asText()).isEqualTo("Agents");
      assertThat(agent.get("designer").get("nodeSize").get("width").asInt()).isEqualTo(300);
      assertThat(agent.get("designer").get("nodeSize").get("height").asInt()).isEqualTo(120);
      assertThat(agent.get("designer").has("resizable")).isFalse();
      assertThat(agent.get("designer").has("draggable")).isFalse();
      assertThat(agent.get("inputs").get(0).get("ui").get("position").asText()).isEqualTo("LEFT");
      assertThat(agent.get("outputs").get(0).get("ui").get("position").asText()).isEqualTo("RIGHT");
      assertThat(prompt.get("designer").get("paletteGroup").asText()).isEqualTo("Llm");
      assertThat(prompt.get("designer").has("nodeSize")).isFalse();
      assertThat(prompt.get("designer").has("resizable")).isFalse();
      assertThat(prompt.get("outputs").get(0).get("schema").asText()).isEqualTo("string");
      var promptConfig = prompt.get("parameters").get(0);
      assertThat(promptConfig.get("type").asText()).isEqualTo("string");
      assertThat(promptConfig.get("ui").get("widget").asText()).isEqualTo("TEXTAREA");
      assertThat(promptConfig.get("required").asBoolean()).isFalse();

      var loopNode =
          nodes.stream()
              .filter(n -> "olo-core:LOOP".equals(n.get("id").asText()))
              .findFirst()
              .orElseThrow();
      var maxIterations =
          StreamSupport.stream(loopNode.get("parameters").spliterator(), false)
              .filter(p -> "maxIterations".equals(p.get("id").asText()))
              .findFirst()
              .orElseThrow();
      assertThat(maxIterations.get("type").asText()).isEqualTo("number");
      assertThat(maxIterations.get("defaultValue").isNumber()).isTrue();
      assertThat(maxIterations.get("defaultValue").asInt()).isEqualTo(1);

      var parallelNode =
          nodes.stream()
              .filter(n -> "olo-core:PARALLEL".equals(n.get("id").asText()))
              .findFirst()
              .orElseThrow();
      var branches =
          StreamSupport.stream(parallelNode.get("parameters").spliterator(), false)
              .filter(p -> "branches".equals(p.get("id").asText()))
              .findFirst()
              .orElseThrow();
      assertThat(branches.get("type").asText()).isEqualTo("number");
      assertThat(branches.get("defaultValue").isNumber()).isTrue();
      assertThat(branches.get("defaultValue").asInt()).isEqualTo(2);

      var switchNode =
          nodes.stream()
              .filter(n -> "olo-core:SWITCH".equals(n.get("id").asText()))
              .findFirst()
              .orElseThrow();
      assertThat(switchNode.get("connectionPolicy").get("maxInputs").asInt()).isEqualTo(1);
      assertThat(switchNode.get("connectionPolicy").get("maxOutputs").asInt()).isEqualTo(-1);
      assertThat(promptConfig.get("id").asText()).isEqualTo("prompt");
      assertThat(promptConfig.get("label").asText()).isEqualTo("Prompt Template");
      assertThat(agent.get("runtime").get("executionModel").asText()).isEqualTo("CHILD_WORKFLOW");
      assertThat(agent.get("runtime").get("capabilities"))
          .extracting(JsonNode::asText)
          .containsExactly("TIMEOUT");
      nodes.forEach(
          node -> {
            var runtime = node.get("runtime");
            assertThat(runtime.get("executionModel").asText()).isNotBlank();
            var declared = runtime.has("capabilities")
                    ? runtime.get("capabilities")
                    : null;
            List<String> effective = declared == null
                    ? RuntimeCapabilities.inheritedCatalogDefaultNames()
                    : RuntimeCapabilities.resolveEffectiveNames(
                            java.util.stream.StreamSupport.stream(declared.spliterator(), false)
                                    .map(JsonNode::asText)
                                    .toList());
            assertThat(effective).contains("DEBUG", "REPLAY");
          });
      nodes.forEach(
          node ->
              assertThat(node.get("emoji").asText())
                  .as("node %s should have emoji for Studio UI", node.get("id").asText())
                  .isNotBlank());
    }
  }
}
