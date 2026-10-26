// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.knowledge.core;

import static org.olo.knowledge.core.Database.*;

import java.sql.*;
import java.util.*;

public final class Receipts {
    private Receipts() {}

    public static void record(
            Connection c,
            Scope scope,
            String resource,
            io.ololabs.toolgate.contracts.ServicePermitClaims claims,
            Object result)
            throws SQLException {
        if (scope.operation().equals("memory.export")) result = Map.of("exported", true);
        update(
                c,
                "INSERT INTO"
                    + " knowledge_receipt(tenant,resource_id,subject,invocation_id,request_digest,result)"
                    + " VALUES (?,?::uuid,?,?,?,?::jsonb)",
                scope.tenant(),
                resource,
                scope.subject(),
                claims.invocationId(),
                claims.requestFingerprint(),
                Json.encode(result));
    }

    public static Object lookup(Connection c, Scope scope, String resource, String invocation)
            throws Exception {
        var matches =
                rows(
                        c,
                        "SELECT result FROM knowledge_receipt WHERE tenant=? AND"
                            + " resource_id=?::uuid AND subject=? AND invocation_id=?",
                        scope.tenant(),
                        resource,
                        scope.subject(),
                        invocation);
        return matches.isEmpty()
                ? Map.of(
                        "state",
                        "NOT_FOUND",
                        "meaning",
                        "Absence is not proof that no effect occurred")
                : Map.of(
                        "state",
                        "COMMITTED",
                        "result",
                        Json.MAPPER.readTree(matches.getFirst().get("result").toString()));
    }
}
