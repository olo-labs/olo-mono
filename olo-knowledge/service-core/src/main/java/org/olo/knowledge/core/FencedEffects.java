// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.knowledge.core;

import io.ololabs.toolgate.service.*;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Trusted target orchestration. SQL status and outcome delivery never authorize another business action. */
public final class FencedEffects implements AutoCloseable {
    private final Database db;
    private final ControlServiceAuthorization control;
    private final String tenant;
    private final JdbcEffectClaims claims=new JdbcEffectClaims();
    private final AtomicBoolean queued=new AtomicBoolean();
    private volatile ScheduledExecutorService worker;
    private String cursor;

    public FencedEffects(Database db,ControlServiceAuthorization control,String tenant)throws Exception {
        this.db=Objects.requireNonNull(db);this.control=Objects.requireNonNull(control);this.tenant=Objects.requireNonNull(tenant);
        verifySchema();
    }
    private void verifySchema()throws Exception{
        try(var c=db.open(tenant)){
            var guarded=Database.rows(c,"SELECT count(*) AS guarded FROM pg_class WHERE oid IN ('toolgate_target_claims'::regclass,'toolgate_target_outcomes'::regclass) AND relrowsecurity AND relforcerowsecurity");
            if(((Number)guarded.getFirst().get("guarded")).intValue()!=2)throw new IllegalStateException("Tenant fencing schema required");
            var unsafe=Database.rows(c,"""
                SELECT has_table_privilege(current_user,'toolgate_target_claims','DELETE')
                  OR has_table_privilege(current_user,'toolgate_target_outcomes','DELETE')
                  OR EXISTS(SELECT 1 FROM unnest(ARRAY['tenant_id','effect_id','authorization_generation','auth_nonce','request_digest','claim_token','authorization_expires_at']) col(name)
                    WHERE has_column_privilege(current_user,'toolgate_target_claims',col.name,'UPDATE'))
                  OR EXISTS(SELECT 1 FROM unnest(ARRAY['tenant_id','effect_id','auth_nonce','claim_token','outcome','result_digest']) col(name)
                    WHERE has_column_privilege(current_user,'toolgate_target_outcomes',col.name,'UPDATE')) AS unsafe
                """);
            if(Boolean.TRUE.equals(unsafe.getFirst().get("unsafe")))throw new IllegalStateException("Restricted claim/outcome permissions required");
            var allowed=Database.rows(c,"""
                SELECT has_table_privilege(current_user,'toolgate_target_claims','INSERT')
                  AND has_table_privilege(current_user,'toolgate_target_outcomes','INSERT')
                  AND has_column_privilege(current_user,'toolgate_target_claims','state','UPDATE')
                  AND has_column_privilege(current_user,'toolgate_target_claims','result_digest','UPDATE')
                  AND has_column_privilege(current_user,'toolgate_target_claims','ownership_generation','UPDATE')
                  AND has_column_privilege(current_user,'toolgate_target_outcomes','delivered','UPDATE') AS allowed
                """);
            if(!Boolean.TRUE.equals(allowed.getFirst().get("allowed")))throw new IllegalStateException("Claim transition permissions required");
            claims.expiredClaims(c,tenant,System.currentTimeMillis(),1);claims.pendingOutcomes(c,tenant,1);c.commit();
        }
    }
    public Write begin(ServiceRequest request)throws Exception{
        var verified=control.verifyWrite(request.permit(),request.evidence(),request.peer());
        if(!tenant.equals(verified.binding().tenant()))throw new SecurityException("Target tenant mismatch");
        JdbcEffectClaims.Acquisition acquired;
        try(var c=db.open(tenant)){acquired=claims.claim(c,verified.binding(),verified.claims().exp());c.commit();}
        if(!acquired.acquired())throw new Existing(acquired.snapshot());
        var authorization=control.consumeClaim(verified,acquired.owner());
        try(var c=db.open(tenant)){
            if(!claims.beginApply(c,acquired.owner()))throw new Existing(claims.inspect(c,tenant,verified.binding().effectId()));
            Database.commit(c,authorization);
        }
        authorization.checkCommitDeadline();
        return new Write(authorization,acquired.owner());
    }
    public final class Write {
        private final ServicePermitVerifier.Authorization authorization;
        private final JdbcEffectClaims.Owner owner;
        private Write(ServicePermitVerifier.Authorization authorization,JdbcEffectClaims.Owner owner){this.authorization=authorization;this.owner=owner;}
        public ServicePermitVerifier.Authorization authorization(){return authorization;}
        /** Called in the same transaction as receipts and the business mutation. */
        public void finish(Connection c,Object result)throws Exception{
            byte[] bytes=Json.MAPPER.writeValueAsBytes(result);
            String digest="sha256:"+HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
            if(!claims.finish(c,owner,true,digest))throw new SecurityException("Effect claim no longer applying");
        }
        /** Signal only after business commit; the outbox survives a missing signal or process crash. */
        public void committed(){signal();}
    }
    public static final class Existing extends Exception {
        private final JdbcEffectClaims.Snapshot snapshot;
        private Existing(JdbcEffectClaims.Snapshot snapshot){super("Effect already claimed");this.snapshot=Objects.requireNonNull(snapshot);}
        public Map<String,Object> status(){return Map.of("effectId",snapshot.binding().effectId(),"state",snapshot.state().name());}
    }
    public synchronized void start(){
        if(worker!=null)throw new IllegalStateException("Recovery already started");
        worker=Executors.newSingleThreadScheduledExecutor();
        worker.scheduleWithFixedDelay(this::background,1,5,TimeUnit.SECONDS);
    }
    private void signal(){
        var executor=worker;
        if(executor!=null&&!executor.isShutdown()&&queued.compareAndSet(false,true)){
            try{executor.execute(()->{try{background();}finally{queued.set(false);}});}
            catch(RejectedExecutionException stopped){queued.set(false);}
        }
    }
    private void background(){
        try{drain();}catch(Exception deferred){
            System.getLogger(FencedEffects.class.getName()).log(System.Logger.Level.WARNING,"target_recovery_deferred");
        }
    }
    /** Bounded passes with keyset rotation; failed deliveries never retry a business mutation. */
    public synchronized void drain()throws Exception{
        List<JdbcEffectClaims.PendingOutcome> pending;
        try(var c=db.open(tenant)){
            for(var expired:claims.expiredClaims(c,tenant,System.currentTimeMillis(),10))claims.abandon(c,expired);
            pending=claims.pendingOutcomes(c,tenant,cursor,10);
            if(pending.isEmpty()&&cursor!=null){cursor=null;pending=claims.pendingOutcomes(c,tenant,null,10);}
            c.commit();
        }
        boolean deferred=false;
        for(var outcome:pending){
            try{
                control.reportOutcome(outcome.binding(),outcome.claimToken(),outcome.outcome(),outcome.resultDigest());
                try(var c=db.open(tenant)){claims.acknowledgeOutcome(c,outcome);c.commit();}
            }catch(Exception unavailable){deferred=true;}
            cursor=outcome.binding().effectId();
        }
        if(deferred)System.getLogger(FencedEffects.class.getName()).log(System.Logger.Level.WARNING,"target_outcome_delivery_deferred");
    }
    @Override public void close(){var executor=worker;if(executor!=null)executor.shutdownNow();}
}
