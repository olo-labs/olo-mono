/* Copyright 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
package org.olo.app.knowledge;
import org.olo.app.security.CurrentPrincipal;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Durable opaque references only. Every use still requires a live ToolGate read. */
@Component
public final class ConversationDirectory {
 private final StringRedisTemplate redis;
 public ConversationDirectory(StringRedisTemplate redis) { this.redis = redis; }
 static String ownerKey(String issuer, String tenant, String user) {
  try {
   String canonical = issuer.length()+":"+issuer+tenant.length()+":"+tenant+user.length()+":"+user;
   return "olo:conversations:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
  } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
 }
 private String owner() { var p=CurrentPrincipal.require(); return ownerKey(p.issuer(),p.tenantId(),p.userId()); }
 public void record(UUID conversation) { redis.opsForZSet().add(owner(),conversation.toString(),System.currentTimeMillis()); }
 public List<String> list() { var ids=redis.opsForZSet().reverseRange(owner(),0,99); return ids==null?List.of():List.copyOf(ids); }
 public void recordKnowledge(UUID kb) { redis.opsForZSet().add(owner()+":knowledge",kb.toString(),System.currentTimeMillis()); }
 public List<String> knowledge() { var ids=redis.opsForZSet().reverseRange(owner()+":knowledge",0,99); return ids==null?List.of():List.copyOf(ids); }
 public void forget(UUID conversation) { redis.opsForZSet().remove(owner(),conversation.toString()); }
 public void recordTurn(UUID conversation, String turn) {
  record(conversation);
  redis.opsForHash().put(owner()+":turns",turn,conversation.toString());
 }
 public String conversation(UUID turn) {
  Object id=redis.opsForHash().get(owner()+":turns",turn.toString());
  if(id==null) throw new SecurityException("Turn unavailable");
  return UUID.fromString(id.toString()).toString();
 }
}
