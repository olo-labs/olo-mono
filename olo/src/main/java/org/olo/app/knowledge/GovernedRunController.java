/* Copyright 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
package org.olo.app.knowledge;
import com.fasterxml.jackson.databind.JsonNode;
import org.olo.temporal.sdk.TemporalClient;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/** UI polling always passes current user authorization and source validation. No raw event output. */
@RestController
@RequestMapping("/api/v1/runs")
public final class GovernedRunController {
 private final ConversationDirectory directory;
 private final PlatformKnowledge knowledge;
 private final TemporalClient temporal;
 private final KnowledgeController conversations;
 public GovernedRunController(ConversationDirectory directory, PlatformKnowledge knowledge, TemporalClient temporal, KnowledgeController conversations) {
  this.directory=directory; this.knowledge=knowledge; this.temporal=temporal; this.conversations=conversations;
 }
 private String workflowId(UUID turn) {
  return org.olo.input.codec.TenantWorkflowIds.run(org.olo.app.security.CurrentPrincipal.require().tenantId(),turn.toString());
 }
 private JsonNode read(UUID turn) {
  return knowledge.read(directory.conversation(turn),Map.of("type","turn","turnId",turn.toString()));
 }
 @GetMapping("/{turn}") public Map<String,Object> status(@PathVariable UUID turn) {
  read(turn);
  return Map.of("runId",turn.toString(),"status",temporal.chatWorkflowStatus(workflowId(turn)));
 }
 @GetMapping("/{turn}/response") public Map<String,Object> response(@PathVariable UUID turn) {
  var history=read(turn); String response="";
  for(var message:history.path("messages")) if("assistant".equals(message.path("role").asText())) response=message.path("content").asText();
  return Map.of("runId",turn.toString(),"response",response);
 }
 public record HumanInput(boolean approved, String message, String historyText) {}
 @PostMapping("/{turn}/human-input") public void human(@PathVariable UUID turn, @RequestBody HumanInput input) {
  read(turn);
  String conversation=directory.conversation(turn);
  knowledge.call(UUID.randomUUID().toString(),"memory.append",conversation,Map.of("type","check"));
  temporal.signalHumanInput(workflowId(turn),input.approved(),input.message());
 }
 @PostMapping("/{turn}/cancel") public Map<String,Object> cancel(@PathVariable UUID turn, @RequestHeader("Idempotency-Key") UUID key) {
  String conversation=directory.conversation(turn);
  return conversations.cancel(UUID.fromString(conversation),turn,key);
 }
}
