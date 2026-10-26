/* Copyright (c) 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
package org.olo.kernel.agentcall.impl;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.kernel.agentcall.AgentCallSubgraphMerger;
import org.olo.kernel.agentcall.model.*;
import java.util.List;

public final class DefaultAgentCallSubgraphMerger implements AgentCallSubgraphMerger {
    @Override public AgentCallValidationResult validate(String json, String allowedAgents) {
        return AgentCallSequenceValidator.validate(json, allowedAgents);
    }
    @Override public SubgraphMergeResult merge(WorkflowDefinition graph, String planner,
            String continuation, List<ParsedAgentCall> calls) {
        return AgentCallChainMerger.merge(graph, planner, continuation, calls);
    }
}
