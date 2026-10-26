// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.knowledge.core;

import java.sql.*;
import java.util.*;

/**
 * Separate service database credentials. Every transaction has a bounded statement/lock timeout.
 */
public final class Database {
    private final String url, user, password;

    public Database(String url, String user, String password) {
        this.url = Objects.requireNonNull(url);
        this.user = Objects.requireNonNull(user);
        this.password = Objects.requireNonNull(password);
    }

    public Connection open(String tenant) throws SQLException {
        var c = DriverManager.getConnection(url, user, password);
        c.setAutoCommit(false);
        c.setNetworkTimeout(Runnable::run, 8000);
        try (var s = c.createStatement()) {
            s.execute("SET LOCAL statement_timeout='8s'");
            s.execute("SET LOCAL lock_timeout='3s'");
        }
        try (var s = c.prepareStatement("SELECT set_config('olo.tenant',?,true)")) {
            s.setString(1, tenant);
            s.execute();
        }
        return c;
    }

    public static void commit(
            Connection c, io.ololabs.toolgate.service.ServicePermitVerifier.Authorization auth)
            throws SQLException {
        long remaining = auth.claims().exp() - System.currentTimeMillis();
        if (remaining <= 0) throw new SecurityException("Commit authorization expired");
        c.setNetworkTimeout(Runnable::run, (int) Math.min(remaining, 8000));
        try (var s = c.createStatement()) {
            s.execute("SET LOCAL statement_timeout='" + Math.min(remaining, 8000) + "ms'");
        }
        auth.checkCommitDeadline();
        try {
            c.commit();
        } catch (SQLException uncertain) {
            throw new CommitUncertain(uncertain);
        }
    }

    public static final class CommitUncertain extends SQLException {
        public CommitUncertain(SQLException cause) {
            super("Storage commit acknowledgement lost; reconcile", cause);
        }
    }

    public static int update(Connection c, String sql, Object... args) throws SQLException {
        try (var s = c.prepareStatement(sql)) {
            bind(s, args);
            return s.executeUpdate();
        }
    }

    public static List<Map<String, Object>> rows(Connection c, String sql, Object... args)
            throws SQLException {
        try (var s = c.prepareStatement(sql)) {
            bind(s, args);
            try (var r = s.executeQuery()) {
                var result = new ArrayList<Map<String, Object>>();
                var md = r.getMetaData();
                while (r.next()) {
                    var row = new LinkedHashMap<String, Object>();
                    for (int i = 1; i <= md.getColumnCount(); i++)
                        row.put(md.getColumnLabel(i), r.getObject(i));
                    result.add(row);
                }
                return result;
            }
        }
    }

    private static void bind(PreparedStatement s, Object[] args) throws SQLException {
        for (int i = 0; i < args.length; i++) s.setObject(i + 1, args[i]);
    }
}
