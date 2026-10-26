// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.knowledge.rag;

import static org.olo.knowledge.core.Database.*;

import com.fasterxml.jackson.databind.JsonNode;

import org.olo.knowledge.core.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;

public final class RagService {
    private final Database db;
    private final EmbeddingModel embeddings;
    private final VectorIndex index;
    private String sweepCursor;

    public RagService(Database db, EmbeddingModel embeddings, VectorIndex index) {
        this.db = db;
        this.embeddings = embeddings;
        this.index = index;
    }

    public Object handle(ServiceRequest request, JsonNode body) throws Exception {
        if (body.has("request")) {
            var flat = Json.MAPPER.readTree(Json.text(body, "request", 20_971_520));
            if (!flat.isObject() || flat.has("kbId"))
                throw new IllegalArgumentException("Invalid request envelope");
            ((com.fasterxml.jackson.databind.node.ObjectNode) flat).set("kbId", body.get("kbId"));
            body = flat;
        }
        try (var c = db.open(request.evidence().tenant())) {
            var auth = request.authorize();
            var scope = request.scope(auth);
            Object result = execute(c, scope, body);
            if (!request.read())
                Receipts.record(c, scope, Json.id(body, "kbId"), auth.claims(), result);
            if (request.read()) {
                request.authorize().checkCommitDeadline();
                var released = Json.MAPPER.valueToTree(result);
                ((com.fasterxml.jackson.databind.node.ObjectNode) released)
                        .put("authorizedAt", System.currentTimeMillis());
                result = released;
            }
            Database.commit(c, auth);
            return result;
        }
    }

    Object execute(Connection c, Scope s, JsonNode b) throws Exception {
        String kb = Json.id(b, "kbId"), op = s.operation();
        if (op.equals("rag.query") && b.path("type").asText().equals("receipt"))
            return Receipts.lookup(c, s, kb, Json.text(b, "invocationId", 128));
        if (op.equals("rag.ingest") && b.path("create").asBoolean(false)) s.create(c, kb, "kb");
        if (op.equals("rag.query")
                || (b.has("docId") && Set.of("rag.update", "rag.delete", "rag.share").contains(op)))
            queryRoot(c, s, kb);
        else s.require(c, kb, true, op.equals("rag.share") && !b.has("docId"));
        return switch (op) {
            case "rag.ingest", "rag.update" -> ingest(c, s, kb, b);
            case "rag.query" -> query(c, s, kb, b);
            case "rag.delete" -> delete(c, s, kb, Json.id(b, "docId"));
            case "rag.share" -> {
                if (b.has("docId")) {
                    String doc = Json.id(b, "docId");
                    var document =
                            rows(
                                    c,
                                    "SELECT owner_id FROM rag_document WHERE tenant=? AND"
                                            + " kb_id=?::uuid AND doc_id=?::uuid AND NOT deleted",
                                    s.tenant(),
                                    kb,
                                    doc);
                    if (document.isEmpty()
                            || (!s.subject().equals(document.getFirst().get("owner_id"))
                                    && !s.subject().equals(queryRoot(c, s, kb).get("owner_id"))))
                        throw Scope.denied();
                    String member = Json.text(b, "member", 256);
                    if (b.path("remove").asBoolean(false))
                        update(
                                c,
                                "DELETE FROM rag_document_member WHERE tenant=? AND kb_id=?::uuid"
                                        + " AND doc_id=?::uuid AND subject=?",
                                s.tenant(),
                                kb,
                                doc,
                                member);
                    else
                        update(
                                c,
                                "INSERT INTO rag_document_member VALUES (?,?::uuid,?::uuid,?,?) ON"
                                        + " CONFLICT(tenant,kb_id,doc_id,subject) DO UPDATE SET"
                                        + " can_write=excluded.can_write",
                                s.tenant(),
                                kb,
                                doc,
                                member,
                                b.path("write").asBoolean(false));
                    update(
                            c,
                            "UPDATE rag_document SET acl_restricted=?,acl_version=acl_version+1"
                                    + " WHERE tenant=? AND kb_id=?::uuid AND doc_id=?::uuid",
                            b.path("restricted").asBoolean(true),
                            s.tenant(),
                            kb,
                            doc);
                    update(
                            c,
                            "UPDATE knowledge_resource SET acl_version=acl_version+1 WHERE tenant=?"
                                    + " AND id=?::uuid",
                            s.tenant(),
                            kb);
                } else
                    s.share(
                            c,
                            kb,
                            Json.text(b, "member", 256),
                            b.path("write").asBoolean(false),
                            b.path("remove").asBoolean(false));
                yield Map.of("shared", true);
            }
            default -> throw new IllegalArgumentException("Unsupported operation");
        };
    }

