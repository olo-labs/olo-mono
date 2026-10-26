-- Fresh installation only. Run as schema owner, then grant DML to the separate service role.
CREATE TABLE knowledge_resource (
 tenant text NOT NULL, id uuid NOT NULL, kind text NOT NULL CHECK(kind IN ('conversation','kb')),
 owner_id text NOT NULL, acl_version bigint NOT NULL DEFAULT 1, deleted boolean NOT NULL DEFAULT false,
 next_sequence bigint NOT NULL DEFAULT 0, PRIMARY KEY(tenant,id)
);
CREATE TABLE knowledge_member (
 tenant text NOT NULL, resource_id uuid NOT NULL, subject text NOT NULL, can_write boolean NOT NULL,
 PRIMARY KEY(tenant,resource_id,subject), FOREIGN KEY(tenant,resource_id) REFERENCES knowledge_resource(tenant,id)
);
CREATE TABLE knowledge_receipt (
 tenant text NOT NULL, resource_id uuid NOT NULL, subject text NOT NULL, invocation_id text NOT NULL,
 request_digest text NOT NULL, result jsonb NOT NULL, created_at timestamptz NOT NULL DEFAULT now(),
 PRIMARY KEY(tenant,invocation_id), FOREIGN KEY(tenant,resource_id) REFERENCES knowledge_resource(tenant,id)
);
DO $$ DECLARE t text; BEGIN
 FOREACH t IN ARRAY ARRAY['knowledge_resource','knowledge_member','knowledge_receipt'] LOOP
  EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY',t);
  EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY',t);
  EXECUTE format('CREATE POLICY tenant_scope ON %I USING (tenant=current_setting(''olo.tenant'',true)) WITH CHECK (tenant=current_setting(''olo.tenant'',true))',t);
 END LOOP;
END $$;
