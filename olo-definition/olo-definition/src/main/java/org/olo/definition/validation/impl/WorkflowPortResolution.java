/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.definition.validation.impl;

import org.olo.definition.node.NodeDefinition;
import org.olo.definition.port.PortDefinition;
import org.olo.definition.port.PortDirection;

import java.util.List;

/**
 * Resolves port ids on edges and filters input/output port lists for a node.
 */
final class WorkflowPortResolution {

    private WorkflowPortResolution() {
    }

    static List<PortDefinition> inputPorts(NodeDefinition node) {
        return node.getPorts().stream()
                .filter(port -> port.getDirection() == PortDirection.INPUT)
                .toList();
    }

    static List<PortDefinition> outputPorts(NodeDefinition node) {
        return node.getPorts().stream()
                .filter(port -> port.getDirection() == PortDirection.OUTPUT)
                .toList();
    }

    /**
     * Resolves a port id against the declared ports on a node, inferring the sole port when omitted.
     */
    static PortResolution resolvePort(
            String portId,
            List<PortDefinition> ports,
            PortDirection expectedDirection,
            String kind,
            String nodeId,
            String prefix,
            List<String> errors) {
        if (ports.isEmpty()) {
            return PortResolution.unresolved(portId);
        }

        String resolvedId = portId;
        if (ValidationUtils.isBlank(resolvedId)) {
            if (ports.size() == 1) {
                resolvedId = ports.get(0).getId();
            } else {
                errors.add(
                        prefix
                                + "sourcePortId/targetPortId is required on node "
                                + nodeId
                                + " (declares "
                                + ports.size()
                                + " "
                                + kind
                                + " ports)");
                return PortResolution.unresolved(null);
            }
        }

        for (PortDefinition port : ports) {
            if (port != null && resolvedId.equals(port.getId())) {
                if (port.getDirection() != expectedDirection) {
                    errors.add(
                            prefix
                                    + "port '"
                                    + resolvedId
                                    + "' on node "
                                    + nodeId
                                    + " must be "
                                    + expectedDirection.value());
                }
                return new PortResolution(resolvedId, port);
            }
        }
        errors.add(
                prefix
                        + "unknown "
                        + kind
                        + " port '"
                        + resolvedId
                        + "' on node "
                        + nodeId);
        return PortResolution.unresolved(resolvedId);
    }

    static boolean usesWireTypeContract(PortDefinition port) {
        return (port.getType() != null && !port.getType().isBlank())
                || (port.getAcceptType() != null && !port.getAcceptType().isBlank());
    }

    record PortResolution(String id, PortDefinition port) {

        static PortResolution unresolved(String id) {
            return new PortResolution(id, null);
        }
    }
}