    private Object ingest(Connection c, Scope s, String kb, JsonNode b) throws Exception {
        if (b.path("type").asText().equals("reindex")) {
            if (!s.operation().equals("rag.update"))
                throw new IllegalArgumentException("Reindex requires rag.update");
            String doc = Json.id(b, "docId");
            requireDocument(c, s, kb, doc, true);
            var source =
                    rows(
                            c,
                            "SELECT content,version,model FROM rag_document WHERE tenant=? AND"
                                + " kb_id=?::uuid AND doc_id=?::uuid AND NOT deleted FOR UPDATE",
                            s.tenant(),
                            kb,
                            doc);
            if (source.isEmpty()) throw Scope.denied();
            var current = source.getFirst();
            if (embeddings.version().equals(current.get("model"))
                    && !b.path("force").asBoolean(false))
                return Map.of(
                        "docId",
                        doc,
                        "version",
                        current.get("version"),
                        "model",
                        embeddings.version());
            b =
                    Json.MAPPER.valueToTree(
                            Map.of(
                                    "docId",
                                    doc,
                                    "content",
                                    current.get("content"),
                                    "mediaType",
                                    "text/plain",
                                    "expectedVersion",
                                    current.get("version")));
        }
        String doc = Json.id(b, "docId"),
                content = Json.text(b, "content", 5_242_880),
                type = Json.text(b, "mediaType", 64);
        if (!Set.of("text/plain", "text/markdown").contains(type)
                || content.getBytes(StandardCharsets.UTF_8).length > 5_242_880
                || content.indexOf('\0') >= 0)
            throw new IllegalArgumentException(
                    "Only bounded UTF-8 plain text and Markdown are accepted");
        long expected = b.path("expectedVersion").asLong(-1);
        var old =
                rows(
                        c,
                        "SELECT * FROM rag_document WHERE tenant=? AND kb_id=?::uuid AND"
                                + " doc_id=?::uuid FOR UPDATE",
                        s.tenant(),
                        kb,
                        doc);
        if (!old.isEmpty() && Boolean.TRUE.equals(old.getFirst().get("deleted")))
            throw Scope.denied();
        if (!old.isEmpty()) requireDocument(c, s, kb, doc, true);
        long current = old.isEmpty() ? 0 : ((Number) old.getFirst().get("version")).longValue();
        String hash =
                HexFormat.of()
                        .formatHex(
                                MessageDigest.getInstance("SHA-256")
                                        .digest(content.getBytes(StandardCharsets.UTF_8)));
        if (current == expected + 1
                && !old.isEmpty()
                && hash.equals(old.getFirst().get("content_sha256"))
                && embeddings.version().equals(old.getFirst().get("model")))
            return Map.of("docId", doc, "version", current, "model", embeddings.version());
        if (current != expected) throw new IllegalArgumentException("Document version conflict");
        long version = current + 1;
        var chunks = chunk(content);
        var vectors = embeddings.embed(chunks);
        var points = new ArrayList<VectorIndex.Point>();
        for (int i = 0; i < chunks.size(); i++) {
            String cid =
                    UUID.nameUUIDFromBytes(
                                    (s.tenant()
                                                    + ":"
                                                    + kb
                                                    + ":"
                                                    + doc
                                                    + ":"
                                                    + version
                                                    + ":"
                                                    + embeddings.version()
                                                    + ":"
                                                    + i)
                                            .getBytes(StandardCharsets.UTF_8))
                            .toString();
            points.add(
                    new VectorIndex.Point(
                            cid,
                            s.tenant(),
                            kb,
                            doc,
                            version,
                            embeddings.version(),
                            vectors.get(i)));
        }
        // Unpublished vectors never pass the SQL version join. Register cleanup in the SAME
        // transaction as publication.
        // Failed transactions may leave opaque orphan vectors; the periodic SQL-authoritative sweep
        // removes them.
        index.put(points);
        if (current > 0)
            update(
                    c,
                    "INSERT INTO rag_vector_cleanup VALUES (?,?::uuid,?::uuid,?) ON CONFLICT DO"
                            + " NOTHING",
                    s.tenant(),
                    kb,
                    doc,
                    current);
        update(
                c,
                "INSERT INTO"
                    + " rag_document(tenant,kb_id,doc_id,owner_id,version,model,content,content_sha256)"
                    + " VALUES (?,?::uuid,?::uuid,?,?,?,?,?) ON CONFLICT(tenant,kb_id,doc_id) DO"
                    + " UPDATE SET"
                    + " version=excluded.version,model=excluded.model,content=excluded.content,content_sha256=excluded.content_sha256",
                s.tenant(),
                kb,
                doc,
                s.subject(),
                version,
                embeddings.version(),
                content,
                hash);
        update(
                c,
                "DELETE FROM rag_chunk WHERE tenant=? AND kb_id=?::uuid AND doc_id=?::uuid",
                s.tenant(),
                kb,
                doc);
        for (int i = 0; i < points.size(); i++)
            update(
                    c,
                    "INSERT INTO rag_chunk VALUES (?,?::uuid,?::uuid,?,?::uuid,?,?,?)",
                    s.tenant(),
                    kb,
                    doc,
                    version,
                    points.get(i).id(),
                    i,
                    chunks.get(i),
                    embeddings.version());
        return Map.of("docId", doc, "version", version, "model", embeddings.version());
    }

