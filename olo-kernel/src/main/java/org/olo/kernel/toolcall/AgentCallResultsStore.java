/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.toolcall;

import org.olo.kernel.context.variables.WorkflowRuntimeVariables;
import org.olo.kernel.toolcall.model.ParsedAgentCall;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tracks child-agent delegation results to prevent re-dispatch loops.
 */
public interface AgentCallResultsStore {

    Set<String> completedAgentIds(WorkflowRuntimeVariables variables);

    List<ParsedAgentCall> filterPending(List<ParsedAgentCall> agentCalls, WorkflowRuntimeVariables variables);

    void appendResult(WorkflowRuntimeVariables variables, String agentId, String message, String response);

    List<Map<String, Object>> readResults(WorkflowRuntimeVariables variables);
}
