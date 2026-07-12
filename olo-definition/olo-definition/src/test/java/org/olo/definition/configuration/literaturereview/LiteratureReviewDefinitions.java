/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.literaturereview;

import org.olo.definition.configuration.scenario.ScenarioActionToolsSupport;
import org.olo.definition.configuration.scenario.ScenarioPlannerSupport;
import org.olo.definition.configuration.scenario.ScenarioPlannerSupport.ScenarioAgentSpec;
import org.olo.definition.configuration.scenario.ScenarioPlannerSupport.ScenarioHumanActionSpec;
import org.olo.definition.configuration.scenario.ScenarioPlannerSupport.ScenarioToolSpec;
import org.olo.definition.configuration.scenario.ScenarioPromptSupport;
import org.olo.definition.validation.WorkflowValidator;
import org.olo.definition.workflow.WorkflowDefinition;

import java.util.List;

/** Programmatic builders for the {@code literature-review} scenario collection. */
public final class LiteratureReviewDefinitions {

    public static final String QUEUE = "oloQueue2";
    public static final String ORCHESTRATOR_ID = "literature-review-orchestrator";

    public static final String PAPER_DISCOVERY_AGENT_ID = "paper-discovery-agent";
    public static final String EVIDENCE_SYNTHESIS_AGENT_ID = "evidence-synthesis-agent";
    public static final String GAP_ANALYSIS_AGENT_ID = "gap-analysis-agent";
    public static final String RESEARCH_BRIEF_AGENT_ID = "research-brief-agent";

    public static final String RESEARCH_LITERATURE_NODE_ID = "research-literature";
    public static final String RESEARCH_LITERATURE_TOOL_ID = "olo-core:research-literature";
    public static final String WEB_SEARCH_NODE_ID = "web-search";
    public static final String WEB_SEARCH_TOOL_ID = "olo-core:web-search";

    static final String JSON_ONLY_PROMPT_TEMPLATE = ScenarioPromptSupport.plannerPromptHeader(
            "structured literature review and research brief production",
            """
            1. Paper discovery — use olo-core:research-literature with the user's topic (mock paper summaries).
               Delegate to paper-discovery-agent to list key papers, authors, and findings.
            2. Broader context — use olo-core:web-search (query argument) for recent news or survey stubs.
               Delegate to evidence-synthesis-agent to combine literature and web findings.
            3. Gap analysis — delegate to gap-analysis-agent to identify open questions and conflicting evidence.
            4. Research brief — delegate to research-brief-agent for an executive summary with citations and next steps."""
                    + ScenarioPromptSupport.humanApprovedActionStep(
                            ScenarioActionToolsSupport.CREATE_PULL_REQUEST_TOOL_ID,
                            "{ \"title\": \"docs: literature review brief\", \"headBranch\": \"research/literature-brief\" }",
                            "open a pull request to publish the approved research brief"));

    private LiteratureReviewDefinitions() {
    }

    public static WorkflowDefinition orchestrator() {
        WorkflowDefinition workflow = ScenarioPlannerSupport.orchestratorBuilder(
                        ORCHESTRATOR_ID,
                        QUEUE,
                        "Literature Review Orchestrator",
                        "Runs mock literature search and delegates synthesis to specialist child workflows",
                        "📚",
                        JSON_ONLY_PROMPT_TEMPLATE,
                        List.of(
                                new ScenarioAgentSpec(PAPER_DISCOVERY_AGENT_ID, "Paper Discovery Agent"),
                                new ScenarioAgentSpec(EVIDENCE_SYNTHESIS_AGENT_ID, "Evidence Synthesis Agent"),
                                new ScenarioAgentSpec(GAP_ANALYSIS_AGENT_ID, "Gap Analysis Agent"),
                                new ScenarioAgentSpec(RESEARCH_BRIEF_AGENT_ID, "Research Brief Agent")),
                        List.of(researchLiteratureTool(), webSearchTool(), ScenarioActionToolsSupport.createPullRequestTool()),
                        new ScenarioHumanActionSpec(
                                "publish pull request", "title and headBranch for the research brief PR"))
                .build();
        WorkflowValidator.validateOrThrow(workflow);
        return workflow;
    }

    public static WorkflowDefinition paperDiscoveryAgent() {
        return childAgent(PAPER_DISCOVERY_AGENT_ID, "Paper Discovery Agent",
                "Finds and ranks academic papers for the research question", "🔎", "papers", "discovery");
    }

    public static WorkflowDefinition evidenceSynthesisAgent() {
        return childAgent(EVIDENCE_SYNTHESIS_AGENT_ID, "Evidence Synthesis Agent",
                "Synthesizes findings across papers and supplementary sources", "🧩", "synthesis", "evidence");
    }

    public static WorkflowDefinition gapAnalysisAgent() {
        return childAgent(GAP_ANALYSIS_AGENT_ID, "Gap Analysis Agent",
                "Identifies research gaps, conflicts, and limitations in the evidence", "🕳️", "gaps", "analysis");
    }

    public static WorkflowDefinition researchBriefAgent() {
        return childAgent(RESEARCH_BRIEF_AGENT_ID, "Research Brief Agent",
                "Produces executive research brief with recommendations", "📝", "brief", "research");
    }

    private static WorkflowDefinition childAgent(
            String id, String name, String description, String emoji, String... keywords) {
        return ScenarioPlannerSupport.childAgentPreset(id, QUEUE, name, description, emoji, keywords);
    }

    private static ScenarioToolSpec researchLiteratureTool() {
        return new ScenarioToolSpec(
                RESEARCH_LITERATURE_NODE_ID, RESEARCH_LITERATURE_TOOL_ID, "Research Literature",
                "Returns mock academic paper summaries for a topic",
                "Find papers on AI safety alignment");
    }

    private static ScenarioToolSpec webSearchTool() {
        return new ScenarioToolSpec(
                WEB_SEARCH_NODE_ID, WEB_SEARCH_TOOL_ID, "Web Search",
                "Stub web search for surveys, preprints, and news",
                "Search recent surveys on large language model orchestration");
    }
}
