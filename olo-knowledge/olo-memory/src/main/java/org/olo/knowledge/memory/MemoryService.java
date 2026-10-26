// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.knowledge.memory;

import static org.olo.knowledge.core.Database.*;

import com.fasterxml.jackson.databind.JsonNode;

import org.olo.knowledge.core.*;

import java.sql.*;
import java.util.*;

/** One conversation lock serializes sequence assignment, membership changes, and release checks. */
public final class MemoryService {
    private final Database db;

    public MemoryService(Database db) {
        this.db = db;
    }

    public Object handle(ServiceRequest request, JsonNode body) throws Exception {
        if (body.has("request")) {
            var flat = Json.MAPPER.readTree(Json.text(body, "request", 20_971_520));
            if (!flat.isObject() || flat.has("conversationId"))
                throw new IllegalArgumentException("Invalid request envelope");
            ((com.fasterxml.jackson.databind.node.ObjectNode) flat)
                    .set("conversationId", body.get("conversationId"));
            body = flat;
        }
        try (var c = db.open(request.evidence().tenant())) {
            var authorization = request.authorize();
            var scope = request.scope(authorization);
            Object result = execute(c, scope, body);
            if (!request.read())
                Receipts.record(
                        c, scope, Json.id(body, "conversationId"), authorization.claims(), result);
            if (request.read()) {
                request.authorize().checkCommitDeadline();
                var released = Json.MAPPER.valueToTree(result);
                ((com.fasterxml.jackson.databind.node.ObjectNode) released)
                        .put("authorizedAt", System.currentTimeMillis());
                result = released;
            }
            Database.commit(c, authorization);
            return result;
        }
    }

    // Exposed for storage qualification. Production enters via the permit + mTLS boundary above.
    Object execute(Connection c, Scope s, JsonNode b) throws Exception {
        String id = Json.id(b, "conversationId"), op = s.operation();
        if (op.equals("memory.read") && b.path("type").asText().equals("receipt"))
            return Receipts.lookup(c, s, id, Json.text(b, "invocationId", 128));
        if (op.equals("memory.append") && b.path("create").asBoolean(false))
            s.create(c, id, "conversation");
        s.require(
                c,
                id,
                !op.equals("memory.read"),
                op.equals("memory.share")
                        || op.equals("memory.delete")
                        || op.equals("memory.export"));
        return switch (op) {
            case "memory.append" -> append(c, s, id, b);
            case "memory.read" -> read(c, s, id, b);
            case "memory.export" -> {
                var messages =
                        rows(
                                c,
                                "SELECT message_id,turn_id,sequence,role,content,dependencies FROM"
                                    + " memory_message WHERE tenant=? AND conversation_id=?::uuid"
                                    + " ORDER BY sequence LIMIT 10001",
                                s.tenant(),
                                id);
                if (messages.size() > 10000)
                    throw new IllegalArgumentException("Export page limit exceeded");
                for (var message : messages)
                    message.put(
                            "dependencies",
                            Json.MAPPER.readTree(message.get("dependencies").toString()));
                yield Map.of("messages", messages);
            }
            case "memory.share" -> {
                s.share(
                        c,
                        id,
                        Json.text(b, "member", 256),
                        b.path("write").asBoolean(false),
                        b.path("remove").asBoolean(false));
                // No summaries survive a membership change without a rebuild from current sources.
                update(
                        c,
                        "DELETE FROM memory_summary WHERE tenant=? AND conversation_id=?::uuid",
                        s.tenant(),
                        id);
                yield Map.of("shared", true);
            }
            case "memory.delete" -> {
                update(
                        c,
                        "DELETE FROM knowledge_receipt WHERE tenant=? AND resource_id=?::uuid",
                        s.tenant(),
                        id);
                for (String table :
                        List.of("memory_message", "memory_summary", "memory_working_state"))
                    update(
                            c,
                            "DELETE FROM " + table + " WHERE tenant=? AND conversation_id=?::uuid",
                            s.tenant(),
                            id);
                update(
                        c,
                        "DELETE FROM knowledge_member WHERE tenant=? AND resource_id=?::uuid",
                        s.tenant(),
                        id);
                update(
                        c,
                        "UPDATE knowledge_resource SET deleted=true,acl_version=acl_version+1 WHERE"
                                + " tenant=? AND id=?::uuid",
                        s.tenant(),
                        id);
                yield Map.of("deleted", true);
            }
            default -> throw new IllegalArgumentException("Unsupported operation");
        };
    }