    private Object query(Connection c, Scope s, String kb, JsonNode b) throws Exception {
        var resource = queryRoot(c, s, kb);
        if (b.path("type").asText().equals("check")) {
            var sources = b.path("sources");
            if (!sources.isArray() || sources.size() > 256)
                throw new IllegalArgumentException("Sources required");
            var valid = new ArrayList<String>();
            for (var source : sources) {
                var docs =
                        rows(
                                c,
                                "SELECT version FROM rag_document WHERE tenant=? AND kb_id=?::uuid"
                                        + " AND doc_id=?::uuid AND NOT deleted",
                                s.tenant(),
                                kb,
                                Json.id(source, "docId"));
                if (!docs.isEmpty()
                        && ((Number) docs.getFirst().get("version")).longValue()
                                == source.path("version").asLong()
                        && documentAllowed(c, s, kb, Json.id(source, "docId"), false))
                    valid.add(Json.id(source, "docId"));
            }
            return Map.of("valid", valid, "aclVersion", resource.get("acl_version"));
        }
        String question = Json.text(b, "query", 4096);
        // Select visible versions in SQL; do not issue one ACL round-trip per document.
        var docs =
                rows(
                        c,
                        "SELECT d.doc_id,d.version FROM rag_document d JOIN knowledge_resource r ON"
                            + " r.tenant=d.tenant AND r.id=d.kb_id WHERE d.tenant=? AND"
                            + " d.kb_id=?::uuid AND NOT d.deleted AND NOT r.deleted AND d.model=?"
                            + " AND (d.owner_id=? OR r.owner_id=? OR EXISTS (SELECT 1 FROM"
                            + " rag_document_member m WHERE m.tenant=d.tenant AND m.kb_id=d.kb_id"
                            + " AND m.doc_id=d.doc_id AND m.subject=?) OR (NOT d.acl_restricted AND"
                            + " EXISTS (SELECT 1 FROM knowledge_member m WHERE m.tenant=d.tenant"
                            + " AND m.resource_id=d.kb_id AND m.subject=?)))",
                        s.tenant(),
                        kb,
                        embeddings.version(),
                        s.subject(),
                        s.subject(),
                        s.subject(),
                        s.subject());
        var versions = new ArrayList<String>();
        for (var d : docs) versions.add(d.get("doc_id") + ":" + d.get("version"));
        var found =
                index.query(
                        s.tenant(),
                        kb,
                        embeddings.version(),
                        versions,
                        embeddings.embed(List.of(question)).getFirst(),
                        5);
        var results = new ArrayList<Map<String, Object>>();
        // Join all candidates to current SQL publication and ACL state in one
        // round trip. Keep vector rank when assembling the authorized result.
        var current = new HashMap<String, Map<String, Object>>();
        if (!found.isEmpty()) {
            var arguments =
                    new ArrayList<Object>(
                            List.of(
                                    s.tenant(),
                                    kb,
                                    embeddings.version(),
                                    s.subject(),
                                    s.subject(),
                                    s.subject(),
                                    s.subject()));
            for (String cid : found) arguments.add(Json.uuid(cid));
            var placeholders = String.join(",", Collections.nCopies(found.size(), "?::uuid"));
            for (var hit :
                    rows(
                            c,
                            "SELECT c.doc_id,c.version,c.chunk_id,c.content,c.ordinal FROM"
                                + " rag_chunk c JOIN rag_document d ON d.tenant=c.tenant AND"
                                + " d.kb_id=c.kb_id AND d.doc_id=c.doc_id AND d.version=c.version"
                                + " JOIN knowledge_resource r ON r.tenant=d.tenant AND r.id=d.kb_id"
                                + " WHERE c.tenant=? AND c.kb_id=?::uuid AND d.model=? AND NOT"
                                + " d.deleted AND NOT r.deleted AND (d.owner_id=? OR r.owner_id=?"
                                + " OR EXISTS (SELECT 1 FROM rag_document_member m WHERE"
                                + " m.tenant=d.tenant AND m.kb_id=d.kb_id AND m.doc_id=d.doc_id AND"
                                + " m.subject=?) OR (NOT d.acl_restricted AND EXISTS (SELECT 1 FROM"
                                + " knowledge_member m WHERE m.tenant=d.tenant AND"
                                + " m.resource_id=d.kb_id AND m.subject=?))) AND c.chunk_id IN ("
                                    + placeholders
                                    + ")",
                            arguments.toArray())) {
                current.put(hit.get("chunk_id").toString(), hit);
            }
        }
        int budget = 0;
        for (String cid : found) {
            var hit = current.remove(cid);
            if (hit == null) continue;
            int size = hit.get("content").toString().getBytes(StandardCharsets.UTF_8).length;
            if (budget + size > 4000) continue;
            budget += size;
            hit.put("resource", "olo://" + s.tenant() + "/kb/" + kb + "/doc/" + hit.get("doc_id"));
            hit.put("aclVersion", resource.get("acl_version"));
            hit.put("untrusted", true);
            results.add(hit);
        }
        // Root remains locked until permit refresh and commit; membership cannot change between
        // query and release.
        return Map.of("chunks", results, "budgetUsed", budget, "untrusted", true);
    }

