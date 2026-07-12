/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.researchplanner;

import org.olo.definition.OloProductTerminology;
import org.olo.definition.configuration.scenario.ScenarioPlannerSupport;
import org.olo.definition.configuration.scenario.ScenarioPlannerSupport.ScenarioAgentSpec;
import org.olo.definition.configuration.scenario.ScenarioPlannerSupport.ScenarioToolSpec;
import org.olo.definition.validation.WorkflowValidator;
import org.olo.definition.workflow.WorkflowDefinition;

import java.util.List;

/** Programmatic builders for the {@code research-planner} scenario collection. */
public final class ResearchPlannerDefinitions {

    public static final String QUEUE = "oloQueue2";
    public static final String ORCHESTRATOR_ID = "research-orchestrator";
    public static final String LITERATURE_AGENT_ID = "literature-agent";
    public static final String SYNTHESIS_AGENT_ID = "synthesis-agent";
    public static final String RESEARCH_LITERATURE_NODE_ID = "research-literature";
    public static final String RESEARCH_LITERATURE_TOOL_ID = "olo-core:research-literature";

    static final String JSON_ONLY_PROMPT_TEMPLATE =
            """
            """
                    + OloProductTerminology.agentRolePrompt("research planning")
                    + """

            User request:
            {message}

            Available tools (strict allow-list — use only these toolId values; empty means do not call tools):
            {availableToolsJson}

            Available agents (strict allow-list — delegate only to these agentId values):
            {availableAgentsJson}

            Prior agent results (JSON array — empty on first pass):
            {agentResultsJson}

            Output rules (strict):
            1. Return ONLY a single JSON object — no markdown, no code fences, no commentary, no trailing text.
            2. Schema:
            {
              "toolCalls": [],
              "agentCalls": [
                { "agentId": "literature-agent", "message": "Find papers on the user topic" }
              ],
              "directResponse": null
            }
            3. When availableAgentsJson is non-empty, return agentCalls to delegate work to child workflows.
               Each agentCall runs as a blocking child workflow (executorId=child-workflow) in order; execution then continues forward (not back to this planner).
            4. Use toolCalls only when availableToolsJson is non-empty and a listed tool is the best fit.
            5. Use literature-agent for literature lookup and synthesis-agent to combine findings into a brief (both are child workflows, not inline synthesis).
            6. Never repeat agentCalls for an agentId that already appears in agentResultsJson.
            7. If no agents or tools are needed, set both arrays to [] and put the final answer in "directResponse".

            If a previous attempt failed validation, fix it:
            {toolCallSequenceJsonValidationError}

            Respond with JSON only.""";

    private ResearchPlannerDefinitions() {
    }

    public static WorkflowDefinition orchestrator() {
        WorkflowDefinition workflow = ScenarioPlannerSupport.orchestratorBuilder(
                        ORCHESTRATOR_ID,
                        QUEUE,
                        "Research Orchestrator",
                        "Plans research tasks, queries literature, and delegates to specialist agents",
                        "🔬",
                        JSON_ONLY_PROMPT_TEMPLATE,
                        List.of(
                                new ScenarioAgentSpec(LITERATURE_AGENT_ID, "Literature Agent"),
                                new ScenarioAgentSpec(SYNTHESIS_AGENT_ID, "Synthesis Agent")),
                        List.of())
                .build();
        WorkflowValidator.validateOrThrow(workflow);
        return workflow;
    }

    public static WorkflowDefinition literatureAgent() {
        return ScenarioPlannerSupport.childAgentPreset(
                LITERATURE_AGENT_ID,
                QUEUE,
                "Literature Agent",
                "Finds and summarizes academic papers for a research topic",
                "📚",
                "literature",
                "research",
                "papers");
    }

    public static WorkflowDefinition synthesisAgent() {
        return ScenarioPlannerSupport.childAgentPreset(
                SYNTHESIS_AGENT_ID,
                QUEUE,
                "Synthesis Agent",
                "Combines findings into an executive research brief",
                "🧠",
                "synthesis",
                "research",
                "brief");
    }

    private static ScenarioToolSpec researchLiteratureTool() {
        return new ScenarioToolSpec(
                RESEARCH_LITERATURE_NODE_ID,
                RESEARCH_LITERATURE_TOOL_ID,
                "Research Literature",
                "Returns mock academic paper summaries for research planner scenarios",
                "Find papers on renewable energy storage");
    }
}
