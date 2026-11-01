// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.knowledge.core;

import io.ololabs.toolgate.contracts.ServiceAudience;
import io.ololabs.toolgate.service.*;
import org.olo.knowledge.testing.PostgresFixture;
import org.junit.jupiter.api.Test;
import java.net.URI;
import java.net.http.HttpClient;
import java.sql.*;
import java.time.Clock;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class FencedEffectsTest {
    private ControlServiceAuthorization control(){
        return new ControlServiceAuthorization(HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build(),
            URI.create("https://control.invalid"),"fixture-control",PostgresFixture.TENANT,ServiceAudience.olo_memory,
            ()->"fixture-only",Clock.systemUTC());
    }
    @Test void runtimeStartupRequiresRlsAndRestrictedClaimPermissions()throws Exception{
        var db=PostgresFixture.create("fencing_privileges",false);
        try(var verified=new FencedEffects(db,control(),PostgresFixture.TENANT)){}
        var owner=new Database(PostgresFixture.url(db),"postgres",System.getenv("KNOWLEDGE_TEST_PASSWORD"));
        assertThrows(IllegalStateException.class,()->new FencedEffects(owner,control(),PostgresFixture.TENANT));
    }
    @Test void tenantRowsBindingsTombstonesAndStateTransitionsAreGuardedInSql()throws Exception{
        var db=PostgresFixture.create("fencing_rows",false);var store=new JdbcEffectClaims();
        var binding=new JdbcEffectClaims.Binding(PostgresFixture.TENANT,"effect-"+UUID.randomUUID(),1,"auth-"+UUID.randomUUID(),"sha256:"+"a".repeat(64));
        try(var c=db.open(binding.tenant())){
            var owner=store.claim(c,binding,System.currentTimeMillis()+15000).owner();
            assertNotNull(owner);assertTrue(store.beginApply(c,owner));assertTrue(store.finish(c,owner,true,"sha256:"+"b".repeat(64)));c.commit();
        }
        try(var c=db.open(PostgresFixture.OTHER_TENANT)){
            assertNull(store.inspect(c,binding.tenant(),binding.effectId()));
            assertTrue(store.pendingOutcomes(c,binding.tenant(),10).isEmpty());c.commit();
        }
        for(String sql:List.of("DELETE FROM toolgate_target_claims","DELETE FROM toolgate_target_outcomes",
            "UPDATE toolgate_target_claims SET auth_nonce='changed'",
            "UPDATE toolgate_target_claims SET state='CLAIMED',result_digest=NULL WHERE state='DONE'")){
            assertThrows(SQLException.class,()->{try(var c=db.open(binding.tenant())){Database.update(c,sql);c.commit();}});
        }
        try(var c=db.open(binding.tenant())){
            assertEquals(JdbcEffectClaims.State.DONE,store.inspect(c,binding.tenant(),binding.effectId()).state());
            var pending=store.pendingOutcome(c,binding);assertNotNull(pending);
            assertTrue(store.acknowledgeOutcome(c,pending));c.commit();
        }
    }
    @Test void missingFencingMigrationRefusesStartup()throws Exception{
        var db=PostgresFixture.create("fencing_missing",false);
        try(var c=DriverManager.getConnection(PostgresFixture.url(db),"postgres",System.getenv("KNOWLEDGE_TEST_PASSWORD"));var q=c.createStatement()){
            q.execute("DROP TABLE toolgate_target_outcomes");q.execute("DROP TABLE toolgate_target_claims");
        }
        var failure=assertThrows(SQLException.class,()->new FencedEffects(db,control(),PostgresFixture.TENANT));
        assertEquals("42P01",failure.getSQLState());
    }
}