    private Object delete(Connection c, Scope s, String kb, String doc) throws Exception {
        var docs =
                rows(
                        c,
                        "SELECT version FROM rag_document WHERE tenant=? AND kb_id=?::uuid AND"
                                + " doc_id=?::uuid FOR UPDATE",
                        s.tenant(),
                        kb,
                        doc);
        if (docs.isEmpty()) throw Scope.denied();
        requireDocument(c, s, kb, doc, true);
        update(
                c,
                "INSERT INTO rag_vector_cleanup SELECT tenant,kb_id,doc_id,version FROM"
                        + " rag_document WHERE tenant=? AND kb_id=?::uuid AND doc_id=?::uuid ON"
                        + " CONFLICT DO NOTHING",
                s.tenant(),
                kb,
                doc);
        update(
                c,
                "UPDATE rag_document SET"
                    + " deleted=true,content='',content_sha256='',acl_version=acl_version+1 WHERE"
                    + " tenant=? AND kb_id=?::uuid AND doc_id=?::uuid",
                s.tenant(),
                kb,
                doc);
        update(
                c,
                "DELETE FROM rag_chunk WHERE tenant=? AND kb_id=?::uuid AND doc_id=?::uuid",
                s.tenant(),
                kb,
                doc);
        update(
                c,
                "DELETE FROM rag_document_member WHERE tenant=? AND kb_id=?::uuid AND"
                        + " doc_id=?::uuid",
                s.tenant(),
                kb,
                doc);
        return Map.of("deleted", true);
    }

    public synchronized void cleanup(String tenant) throws Exception {
        try (var c = db.open(tenant)) {
            for (var item :
                    rows(
                            c,
                            "SELECT * FROM rag_vector_cleanup WHERE tenant=? LIMIT 100 FOR UPDATE"
                                    + " SKIP LOCKED",
                            tenant)) {
                String kb = item.get("kb_id").toString(), doc = item.get("doc_id").toString();
                long version = ((Number) item.get("version")).longValue();
                index.delete(tenant, kb, doc, version);
                update(
                        c,
                        "DELETE FROM rag_vector_cleanup WHERE tenant=? AND kb_id=?::uuid AND"
                                + " doc_id=?::uuid AND version=?",
                        tenant,
                        kb,
                        doc,
                        version);
            }
            var page = index.scan(tenant, sweepCursor);
            for (var point : page.points()) {
                String kb = Json.uuid(point.kb()), doc = Json.uuid(point.doc());
                // Includes uncommitted root creation: do not sweep a vector while its publication
                // is in progress.
                Scope.lock(c, tenant, kb);
                rows(
                        c,
                        "SELECT id FROM knowledge_resource WHERE tenant=? AND id=?::uuid FOR"
                                + " UPDATE",
                        tenant,
                        kb);
                var active =
                        rows(
                                c,
                                "SELECT 1 FROM rag_document d JOIN knowledge_resource r ON"
                                    + " r.tenant=d.tenant AND r.id=d.kb_id WHERE d.tenant=? AND"
                                    + " d.kb_id=?::uuid AND d.doc_id=?::uuid AND d.version=? AND"
                                    + " NOT d.deleted AND NOT r.deleted",
                                tenant,
                                kb,
                                doc,
                                point.version());
                if (active.isEmpty()) index.delete(tenant, kb, doc, point.version());
            }
            c.commit();
            sweepCursor = page.next();
        }
    }

