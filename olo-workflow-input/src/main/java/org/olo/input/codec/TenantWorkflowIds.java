/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.input.codec;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Workflow ids that carry their tenant (M5.2): {@code run-<tenant>~<runId>} for runs and {@code <tenant>~<name>} for
 * child workflows. The payload codec reads the tenant from the workflow id of the payload it encodes, so every payload
 * of a run is sealed with that tenant's key.
 */
public final class TenantWorkflowIds {

    /** Prefix of run workflow ids. */
    public static final String RUN_PREFIX = "run-";
    /** Separates the tenant from the rest of the id. */
    public static final char SEPARATOR = '~';

    private static final Pattern TENANT = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,127}");

    private TenantWorkflowIds() {
    }

    /** The workflow id of run {@code runId} in {@code tenantId}. */
    public static String run(String tenantId, String runId) {
        Objects.requireNonNull(runId, "runId");
        return RUN_PREFIX + tenant(tenantId) + SEPARATOR + runId.trim();
    }

    /** A child workflow id in {@code tenantId}; {@code name} is the rest of the id. */
    public static String child(String tenantId, String name) {
        Objects.requireNonNull(name, "name");
        return tenant(tenantId) + SEPARATOR + name;
    }

    /** The tenant a workflow id names, or {@code null} when it names none. */
    public static String tenantOf(String workflowId) {
        if (workflowId == null) {
            return null;
        }
        String id = workflowId.startsWith(RUN_PREFIX) ? workflowId.substring(RUN_PREFIX.length()) : workflowId;
        int at = id.indexOf(SEPARATOR);
        if (at <= 0) {
            return null;
        }
        String tenant = id.substring(0, at);
        return TENANT.matcher(tenant).matches() ? tenant : null;
    }

    private static String tenant(String tenantId) {
        if (tenantId == null || !TENANT.matcher(tenantId).matches()) {
            throw new IllegalArgumentException("tenant id must be 1-128 characters [A-Za-z0-9._-]");
        }
        return tenantId;
    }
}
