/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.definition.validation.impl;

import org.olo.definition.node.NodeDefinition;
import org.olo.definition.path.DataPath;
import org.olo.definition.path.DataPathParseResult;
import org.olo.definition.path.DataPathParser;
import org.olo.definition.path.PathRoot;

import java.util.List;
import java.util.Set;

/**
 * Validates node read/write data paths against declared state, input, and parameter fields.
 *
 * <p>Write paths must target the {@code state.} namespace. Read paths may reference any declared
 * root ({@code state.}, {@code input.}, {@code parameter.}) and must resolve to a known top-level
 * field name.
 */
final class WorkflowDataPathValidator {

    private WorkflowDataPathValidator() {
    }

    /** Validates all read and write paths declared on a single node. */
    static void validateNode(
            NodeDefinition node,
            Set<String> stateFieldNames,
            Set<String> inputFieldNames,
            Set<String> parameterFieldNames,
            List<String> errors) {
        String nodeId = node.getId();
        for (String read : node.getReads()) {
            validateDataPath(
                    nodeId, "read", read, false, stateFieldNames, inputFieldNames, parameterFieldNames, errors);
        }
        for (String write : node.getWrites()) {
            validateDataPath(
                    nodeId, "write", write, true, stateFieldNames, inputFieldNames, parameterFieldNames, errors);
        }
    }

    private static void validateDataPath(
            String nodeId,
            String accessKind,
            String pathLiteral,
            boolean write,
            Set<String> stateFieldNames,
            Set<String> inputFieldNames,
            Set<String> parameterFieldNames,
            List<String> errors) {
        DataPathParseResult parsed = DataPathParser.parse(pathLiteral);
        if (!parsed.isSuccess()) {
            errors.add("node " + nodeId + ": invalid " + accessKind + " path '" + pathLiteral + "': "
                    + parsed.error().orElse("parse error"));
            return;
        }
        DataPath path = parsed.path().orElseThrow();
        if (write && path.root() != PathRoot.STATE) {
            errors.add("node " + nodeId + ": write path must use state. namespace, found: " + path.literal());
            return;
        }
        String topLevel = path.topLevelName();
        boolean declared = switch (path.root()) {
            case STATE -> stateFieldNames.contains(topLevel);
            case INPUT -> inputFieldNames.contains(topLevel);
            case PARAMETER -> parameterFieldNames.contains(topLevel);
        };
        if (!declared) {
            errors.add("node " + nodeId + " " + accessKind + "s unknown " + path.root().prefix() + " field: "
                    + path.literal() + " (no " + path.root().prefix() + " field '" + topLevel + "')");
        }
    }
}
