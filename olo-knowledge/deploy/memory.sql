CREATE TABLE memory_message (
 tenant text NOT NULL, conversation_id uuid NOT NULL, message_id uuid NOT NULL, turn_id uuid NOT NULL,
 sequence bigint NOT NULL, role text NOT NULL CHECK(role IN ('user','assistant','failure','cancellation','pending')),
 content text NOT NULL, dependencies jsonb NOT NULL DEFAULT '[]',
 PRIMARY KEY(tenant,conversation_id,message_id), UNIQUE(tenant,conversation_id,sequence),
 FOREIGN KEY(tenant,conversation_id) REFERENCES knowledge_resource(tenant,id)
);
CREATE UNIQUE INDEX memory_one_final ON memory_message(tenant,conversation_id,turn_id) WHERE role IN ('assistant','failure','cancellation');
CREATE TABLE memory_summary (
 tenant text NOT NULL, conversation_id uuid NOT NULL, version bigint NOT NULL,
 content text NOT NULL, dependencies jsonb NOT NULL, PRIMARY KEY(tenant,conversation_id,version),
 FOREIGN KEY(tenant,conversation_id) REFERENCES knowledge_resource(tenant,id)
);
CREATE TABLE memory_working_state (
 tenant text NOT NULL, conversation_id uuid NOT NULL, state_key uuid NOT NULL, version bigint NOT NULL,
 value jsonb NOT NULL, expires_at timestamptz NOT NULL,
 PRIMARY KEY(tenant,conversation_id,state_key), FOREIGN KEY(tenant,conversation_id) REFERENCES knowledge_resource(tenant,id)
);
DO $$ DECLARE t text; BEGIN
 FOREACH t IN ARRAY ARRAY['memory_message','memory_summary','memory_working_state'] LOOP
  EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY',t);
  EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY',t);
  EXECUTE format('CREATE POLICY tenant_scope ON %I USING (tenant=current_setting(''olo.tenant'',true)) WITH CHECK (tenant=current_setting(''olo.tenant'',true))',t);
 END LOOP;
END $$;
