/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

/**
 * Deterministic JSON-RPC ids for {@code tools/call}. ToolGate maps a retry with the same id and caller to the
 * same invocation, so a Temporal activity retry that repeats a call gets the finished result instead of
 * running the tool again. The arguments are part of the id, so a different call is a different invocation.
 */
final class ToolCallRequestIds {

    private static final ObjectMapper CANONICAL = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    private ToolCallRequestIds() {
    }

    /**
     * @param runId    the OLO run id
     * @param scope    workflow id and AGENT node id
     * @param pass     how many times this AGENT node has already run in this run
     * @param round    tool round within this turn
     * @param index    position of the call within the round
     */
    static String requestId(String runId, String scope, int pass, int round, int index, String toolName, JsonNode arguments) {
        String canonicalArguments;
        try {
            canonicalArguments = arguments == null
                    ? "{}"
                    : CANONICAL.writeValueAsString(CANONICAL.treeToValue(arguments, Map.class));
        } catch (Exception e) {
            canonicalArguments = String.valueOf(arguments);
        }
        String material = String.join("\u0000",
                runId == null ? "" : runId,
                scope,
                Integer.toString(pass),
                Integer.toString(round),
                Integer.toString(index),
                toolName,
                canonicalArguments);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(material.getBytes(StandardCharsets.UTF_8));
            return "olo-" + HexFormat.of().formatHex(digest, 0, 24);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
