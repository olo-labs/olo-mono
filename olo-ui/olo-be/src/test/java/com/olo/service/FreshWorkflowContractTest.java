/* Copyright 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
package com.olo.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class FreshWorkflowContractTest {
 @Test void rejectsRetiredFieldsWithoutEchoingPrivateValues() throws Exception {
  var mapper=new ObjectMapper();
  for(String field:new String[]{"tools","toolCalls","tool_requirements","toolCallPlanner","selectedTools","injectCapabilities","orchestration"}) {
   var document=mapper.createObjectNode();document.putObject("nested").put(field,"PRIVATE_CANARY");
   assertThatThrownBy(()->FreshWorkflowContract.validate(document)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining(field).hasMessageNotContaining("PRIVATE_CANARY");
  }
  assertThatThrownBy(()->FreshWorkflowContract.validate(mapper.readTree("{\"nodes\":[{\"type\":\" tool \"}]}"))).hasMessageContaining("TOOL");
 }
}