    private Object append(Connection c, Scope s, String id, JsonNode b) throws Exception {
        String type = b.path("type").asText("message");
        if (type.equals("create") || type.equals("check")) return Map.of("authorized", true);
        if (type.equals("working")) return working(c, s, id, b);
        if (!type.equals("message")) throw new IllegalArgumentException("Invalid append type");
        String message = Json.id(b, "messageId"),
                turn = Json.id(b, "turnId"),
                role = Json.text(b, "role", 16),
                content = Json.text(b, "content", 131072);
        if (!Set.of("user", "assistant", "pending", "failure", "cancellation").contains(role))
            throw new IllegalArgumentException("Invalid role");
        JsonNode deps = b.path("dependencies");
        if (!deps.isArray() || deps.size() > 256)
            throw new IllegalArgumentException("Source dependencies required");
        String dependencies = Json.encode(deps);
        if (Set.of("failure", "cancellation", "pending").contains(role)) {
            var finished =
                    rows(
                            c,
                            "SELECT message_id FROM memory_message WHERE tenant=? AND"
                                    + " conversation_id=?::uuid AND turn_id=?::uuid AND role IN"
                                    + " ('assistant','failure','cancellation')",
                            s.tenant(),
                            id,
                            turn);
            if (!finished.isEmpty()) return Map.of("alreadyFinal", true);
        }
        var existing =
                rows(
                        c,
                        "SELECT turn_id,role,content,dependencies,sequence FROM memory_message"
                            + " WHERE tenant=? AND conversation_id=?::uuid AND message_id=?::uuid",
                        s.tenant(),
                        id,
                        message);
        if (!existing.isEmpty()) {
            var old = existing.getFirst();
            if (!turn.equals(old.get("turn_id").toString())
                    || !role.equals(old.get("role"))
                    || !content.equals(old.get("content"))
                    || !deps.equals(Json.MAPPER.readTree(old.get("dependencies").toString())))
                throw new IllegalArgumentException("Idempotency conflict");
            return Map.of("messageId", message, "sequence", old.get("sequence"));
        }
        var seq =
                rows(
                                c,
                                "UPDATE knowledge_resource SET next_sequence=next_sequence+1 WHERE"
                                        + " tenant=? AND id=?::uuid RETURNING next_sequence",
                                s.tenant(),
                                id)
                        .getFirst()
                        .get("next_sequence");
        update(
                c,
                "INSERT INTO"
                    + " memory_message(tenant,conversation_id,message_id,turn_id,sequence,role,content,dependencies)"
                    + " VALUES (?,?::uuid,?::uuid,?::uuid,?,?,?,?::jsonb)",
                s.tenant(),
                id,
                message,
                turn,
                seq,
                role,
                content,
                dependencies);
        if (!role.equals("pending"))
            update(
                    c,
                    "DELETE FROM memory_message WHERE tenant=? AND conversation_id=?::uuid AND"
                            + " turn_id=?::uuid AND role='pending'",
                    s.tenant(),
                    id,
                    turn);
        update(
                c,
                "DELETE FROM memory_summary WHERE tenant=? AND conversation_id=?::uuid",
                s.tenant(),
                id);
        return Map.of("messageId", message, "sequence", seq);
    }

