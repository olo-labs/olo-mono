/* Copyright 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
package com.olo.service;
import com.olo.config.OloConfigurationProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.assertj.core.api.Assertions.*;
class GraphLogServiceTest {
 @TempDir Path root;
 private GraphLogService service() { var p = new OloConfigurationProperties(); p.setLogDirectory(root.toString()); return new GraphLogService(p); }
 @Test void exposesOnlyStructuralMetadata() throws Exception {
  Files.writeString(root.resolve("entry.json"), """
   {"workflowId":"agent","kind":"agent-call","nodeCount":5,"edgeCount":4,"sourceJson":"PRIVATE_CANARY"}
   """);
  var entries=service().listLogs();
  assertThat(entries).hasSize(1);
  assertThat(entries.get(0).nodeCount()).isEqualTo(5);
  assertThat(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(service().readMetadata("entry.json"))).doesNotContain("PRIVATE_CANARY", "sourceJson");
 }
 @Test void rejectsOldGraphPayloadAndTraversal() throws Exception {
  Files.writeString(root.resolve("old.json"), "{\"mergedGraph\":{\"id\":\"agent\"}}");
  assertThat(service().listLogs()).isEmpty();
  assertThatThrownBy(() -> service().readMetadata("old.json")).isInstanceOf(IllegalArgumentException.class);
  assertThatThrownBy(() -> service().readMetadata("../outside.json")).isInstanceOf(IllegalArgumentException.class);
 }
}
