/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.scenario;

import org.olo.definition.OloProductTerminology;

/** Shared planner prompt fragments for multi-agent scenario collections. */
public final class ScenarioPromptSupport {

    private ScenarioPromptSupport() {
    }

    public static String plannerPromptHeader(String roleDescription, String playbook) {
        return OloProductTerminology.agentRolePrompt(roleDescription)
                + ScenarioConversationPluginSupport.conversationContextPromptBlock()
                + """

                User request:
                {message}

                Available tools (strict allow-list — use only these toolId values):
                {availableToolsJson}

                Available agents (strict allow-list — delegate only to these agentId values):
                {availableAgentsJson}

                Prior agent results (JSON array — empty on first pass):
                {agentResultsJson}

                Investigation playbook (execute in order across planner passes):
                """
                + playbook
                + """

                Output rules (strict):
                1. Return ONLY a single JSON object — no markdown, no code fences, no commentary, no trailing text.
                2. Schema:
                {
                  "toolCalls": [],
                  "agentCalls": [
                    { "agentId": "<agent-id>", "message": "<task for child workflow>" }
                  ],
                  "directResponse": null
                }
                3. Each agentCall runs as a blocking child workflow (executorId=child-workflow) in order; execution then continues forward.
                4. Never repeat agentCalls for an agentId that already appears in agentResultsJson.
                5. If no tools or agents are needed, set both arrays to [] and put the final answer in "directResponse".

                If a previous attempt failed validation, fix it:
                {toolCallSequenceJsonValidationError}

                Respond with JSON only.""";
    }

    /**
     * Playbook step instructing the planner to invoke a human-approved mock action tool on the final pass.
     * The tool writes a JSONL entry to the mock action execution log; surface {@code confirmationId} and
     * {@code logPath} from tool results in {@code directResponse}.
     */
    public static String humanApprovedActionStep(String toolId, String argumentExample, String purpose) {
        return """

                Final step (human-approved mock action) — after investigation agents have completed:
                - Call """
                + toolId
                + " with arguments supplied by the operator at human-input (e.g. "
                + argumentExample
                + ").\n"
                + "  Purpose: "
                + purpose
                + "\n"
                + "  The tool records a mock execution entry; include confirmationId, logPath, and summary in directResponse.";
    }
}
