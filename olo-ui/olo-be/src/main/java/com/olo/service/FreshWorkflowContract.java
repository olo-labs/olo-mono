/* Copyright 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
package com.olo.service;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;
final class FreshWorkflowContract {
 private static final Set<String> RETIRED=Set.of("tools","toolCalls","tool_requirements","toolRequirements","toolCallPlanner","selectedTools","injectCapabilities","availableToolsVariable","dynamicToolExecution","aggregateToolResult","toolSynthesis","orchestration");
 static void validate(JsonNode node) {
  if(node.isObject()) node.fields().forEachRemaining(entry->{
   if(RETIRED.contains(entry.getKey())) throw new IllegalArgumentException("Unsupported workflow field: "+entry.getKey());
   if(entry.getKey().equals("type") && entry.getValue().asText().strip().equalsIgnoreCase("TOOL")) throw new IllegalArgumentException("Unsupported workflow node type: TOOL");
   validate(entry.getValue());
  });
  else if(node.isArray()) node.forEach(FreshWorkflowContract::validate);
  else if(node.isTextual() && node.asText().contains("{availableToolsJson}")) throw new IllegalArgumentException("Unsupported workflow placeholder: availableToolsJson");
 }
}
