// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.knowledge.testing;

import com.fasterxml.jackson.databind.JsonNode;
import org.olo.knowledge.core.Json;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.LongSupplier;

/** Scripted Control protocol peer; real Control authorization/RLS is qualified in ToolGate's own suite. */
public final class ScriptedClaims {
    public record Reply(int status,Object body) {}
    private static final class Entry {
        final String effect,token;final long generation;
        String state="CONSUMED",result;
        Entry(JsonNode claims,String token){effect=claims.path("effectId").asText();generation=claims.path("authorizationGeneration").asLong();this.token=token;}
    }
    private final Set<String> consumed;
    private final LongSupplier epoch;
    private final Map<String,Entry> entries=new HashMap<>();
    public volatile boolean outcomesAvailable=true,consumptionAvailable=true,forceOtherClaim,corruptGeneration;
    public volatile String dropOutcomeNonce;
    public final AtomicBoolean dropOutcomeAcknowledgement=new AtomicBoolean();
    public final AtomicInteger outcomeRequests=new AtomicInteger();
    public volatile CountDownLatch consumedPause,consumedRelease;
    public ScriptedClaims(Set<String> consumed,LongSupplier epoch){this.consumed=consumed;this.epoch=epoch;}
    private Object result(Entry e,String token){
        return Map.of("effectId",e.effect,"authorizationGeneration",e.generation+(corruptGeneration?1:0),
            "status",e.token.equals(token)?e.state:"CONSUMED_BY_OTHER_CLAIM");
    }
    public Reply handle(String path,JsonNode submitted)throws java.io.IOException{
        Reply reply;
        synchronized(this){
            if(path.endsWith("/consume")){
                if(!consumptionAvailable)return new Reply(503,Map.of());
                var request=submitted.path("request");String token=submitted.path("claimToken").asText();
                var claims=Json.MAPPER.readTree(Base64.getUrlDecoder().decode(request.path("permit").path("jws").asText().split("\\.")[1]));
                if(!token.matches("[a-f0-9]{64}")||claims.path("authorizationEpoch").asLong()!=epoch.getAsLong()
                    ||!request.path("httpRequestDigest").asText().equals(claims.path("httpRequestDigest").asText())
                    ||!request.path("requestFingerprint").asText().equals(claims.path("requestFingerprint").asText())
                    ||!request.path("executorKeySha256").asText().equals(claims.path("executorKeySha256").asText()))
                    return new Reply(403,Map.of());
                String nonce=claims.path("nonce").asText();
                var entry=entries.get(nonce);
                if(entry==null){entry=new Entry(claims,forceOtherClaim?"0".repeat(64):token);entries.put(nonce,entry);consumed.add(nonce);}
                reply=new Reply(200,result(entry,token));
            }else{
                var reference=path.endsWith("/outcome")?submitted.path("claim"):submitted;
                String nonce=reference.path("authNonce").asText(),token=reference.path("claimToken").asText();
                var entry=entries.get(nonce);if(entry==null)return new Reply(403,Map.of());
                if(path.endsWith("/outcome")){
                    outcomeRequests.incrementAndGet();
                    if(!outcomesAvailable)return new Reply(503,Map.of());
                    if(!entry.token.equals(token))return new Reply(403,Map.of());
                    String outcome=submitted.path("outcome").asText(),digest=submitted.path("resultDigest").asText();
                    if(!Set.of("COMPLETED","FAILED").contains(outcome)||!digest.matches("sha256:[a-f0-9]{64}"))
                        return new Reply(403,Map.of());
                    if(entry.result!=null&&(!entry.result.equals(digest)||!entry.state.equals(outcome)))entry.state="CONFLICT";
                    else{entry.state=outcome;entry.result=digest;}
                    if((dropOutcomeNonce==null||nonce.equals(dropOutcomeNonce))&&dropOutcomeAcknowledgement.compareAndSet(true,false))return new Reply(0,Map.of());
                }
                reply=new Reply(200,result(entry,token));
            }
        }
        if(path.endsWith("/consume")){
            var pause=consumedPause;var release=consumedRelease;
            if(pause!=null&&release!=null){
                pause.countDown();
                try{if(!release.await(2,TimeUnit.SECONDS))throw new java.io.IOException("Fixture release timed out");}
                catch(InterruptedException stopped){Thread.currentThread().interrupt();throw new java.io.IOException("Fixture interrupted",stopped);}
            }
        }
        return reply;
    }
    public synchronized String state(String nonce){var entry=entries.get(nonce);return entry==null?"ABSENT":entry.state;}
}
