/* Copyright 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
package org.olo.app.knowledge;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.olo.temporal.sdk.TemporalClient;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class GovernedRunControllerTest {
 @Test void refusalStopsStatusResponseAndCancellation() {
  var directory=mock(ConversationDirectory.class);var knowledge=mock(PlatformKnowledge.class);var temporal=mock(TemporalClient.class);var conversations=mock(KnowledgeController.class);
  var turn=UUID.randomUUID();when(directory.conversation(turn)).thenReturn(UUID.randomUUID().toString());
  when(knowledge.read(anyString(),anyMap())).thenThrow(new SecurityException("revoked"));
  var api=new GovernedRunController(directory,knowledge,temporal,conversations);
  assertThrows(SecurityException.class,()->api.status(turn));
  assertThrows(SecurityException.class,()->api.response(turn));
  assertThrows(SecurityException.class,()->api.human(turn,new GovernedRunController.HumanInput(true,"yes",null)));
  verifyNoInteractions(temporal,conversations);
 }
 @Test void responseComesOnlyFromGovernedTurnHistory() throws Exception {
  var directory=mock(ConversationDirectory.class);var knowledge=mock(PlatformKnowledge.class);var temporal=mock(TemporalClient.class);
  var turn=UUID.randomUUID();String conversation=UUID.randomUUID().toString();when(directory.conversation(turn)).thenReturn(conversation);
  when(knowledge.read(eq(conversation),anyMap())).thenReturn(new ObjectMapper().readTree("{\"messages\":[{\"role\":\"user\",\"content\":\"private request\"},{\"role\":\"assistant\",\"content\":\"governed answer\"}]}"));
  var api=new GovernedRunController(directory,knowledge,temporal,mock(KnowledgeController.class));
  assertEquals("governed answer",api.response(turn).get("response"));
  verify(knowledge).read(conversation,Map.of("type","turn","turnId",turn.toString()));
  verifyNoInteractions(temporal);
 }
 @Test void directoryScopesReferencesByIssuerTenantAndUserWithoutAmbiguity() {
  var keys=Set.of(ConversationDirectory.ownerKey("i","t","u"),ConversationDirectory.ownerKey("i2","t","u"),ConversationDirectory.ownerKey("i","t2","u"),ConversationDirectory.ownerKey("i","t","u2"),ConversationDirectory.ownerKey("it","","u"));
  assertEquals(5,keys.size());
 }
}
