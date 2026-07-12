/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.definition.validation.impl;

import org.olo.definition.error.ErrorRoute;
import org.olo.definition.error.OnFailureDefinition;
import org.olo.definition.error.RetryPolicy;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeRouterDefinition;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates node routers and onFailure retry/route configuration.
 */
final class WorkflowNodeFailureValidator {

    private WorkflowNodeFailureValidator() {
    }

    static void validateNodeRouters(NodeDefinition node, Set<String> nodeIds, List<String> errors) {
        Set<String> routerIds = new HashSet<>();
        for (NodeRouterDefinition router : node.getRouters()) {
            if (router == null) {
                errors.add("router entry must not be null on node: " + node.getId());
                continue;
            }
            if (!ValidationUtils.isBlank(router.getId()) && !routerIds.add(router.getId())) {
                errors.add("duplicate router id '" + router.getId() + "' on node: " + node.getId());
            }
            if (!ValidationUtils.isBlank(router.getTargetNodeId()) && !nodeIds.contains(router.getTargetNodeId())) {
                errors.add(
                        "router on node "
                                + node.getId()
                                + " references unknown target node: "
                                + router.getTargetNodeId());
            }
        }
    }

    static void validateOnFailure(NodeDefinition node, Set<String> nodeIds, List<String> errors) {
        OnFailureDefinition onFailure = node.getOnFailure();
        if (onFailure == null) {
            return;
        }
        String nodeId = node.getId();
        if (onFailure.getRetry() == null && onFailure.getRoute() == null) {
            errors.add("onFailure on node " + nodeId + " must declare retry and/or route");
            return;
        }
        RetryPolicy retry = onFailure.getRetry();
        if (retry != null) {
            if (retry.getAttempts() < 1) {
                errors.add("onFailure retry attempts must be >= 1 on node: " + nodeId);
            }
            if (retry.getInitialDelayMs() != null && retry.getInitialDelayMs() < 0) {
                errors.add("onFailure retry initialDelayMs must be >= 0 on node: " + nodeId);
            }
            if (retry.getMaxDelayMs() != null && retry.getMaxDelayMs() < 0) {
                errors.add("onFailure retry maxDelayMs must be >= 0 on node: " + nodeId);
            }
        }
        ErrorRoute route = onFailure.getRoute();
        if (route != null) {
            if (ValidationUtils.isBlank(route.getTargetNodeId())) {
                errors.add("onFailure route targetNodeId is required on node: " + nodeId);
            } else if (!nodeIds.contains(route.getTargetNodeId())) {
                errors.add(
                        "onFailure route on node "
                                + nodeId
                                + " references unknown target node: "
                                + route.getTargetNodeId());
            } else if (route.getTargetNodeId().equals(nodeId)) {
                errors.add("onFailure route on node " + nodeId + " must not target the same node");
            }
        }
    }
}
