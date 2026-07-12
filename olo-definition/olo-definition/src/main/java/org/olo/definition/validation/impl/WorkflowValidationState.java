/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package org.olo.definition.validation.impl;

import org.olo.definition.node.NodeDefinition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Mutable cross-validator state built incrementally during {@code WorkflowValidator.validate}.
 *
 * <p>Collects node identity maps, declared field names, and edge connection counts so later
 * validators can reference nodes and ports without re-scanning the workflow graph.
 */
final class WorkflowValidationState {

    final Set<String> nodeIds = new HashSet<>();
    final Map<String, NodeDefinition> nodesById = new HashMap<>();
    final Set<String> stateFieldNames = new HashSet<>();
    final Set<String> inputFieldNames = new HashSet<>();
    final Set<String> parameterFieldNames = new HashSet<>();
    final Set<String> providerIds = new HashSet<>();
    final Set<String> hookImplementationIds = new HashSet<>();
    final Map<String, Map<String, Integer>> outgoingCounts = new HashMap<>();
    final Map<String, Map<String, Integer>> incomingCounts = new HashMap<>();

    private WorkflowValidationState() {
    }

    static WorkflowValidationState create() {
        return new WorkflowValidationState();
    }
}
