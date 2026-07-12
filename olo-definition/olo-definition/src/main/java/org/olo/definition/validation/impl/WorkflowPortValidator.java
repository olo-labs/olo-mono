/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.definition.validation.impl;

import org.olo.definition.edge.EdgeDefinition;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.port.PortDefinition;
import org.olo.definition.port.PortDirection;
import org.olo.definition.port.PortWireCompatibility;
import org.olo.definition.validation.SchemaCompatibility;
import org.olo.definition.workflow.WorkflowDefinition;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validates port declarations on nodes, edge port wiring, and connection count constraints.
 */
final class WorkflowPortValidator {

    private WorkflowPortValidator() {
    }

    /** Validates port declarations on a single node. */
    static void validateNodePorts(NodeDefinition node, List<String> errors) {
        if (node.getPorts().isEmpty()) {
            errors.add("node " + node.getId() + " requires at least one port");
            return;
        }
        Set<String> ids = new HashSet<>();
        for (PortDefinition port : node.getPorts()) {
            if (port == null) {
                errors.add("null port on node: " + node.getId());
                continue;
            }
            if (ValidationUtils.isBlank(port.getId())) {
                errors.add("port id is required on node: " + node.getId());
            } else if (!ids.add(port.getId())) {
                errors.add("duplicate port id '" + port.getId() + "' on node: " + node.getId());
            }
            if (ValidationUtils.isBlank(port.getName())) {
                errors.add("port name is required on node: " + node.getId());
            }
            if (port.getDirection() == null) {
                errors.add("port direction is required on node " + node.getId() + " port: " + port.getId());
            }
            if (ValidationUtils.isBlank(port.getSchema())) {
                errors.add("port '" + port.getId() + "' on node " + node.getId() + " requires a schema");
            }
            if (port.getMinConnections() < 0) {
                errors.add("port '" + port.getId() + "' on node " + node.getId() + " minConnections must be >= 0");
            }
            if (port.getMaxConnections() != null && port.getMaxConnections() < port.getMinConnections()) {
                errors.add("port '" + port.getId() + "' on node " + node.getId()
                        + " maxConnections must be >= minConnections");
            }
        }
    }

    /** Validates edge port endpoints and updates connection counts in state. */
    static void validateEdgePorts(
            EdgeDefinition edge,
            Map<String, NodeDefinition> nodesById,
            String prefix,
            WorkflowValidationState state,
            List<String> errors) {
        NodeDefinition source = nodesById.get(edge.getSourceNodeId());
        NodeDefinition target = nodesById.get(edge.getTargetNodeId());
        if (source == null) {
            return;
        }

        WorkflowPortResolution.PortResolution sourcePort = WorkflowPortResolution.resolvePort(
                edge.getSourcePortId(),
                WorkflowPortResolution.outputPorts(source),
                PortDirection.OUTPUT,
                "output",
                source.getId(),
                prefix,
                errors);

        if (target == null) {
            return;
        }

        WorkflowPortResolution.PortResolution targetPort = WorkflowPortResolution.resolvePort(
                edge.getTargetPortId(),
                WorkflowPortResolution.inputPorts(target),
                PortDirection.INPUT,
                "input",
                target.getId(),
                prefix,
                errors);

        if (sourcePort.port() != null && targetPort.port() != null) {
            boolean compatible = WorkflowPortResolution.usesWireTypeContract(sourcePort.port())
                            || WorkflowPortResolution.usesWireTypeContract(targetPort.port())
                    ? PortWireCompatibility.compatible(sourcePort.port(), targetPort.port())
                    : SchemaCompatibility.compatible(
                            sourcePort.port().getSchema(), targetPort.port().getSchema());
            if (!compatible) {
                errors.add(
                        prefix
                                + "wire type mismatch: output port '"
                                + sourcePort.port().getId()
                                + "' on node "
                                + source.getId()
                                + " ("
                                + PortWireCompatibility.wireType(sourcePort.port())
                                + ") is not compatible with input port '"
                                + targetPort.port().getId()
                                + "' on node "
                                + target.getId()
                                + " (accepts "
                                + String.join(", ", PortWireCompatibility.acceptTypes(targetPort.port()))
                                + ")");
            }
        }

        if (sourcePort.port() != null && edge.getSourceNodeId() != null) {
            ValidationUtils.incrementCount(state.outgoingCounts, edge.getSourceNodeId(), sourcePort.port().getId());
        }
        if (targetPort.port() != null && edge.getTargetNodeId() != null) {
            ValidationUtils.incrementCount(state.incomingCounts, edge.getTargetNodeId(), targetPort.port().getId());
        }
    }

    /** Checks each port's connection count against required, min, and max constraints. */
    static void validatePortConnectionCounts(
            NodeDefinition node, WorkflowValidationState state, List<String> errors) {
        for (PortDefinition port : node.getPorts()) {
            if (port == null || ValidationUtils.isBlank(port.getId())) {
                continue;
            }
            int count = port.getDirection() == PortDirection.OUTPUT
                    ? state.outgoingCounts.getOrDefault(node.getId(), Map.of()).getOrDefault(port.getId(), 0)
                    : state.incomingCounts.getOrDefault(node.getId(), Map.of()).getOrDefault(port.getId(), 0);
            if (port.isRequired() && count == 0) {
                errors.add("required port '" + port.getId() + "' on node " + node.getId() + " has no connections");
            }
            if (count < port.getMinConnections()) {
                errors.add(
                        "port '"
                                + port.getId()
                                + "' on node "
                                + node.getId()
                                + " has "
                                + count
                                + " connections but requires at least "
                                + port.getMinConnections());
            }
            if (port.getMaxConnections() != null && count > port.getMaxConnections()) {
                errors.add(
                        "port '"
                                + port.getId()
                                + "' on node "
                                + node.getId()
                                + " has "
                                + count
                                + " connections but allows at most "
                                + port.getMaxConnections());
            }
        }
    }
}
