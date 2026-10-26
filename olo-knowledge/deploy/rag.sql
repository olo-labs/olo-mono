CREATE TABLE rag_document (
 tenant text NOT NULL, kb_id uuid NOT NULL, doc_id uuid NOT NULL, owner_id text NOT NULL,
 version bigint NOT NULL, acl_version bigint NOT NULL DEFAULT 1, acl_restricted boolean NOT NULL DEFAULT false, deleted boolean NOT NULL DEFAULT false,
 model text NOT NULL, content text NOT NULL, content_sha256 text NOT NULL,
 PRIMARY KEY(tenant,kb_id,doc_id), FOREIGN KEY(tenant,kb_id) REFERENCES knowledge_resource(tenant,id)
);
CREATE TABLE rag_chunk (
 tenant text NOT NULL, kb_id uuid NOT NULL, doc_id uuid NOT NULL, version bigint NOT NULL,
 chunk_id uuid NOT NULL, ordinal integer NOT NULL, content text NOT NULL, model text NOT NULL,
 PRIMARY KEY(tenant,kb_id,chunk_id), FOREIGN KEY(tenant,kb_id,doc_id) REFERENCES rag_document(tenant,kb_id,doc_id)
);
CREATE TABLE rag_document_member (
 tenant text NOT NULL, kb_id uuid NOT NULL, doc_id uuid NOT NULL, subject text NOT NULL, can_write boolean NOT NULL,
 PRIMARY KEY(tenant,kb_id,doc_id,subject), FOREIGN KEY(tenant,kb_id,doc_id) REFERENCES rag_document(tenant,kb_id,doc_id)
);
-- Durable cleanup outbox: tombstones are authoritative even while Qdrant is unavailable.
CREATE TABLE rag_vector_cleanup (
 tenant text NOT NULL, kb_id uuid NOT NULL, doc_id uuid NOT NULL, version bigint NOT NULL,
 PRIMARY KEY(tenant,kb_id,doc_id,version)
);
DO $$ DECLARE t text; BEGIN
 FOREACH t IN ARRAY ARRAY['rag_document','rag_chunk','rag_document_member','rag_vector_cleanup'] LOOP
  EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY',t);
  EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY',t);
  EXECUTE format('CREATE POLICY tenant_scope ON %I USING (tenant=current_setting(''olo.tenant'',true)) WITH CHECK (tenant=current_setting(''olo.tenant'',true))',t);
 END LOOP;
END $$;
