// Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
package org.olo.knowledge.memory;

import org.olo.knowledge.core.*;

public final class WorkingStateRetention {
    private final Database db;

    public WorkingStateRetention(Database db) {
        this.db = db;
    }

    public void purge(String tenant) throws Exception {
        try (var c = db.open(tenant)) {
            Database.update(
                    c,
                    "DELETE FROM memory_working_state WHERE tenant=? AND expires_at<=now()",
                    tenant);
            c.commit();
        }
    }
}
