-- Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
-- Fresh current schema: run as owner after common.sql in an empty target store.
-- Existing target tables are rejected; there is no upgrade or conversion path.
CREATE TABLE toolgate_target_claims (
 tenant_id varchar(128) NOT NULL, effect_id varchar(128) NOT NULL,
 authorization_generation bigint NOT NULL CHECK(authorization_generation>0),
 auth_nonce varchar(128) NOT NULL, request_digest varchar(71) NOT NULL,
 ownership_generation bigint NOT NULL CHECK(ownership_generation>0),
 claim_token varchar(64) NOT NULL,
 state varchar(16) NOT NULL CHECK(state IN ('CLAIMED','APPLYING','DONE','FAILED','ABANDONED')),
 result_digest varchar(71),
 authorization_expires_at bigint NOT NULL CHECK(authorization_expires_at>0),
 PRIMARY KEY(tenant_id,effect_id), UNIQUE(tenant_id,auth_nonce),
 CHECK((state IN ('DONE','FAILED') AND result_digest IS NOT NULL)
    OR (state NOT IN ('DONE','FAILED') AND result_digest IS NULL))
);
CREATE TABLE toolgate_target_outcomes (
 tenant_id varchar(128) NOT NULL, effect_id varchar(128) NOT NULL,
 auth_nonce varchar(128) NOT NULL, claim_token varchar(64) NOT NULL,
 outcome varchar(16) NOT NULL CHECK(outcome IN ('COMPLETED','FAILED')),
 result_digest varchar(71) NOT NULL, delivered integer NOT NULL DEFAULT 0 CHECK(delivered IN (0,1)),
 PRIMARY KEY(tenant_id,effect_id),
 FOREIGN KEY(tenant_id,effect_id) REFERENCES toolgate_target_claims(tenant_id,effect_id)
);
CREATE TABLE toolgate_target_schema (
 format varchar(64) PRIMARY KEY CHECK(format='toolgate-target-claims-v1')
);
INSERT INTO toolgate_target_schema(format) VALUES('toolgate-target-claims-v1');
DO $$ DECLARE t text; BEGIN
 FOREACH t IN ARRAY ARRAY['toolgate_target_claims','toolgate_target_outcomes'] LOOP
  EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY',t);
  EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY',t);
  IF NOT EXISTS(SELECT 1 FROM pg_policies WHERE schemaname=current_schema() AND tablename=t AND policyname='target_tenant_scope') THEN
   EXECUTE format('CREATE POLICY target_tenant_scope ON %I USING (tenant_id=current_setting(''olo.tenant'',true)) WITH CHECK (tenant_id=current_setting(''olo.tenant'',true))',t);
  END IF;
 END LOOP;
END $$;
CREATE FUNCTION toolgate_target_claim_transition() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.tenant_id<>OLD.tenant_id OR NEW.effect_id<>OLD.effect_id OR NEW.authorization_generation<>OLD.authorization_generation
    OR NEW.auth_nonce<>OLD.auth_nonce OR NEW.request_digest<>OLD.request_digest OR NEW.claim_token<>OLD.claim_token
    OR NEW.authorization_expires_at<>OLD.authorization_expires_at THEN
  RAISE EXCEPTION 'immutable target claim binding';
 END IF;
 IF NOT (
   (OLD.state='CLAIMED' AND NEW.state='APPLYING' AND NEW.ownership_generation=OLD.ownership_generation)
   OR (OLD.state='CLAIMED' AND NEW.state='ABANDONED' AND NEW.ownership_generation=OLD.ownership_generation+1)
   OR (OLD.state='APPLYING' AND NEW.state IN ('DONE','FAILED') AND NEW.ownership_generation=OLD.ownership_generation)
 ) THEN RAISE EXCEPTION 'invalid target claim transition'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER toolgate_target_claim_transition BEFORE UPDATE ON toolgate_target_claims FOR EACH ROW EXECUTE FUNCTION toolgate_target_claim_transition();
-- After the ordinary service grants, revoke table-wide UPDATE/DELETE on these two tables.
-- Grant UPDATE(state,result_digest,ownership_generation) on claims and UPDATE(delivered) on outcomes only.
