/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.traversal.spi.impl;

import org.olo.core.node.CoreNodeTypes;
import org.olo.kernel.traversal.spi.NodeTypeResolver;

public final class CoreNodeTypeResolver implements NodeTypeResolver {

    @Override
    public String resolve(String definitionNodeType) {
        if (definitionNodeType == null || definitionNodeType.isBlank()) {
            return definitionNodeType;
        }
        return switch (definitionNodeType) {
            case "PROMPT" -> CoreNodeTypes.PROMPT;
            case "AGENT" -> CoreNodeTypes.AGENT;
            case "PARALLEL" -> CoreNodeTypes.PARALLEL;
            case "LOOP" -> CoreNodeTypes.LOOP;
            case "SWITCH" -> CoreNodeTypes.SWITCH;
            case "APPROVAL" -> CoreNodeTypes.APPROVAL;
            default -> definitionNodeType;
        };
    }
}
