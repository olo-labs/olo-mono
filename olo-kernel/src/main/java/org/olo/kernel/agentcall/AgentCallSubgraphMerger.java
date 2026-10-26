/* Copyright (c) 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
package org.olo.kernel.agentcall;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.kernel.agentcall.model.*;
import java.util.List;

/** Validates allow-listed agent delegation and inserts child workflows. */
public interface AgentCallSubgraphMerger {
    AgentCallValidationResult validate(String rawJson, String allowedAgentsJson);
    SubgraphMergeResult merge(WorkflowDefinition graph, String plannerNodeId,
            String continueNodeId, List<ParsedAgentCall> agentCalls);
}