    private Object read(Connection c, Scope s, String id, JsonNode b) throws Exception {
        if (b.path("type").asText().equals("check"))
            return Map.of(
                    "source",
                    Map.of(
                            "resource",
                            "olo://" + s.tenant() + "/conversation/" + id,
                            "aclVersion",
                            s.require(c, id, false, false).get("acl_version")));
        if (b.path("type").asText().equals("turn")) {
            var messages =
                    rows(
                            c,
                            "SELECT message_id,turn_id,sequence,role,content,dependencies FROM"
                                + " memory_message WHERE tenant=? AND conversation_id=?::uuid AND"
                                + " turn_id=?::uuid ORDER BY sequence",
                            s.tenant(),
                            id,
                            Json.id(b, "turnId"));
            for (var message : messages)
                message.put(
                        "dependencies",
                        Json.MAPPER.readTree(message.get("dependencies").toString()));
            return Map.of("messages", messages);
        }
        if (b.path("type").asText().equals("working")) {
            update(
                    c,
                    "DELETE FROM memory_working_state WHERE tenant=? AND conversation_id=?::uuid"
                            + " AND expires_at<=now()",
                    s.tenant(),
                    id);
            return Map.of(
                    "states",
                    rows(
                            c,
                            "SELECT state_key,version,value,expires_at FROM memory_working_state"
                                    + " WHERE tenant=? AND conversation_id=?::uuid AND"
                                    + " expires_at>now()",
                            s.tenant(),
                            id));
        }
        long boundary = Long.MAX_VALUE;
        if (b.hasNonNull("beforeMessageId")) {
            var cutoff =
                    rows(
                            c,
                            "SELECT sequence FROM memory_message WHERE tenant=? AND"
                                    + " conversation_id=?::uuid AND message_id=?::uuid",
                            s.tenant(),
                            id,
                            Json.id(b, "beforeMessageId"));
            if (cutoff.isEmpty()) throw new IllegalArgumentException("Accepted message missing");
            boundary = ((Number) cutoff.getFirst().get("sequence")).longValue();
        }
        int budget = Math.min(4000, Math.max(0, b.path("budget").asInt(4000)));
        var candidates =
                rows(
                        c,
                        "SELECT message_id,turn_id,sequence,role,content,dependencies FROM"
                            + " memory_message WHERE tenant=? AND conversation_id=?::uuid AND"
                            + " sequence<? AND role IN ('user','assistant') ORDER BY sequence DESC"
                            + " LIMIT 256",
                        s.tenant(),
                        id,
                        boundary);
        var selected = new ArrayList<Map<String, Object>>();
        int used = 0;
        for (var item : candidates) {
            // UTF-8 bytes are a conservative tokenizer-independent bound (a token has at least one
            // byte).
            int tokens =
                    item.get("content")
                            .toString()
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8)
                            .length;
            if (used + tokens > budget) break;
            used += tokens;
            item.put("dependencies", Json.MAPPER.readTree(item.get("dependencies").toString()));
            selected.add(item);
        }
        Collections.reverse(selected);
        var root = s.require(c, id, false, false);
        var source =
                Map.of(
                        "resource",
                        "olo://" + s.tenant() + "/conversation/" + id,
                        "aclVersion",
                        root.get("acl_version"));
        // Deterministic extractive summary: version and exact source-message/dependency set are
        // persisted together.
        // A caller still reauthorizes external dependencies before using this derived content.
        var summaryDependencies = new LinkedHashMap<String, Object>();
        summaryDependencies.put("conversation", source);
        summaryDependencies.put(
                "messages", selected.stream().map(m -> m.get("message_id").toString()).toList());
        summaryDependencies.put(
                "sources", selected.stream().map(m -> m.get("dependencies")).toList());
        String summary =
                selected.stream()
                        .map(m -> m.get("role") + ": " + m.get("content"))
                        .collect(java.util.stream.Collectors.joining("\n"));
        // Different concurrent turn boundaries must not overwrite a summary for another source set.
        long summaryVersion =
                boundary == Long.MAX_VALUE
                        ? ((Number) root.get("next_sequence")).longValue()
                        : boundary - 1;
        update(
                c,
                "INSERT INTO memory_summary VALUES (?,?::uuid,?,?,?::jsonb) ON"
                        + " CONFLICT(tenant,conversation_id,version) DO UPDATE SET"
                        + " content=excluded.content,dependencies=excluded.dependencies",
                s.tenant(),
                id,
                summaryVersion,
                summary,
                Json.encode(summaryDependencies));
        return Map.of(
                "messages",
                selected,
                "source",
                source,
                "budgetUsed",
                used,
                "summary",
                Map.of(
                        "version",
                        summaryVersion,
                        "content",
                        summary,
                        "dependencies",
                        summaryDependencies));
    }

    private Object working(Connection c, Scope s, String id, JsonNode b) throws Exception {
        String key = Json.id(b, "stateKey");
        long expected = b.path("expectedVersion").asLong(-1);
        int ttl = b.path("ttlSeconds").asInt();
        if (ttl < 1
                || ttl > 86400
                || !b.has("value")
                || Json.encode(b.get("value")).length() > 16384)
            throw new IllegalArgumentException("Invalid working state");
        update(
                c,
                "DELETE FROM memory_working_state WHERE tenant=? AND conversation_id=?::uuid AND"
                        + " state_key=?::uuid AND expires_at<=now()",
                s.tenant(),
                id,
                key);
        var old =
                rows(
                        c,
                        "SELECT version FROM memory_working_state WHERE tenant=? AND"
                                + " conversation_id=?::uuid AND state_key=?::uuid",
                        s.tenant(),
                        id,
                        key);
        long current = old.isEmpty() ? 0 : ((Number) old.getFirst().get("version")).longValue();
        if (expected != current)
            throw new IllegalArgumentException("Working state version conflict");
        update(
                c,
                "INSERT INTO memory_working_state VALUES"
                    + " (?,?::uuid,?::uuid,?,?::jsonb,now()+?*interval '1 second') ON"
                    + " CONFLICT(tenant,conversation_id,state_key) DO UPDATE SET"
                    + " version=excluded.version,value=excluded.value,expires_at=excluded.expires_at",
                s.tenant(),
                id,
                key,
                current + 1,
                Json.encode(b.get("value")),
                ttl);
        return Map.of("version", current + 1);
    }
}
