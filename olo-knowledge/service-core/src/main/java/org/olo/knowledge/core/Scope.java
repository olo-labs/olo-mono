// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.knowledge.core;

import static org.olo.knowledge.core.Database.*;

import java.sql.*;
import java.util.*;

/**
 * ToolGate grants and service ownership are independent requirements. Caller identities only come
 * from a permit.
 */
public record Scope(String tenant, String subject, String operation) {
    public Map<String, Object> require(Connection c, String id, boolean write, boolean ownerOnly)
            throws SQLException {
        lock(c, tenant, id);
        var roots =
                rows(
                        c,
                        "SELECT * FROM knowledge_resource WHERE tenant=? AND id=?::uuid FOR UPDATE",
                        tenant,
                        id);
        if (roots.isEmpty() || Boolean.TRUE.equals(roots.getFirst().get("deleted"))) throw denied();
        var root = roots.getFirst();
        if (subject.equals(root.get("owner_id"))) return root;
        if (ownerOnly) throw denied();
        var grants =
                rows(
                        c,
                        "SELECT can_write FROM knowledge_member WHERE tenant=? AND"
                                + " resource_id=?::uuid AND subject=?",
                        tenant,
                        id,
                        subject);
        if (grants.isEmpty() || (write && !Boolean.TRUE.equals(grants.getFirst().get("can_write"))))
            throw denied();
        return root;
    }

    public void create(Connection c, String id, String kind) throws SQLException {
        lock(c, tenant, id);
        update(
                c,
                "INSERT INTO knowledge_resource(tenant,id,kind,owner_id) VALUES (?,?::uuid,?,?) ON"
                        + " CONFLICT DO NOTHING",
                tenant,
                id,
                kind,
                subject);
        var root = require(c, id, true, false);
        if (!kind.equals(root.get("kind"))) throw denied();
    }

    public static void lock(Connection c, String tenant, String id) throws SQLException {
        rows(c, "SELECT pg_advisory_xact_lock(hashtextextended(?,0))", tenant + "\n" + id);
    }

    public void share(Connection c, String id, String member, boolean write, boolean remove)
            throws SQLException {
        require(c, id, true, true);
        if (remove)
            update(
                    c,
                    "DELETE FROM knowledge_member WHERE tenant=? AND resource_id=?::uuid AND"
                            + " subject=?",
                    tenant,
                    id,
                    member);
        else
            update(
                    c,
                    "INSERT INTO knowledge_member(tenant,resource_id,subject,can_write) VALUES"
                        + " (?,?::uuid,?,?) ON CONFLICT(tenant,resource_id,subject) DO UPDATE SET"
                        + " can_write=excluded.can_write",
                    tenant,
                    id,
                    member,
                    write);
        update(
                c,
                "UPDATE knowledge_resource SET acl_version=acl_version+1 WHERE tenant=? AND"
                        + " id=?::uuid",
                tenant,
                id);
    }

    public static SecurityException denied() {
        return new SecurityException("Resource unavailable");
    }
}
