// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.knowledge.testing;

import org.olo.knowledge.core.*;

import java.nio.file.*;
import java.sql.*;
import java.util.*;

/**
 * Real PostgreSQL with forced RLS under a non-owner, non-superuser role. Launched by
 * tools/test_services.py.
 */
public final class PostgresFixture {
    private static final Map<Database, String> URLS = new java.util.IdentityHashMap<>();

    public static String url(Database db) {
        return URLS.get(db);
    }

    public static final String TENANT = UUID.randomUUID().toString();
    public static final String OTHER_TENANT = UUID.randomUUID().toString();

    public static Database create(String name, boolean rag) throws Exception {
        String base =
                Objects.requireNonNull(
                        System.getenv("KNOWLEDGE_TEST_URL"),
                        "Run tools/test_services.py with PostgreSQL");
        String password = System.getenv("KNOWLEDGE_TEST_PASSWORD");
        String schema = name + "_" + UUID.randomUUID().toString().replace("-", "");
        try (var admin = DriverManager.getConnection(base, "postgres", password);
                var s = admin.createStatement()) {
            s.execute("CREATE SCHEMA " + schema);
            s.execute("SET search_path=" + schema);
            s.execute(Files.readString(Path.of("../deploy/common.sql")));
            s.execute(
                    Files.readString(Path.of(rag ? "../deploy/rag.sql" : "../deploy/memory.sql")));
            s.execute("GRANT USAGE ON SCHEMA " + schema + " TO knowledge_test");
            s.execute(
                    "GRANT SELECT,INSERT,UPDATE,DELETE ON ALL TABLES IN SCHEMA "
                            + schema
                            + " TO knowledge_test");
        }
        var db = new Database(base + "&currentSchema=" + schema, "knowledge_test", password);
        URLS.put(db, base + "&currentSchema=" + schema);
        return db;
    }

    public static com.fasterxml.jackson.databind.JsonNode json(Object value) {
        return Json.MAPPER.valueToTree(value);
    }
}
