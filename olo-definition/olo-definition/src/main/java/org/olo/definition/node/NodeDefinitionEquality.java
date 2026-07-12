/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.node;

import java.util.Objects;

/**
 * Value equality and string representation for {@link NodeDefinition}.
 */
final class NodeDefinitionEquality {

    private NodeDefinitionEquality() {
    }

    static boolean equals(NodeDefinition left, Object other) {
        if (left == other) {
            return true;
        }
        if (!(other instanceof NodeDefinition that)) {
            return false;
        }
        return Objects.equals(left.getId(), that.getId())
                && Objects.equals(left.getType(), that.getType())
                && Objects.equals(left.getLabel(), that.getLabel())
                && Objects.equals(left.getCapability(), that.getCapability())
                && Objects.equals(left.getPorts(), that.getPorts())
                && Objects.equals(left.getExecution(), that.getExecution())
                && Objects.equals(left.getReads(), that.getReads())
                && Objects.equals(left.getWrites(), that.getWrites())
                && Objects.equals(left.getConfiguration(), that.getConfiguration())
                && Objects.equals(left.getHooks(), that.getHooks());
    }

    static int hashCode(NodeDefinition node) {
        return Objects.hash(
                node.getId(),
                node.getType(),
                node.getLabel(),
                node.getCapability(),
                node.getPorts(),
                node.getExecution(),
                node.getReads(),
                node.getWrites(),
                node.getConfiguration(),
                node.getHooks());
    }

    static String toString(NodeDefinition node) {
        return "NodeDefinition{id='" + node.getId() + "', type='" + node.getType() + "'}";
    }
}
