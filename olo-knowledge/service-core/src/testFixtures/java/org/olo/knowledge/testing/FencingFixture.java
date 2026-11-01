// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.knowledge.testing;

import org.olo.knowledge.core.*;
import io.ololabs.toolgate.service.JdbcEffectClaims;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real process/TLS/database conformance helpers. No production fault switches are used. */
public final class FencingFixture {
    public record Replica(Process process,URI endpoint) implements AutoCloseable {
        @Override public void close()throws Exception{
            process.destroy();if(!process.waitFor(5,TimeUnit.SECONDS))process.destroyForcibly().waitFor();
        }
    }
    public static Replica replica(ProcessBuilder template,HttpClient http,String operation,Path log)throws Exception{
        int port;try(var socket=new java.net.ServerSocket(0)){port=socket.getLocalPort();}
        var builder=new ProcessBuilder(new ArrayList<>(template.command()));
        builder.environment().clear();builder.environment().putAll(template.environment());builder.environment().put("PORT",Integer.toString(port));
        builder.redirectErrorStream(true).redirectOutput(log.toFile());
        var replica=new Replica(builder.start(),URI.create("https://localhost:"+port+"/v1/"));
        try{
            for(int n=0;n<80;n++){
                try{
                    var request=HttpRequest.newBuilder(replica.endpoint().resolve(operation)).timeout(Duration.ofSeconds(1)).GET().build();
                    if(http.send(request,HttpResponse.BodyHandlers.discarding()).statusCode()==405)return replica;
                }catch(java.io.IOException starting){}
                Thread.sleep(100);
            }
            throw new AssertionError("Replica did not become ready: "+log.getFileName());
        }catch(Exception|AssertionError failed){replica.close();throw failed;}
    }
    @FunctionalInterface public interface Send { HttpResponse<String> call(URI endpoint,String permit)throws Exception; }
    public static void pausedOwner(Database db,String tenant,URI endpoint,String permit,ScriptedClaims control,Send send)throws Exception{
        var payload=Json.MAPPER.readTree(Base64.getUrlDecoder().decode(permit.split("\\.")[1]));
        var accepted=new CountDownLatch(1);var release=new CountDownLatch(1);
        control.consumedPause=accepted;control.consumedRelease=release;
        var pool=Executors.newSingleThreadExecutor();
        try{
            var response=pool.submit(()->send.call(endpoint,permit));
            assertTrue(accepted.await(2,TimeUnit.SECONDS),"Claim consumption did not reach the barrier");
            try(var c=db.open(tenant)){
                var store=new JdbcEffectClaims();var snapshot=store.inspect(c,tenant,payload.path("effectId").asText());
                assertNotNull(snapshot);assertEquals(JdbcEffectClaims.State.CLAIMED,snapshot.state());
                assertTrue(store.abandon(c,snapshot));c.commit();
            }
            release.countDown();var refused=response.get(10,TimeUnit.SECONDS);
            assertEquals(409,refused.statusCode());
            assertEquals("ABANDONED",Json.MAPPER.readTree(refused.body()).path("state").asText());
        }finally{
            release.countDown();control.consumedPause=null;control.consumedRelease=null;pool.shutdownNow();
        }
    }
    public static List<HttpResponse<String>> race(URI first,URI second,String permit,Send send)throws Exception{
        var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try{
            var a=pool.submit(()->{start.await();return send.call(first,permit);});
            var b=pool.submit(()->{start.await();return send.call(second,permit);});
            start.countDown();return List.of(a.get(12,TimeUnit.SECONDS),b.get(12,TimeUnit.SECONDS));
        }finally{pool.shutdownNow();}
    }
    public static void recoveredOutcome(Database db,String tenant,String permit,ScriptedClaims control)throws Exception{
        var payload=Json.MAPPER.readTree(Base64.getUrlDecoder().decode(permit.split("\\.")[1]));
        String effect=payload.path("effectId").asText(),nonce=payload.path("nonce").asText();
        control.dropOutcomeNonce=nonce;control.dropOutcomeAcknowledgement.set(true);control.outcomesAvailable=true;
        long deadline=System.nanoTime()+Duration.ofSeconds(25).toNanos();
        boolean delivered=false;
        while(System.nanoTime()<deadline){
            try(var c=db.open(tenant)){
                var rows=Database.rows(c,"SELECT delivered FROM toolgate_target_outcomes WHERE tenant_id=? AND effect_id=?",tenant,effect);
                delivered=!rows.isEmpty()&&((Number)rows.getFirst().get("delivered")).intValue()==1;
                c.commit();
            }
            if(delivered)break;Thread.sleep(100);
        }
        assertTrue(delivered,"Surviving replica must deliver the durable outcome after process loss");
        assertEquals("COMPLETED",control.state(nonce));assertFalse(control.dropOutcomeAcknowledgement.get());
    }
    public static void oneMutationAndGuardedRecords(Database db,String tenant,String table)throws Exception{
        if(!Set.of("memory_message","rag_document").contains(table))throw new IllegalArgumentException("Fixture table required");
        try(var c=db.open(tenant)){
            assertEquals(1,((Number)Database.rows(c,"SELECT count(*) AS total FROM "+table+" WHERE tenant=?",tenant).getFirst().get("total")).intValue());c.commit();
        }
        try(var c=db.open(UUID.randomUUID().toString())){
            assertTrue(Database.rows(c,"SELECT * FROM toolgate_target_claims").isEmpty());
            assertTrue(Database.rows(c,"SELECT * FROM toolgate_target_outcomes").isEmpty());c.commit();
        }
        for(String sql:List.of("DELETE FROM toolgate_target_claims","DELETE FROM toolgate_target_outcomes",
            "UPDATE toolgate_target_claims SET auth_nonce='changed'",
            "UPDATE toolgate_target_claims SET state='CLAIMED',result_digest=NULL WHERE state='DONE'")){
            assertThrows(java.sql.SQLException.class,()->{try(var c=db.open(tenant)){Database.update(c,sql);c.commit();}});
        }
    }
}