    private Map<String, Object> queryRoot(Connection c, Scope s, String kb) throws Exception {
        var root =
                rows(
                        c,
                        "SELECT * FROM knowledge_resource WHERE tenant=? AND id=?::uuid AND NOT"
                                + " deleted "
                                + (s.operation().equals("rag.query") ? "FOR SHARE" : "FOR UPDATE"),
                        s.tenant(),
                        kb);
        if (root.isEmpty()) throw Scope.denied();
        if (s.subject().equals(root.getFirst().get("owner_id"))) return root.getFirst();
        boolean member =
                !rows(
                                c,
                                "SELECT 1 FROM knowledge_member WHERE tenant=? AND"
                                        + " resource_id=?::uuid AND subject=?",
                                s.tenant(),
                                kb,
                                s.subject())
                        .isEmpty();
        boolean doc =
                !rows(
                                c,
                                "SELECT 1 FROM rag_document_member m JOIN rag_document d"
                                    + " USING(tenant,kb_id,doc_id) WHERE m.tenant=? AND"
                                    + " m.kb_id=?::uuid AND m.subject=? AND NOT d.deleted LIMIT 1",
                                s.tenant(),
                                kb,
                                s.subject())
                        .isEmpty();
        if (!member && !doc) throw Scope.denied();
        return root.getFirst();
    }

    private boolean documentAllowed(Connection c, Scope s, String kb, String doc, boolean write)
            throws Exception {
        var docs =
                rows(
                        c,
                        "SELECT d.owner_id,d.acl_restricted,r.owner_id AS kb_owner FROM"
                            + " rag_document d JOIN knowledge_resource r ON r.tenant=d.tenant AND"
                            + " r.id=d.kb_id WHERE d.tenant=? AND d.kb_id=?::uuid AND"
                            + " d.doc_id=?::uuid AND NOT d.deleted AND NOT r.deleted",
                        s.tenant(),
                        kb,
                        doc);
        if (docs.isEmpty()) return false;
        var d = docs.getFirst();
        if (s.subject().equals(d.get("owner_id")) || s.subject().equals(d.get("kb_owner")))
            return true;
        var explicit =
                rows(
                        c,
                        "SELECT can_write FROM rag_document_member WHERE tenant=? AND kb_id=?::uuid"
                                + " AND doc_id=?::uuid AND subject=?",
                        s.tenant(),
                        kb,
                        doc,
                        s.subject());
        if (!explicit.isEmpty()
                && (!write || Boolean.TRUE.equals(explicit.getFirst().get("can_write"))))
            return true;
        if (Boolean.TRUE.equals(d.get("acl_restricted"))) return false;
        var inherited =
                rows(
                        c,
                        "SELECT can_write FROM knowledge_member WHERE tenant=? AND"
                                + " resource_id=?::uuid AND subject=?",
                        s.tenant(),
                        kb,
                        s.subject());
        return !inherited.isEmpty()
                && (!write || Boolean.TRUE.equals(inherited.getFirst().get("can_write")));
    }

    private void requireDocument(Connection c, Scope s, String kb, String doc, boolean write)
            throws Exception {
        if (!documentAllowed(c, s, kb, doc, write)) throw Scope.denied();
    }

    static List<String> chunk(String content) {
        var chunks = new ArrayList<String>();
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(content.length(), start + 1000);
            if (end < content.length() && Character.isHighSurrogate(content.charAt(end - 1))) end--;
            chunks.add(content.substring(start, end));
            start = end;
        }
        return chunks;
    }
}
