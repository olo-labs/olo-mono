/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.logrcaanalysis;

import org.olo.definition.OloProductTerminology;
import org.olo.definition.configuration.scenario.ScenarioConversationPluginSupport;
import org.olo.definition.configuration.scenario.ScenarioActionToolsSupport;
import org.olo.definition.configuration.scenario.ScenarioPlannerSupport;
import org.olo.definition.configuration.scenario.ScenarioPlannerSupport.ScenarioAgentSpec;
import org.olo.definition.configuration.scenario.ScenarioPlannerSupport.ScenarioHumanActionSpec;
import org.olo.definition.configuration.scenario.ScenarioPlannerSupport.ScenarioToolSpec;
import org.olo.definition.validation.WorkflowValidator;
import org.olo.definition.workflow.WorkflowDefinition;

import java.util.List;

/** Programmatic builders for the {@code log-rca-analysis} scenario collection. */
public final class LogRcaAnalysisDefinitions {

    public static final String QUEUE = "oloQueue2";
    public static final String ORCHESTRATOR_ID = "log-rca-orchestrator";

    public static final String LOG_FAILURE_AGENT_ID = "log-failure-agent";
    public static final String METRICS_RCA_AGENT_ID = "metrics-rca-agent";
    public static final String CODE_CHANGE_RCA_AGENT_ID = "code-change-rca-agent";
    public static final String INCIDENT_SUMMARY_AGENT_ID = "incident-summary-agent";

    public static final String LOG_READER_NODE_ID = "log-reader";
    public static final String LOG_READER_TOOL_ID = "olo-core:log-reader";
    public static final String CPU_USAGE_NODE_ID = "cpu-usage";
    public static final String CPU_USAGE_TOOL_ID = "olo-core:cpu-usage";
    public static final String RECENT_CODE_NODE_ID = "recently-changed-code";
    public static final String RECENT_CODE_TOOL_ID = "olo-core:recently-changed-code";

    static final String JSON_ONLY_PROMPT_TEMPLATE =
            """
            """
                    + OloProductTerminology.agentRolePrompt("log failure triage and root-cause analysis")
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
            1. Failure identification — scan application logs for errors, stack traces, and the incident time window.
               Prefer olo-core:log-reader with ISO-8601 startTime/endTime when the user mentions a time range.
               Optionally delegate to log-failure-agent for a structured failure signature and affected services.
            2. Root-cause analysis — after failures are identified, delegate to specialist child workflows:
               - metrics-rca-agent: correlate CPU/memory spikes and latency with the failure window
               - code-change-rca-agent: inspect recent deployments and code changes near the incident
               Run these as separate agentCalls (each executes as its own child workflow).
            3. Final summary — when failure findings and RCA hypotheses exist in agentResultsJson, delegate once to
               incident-summary-agent to publish an executive incident summary (timeline, root cause, impact, actions).
            4. Human-approved recovery — after the summary agent completes, call olo-core:restart-container with
               containerId and namespace from the operator's human-input approval. Include confirmationId and logPath
               from the tool result in directResponse.

            Output rules (strict):
            1. Return ONLY a single JSON object — no markdown, no code fences, no commentary, no trailing text.
            2. Schema:
            {
              "toolCalls": [
                { "toolId": "olo-core:log-reader", "arguments": { "startTime": "2026-06-14T14:30:00Z", "endTime": "2026-06-14T14:35:00Z" } }
              ],
              "agentCalls": [
                { "agentId": "log-failure-agent", "message": "Identify failure signatures from the incident logs" }
              ],
              "directResponse": null
            }
            3. Each agentCall runs as a blocking child workflow (executorId=child-workflow) in order; execution then continues forward.
            4. Never repeat agentCalls for an agentId that already appears in agentResultsJson.
            5. Do not call incident-summary-agent until log-failure-agent and at least one RCA agent have completed.
            6. If no tools or agents are needed, set both arrays to [] and put the final answer in "directResponse".

            If a previous attempt failed validation, fix it:
            {toolCallSequenceJsonValidationError}

            Respond with JSON only.""";

    private LogRcaAnalysisDefinitions() {
    }

    public static WorkflowDefinition orchestrator() {
        WorkflowDefinition workflow = ScenarioPlannerSupport.orchestratorBuilder(
                        ORCHESTRATOR_ID,
                        QUEUE,
                        "Log RCA Orchestrator",
                        "Plans log failure triage, delegates RCA to specialist child workflows, and publishes a final summary",
                        "🧯",
                        JSON_ONLY_PROMPT_TEMPLATE,
                        List.of(
                                new ScenarioAgentSpec(LOG_FAILURE_AGENT_ID, "Log Failure Agent"),
                                new ScenarioAgentSpec(METRICS_RCA_AGENT_ID, "Metrics RCA Agent"),
                                new ScenarioAgentSpec(CODE_CHANGE_RCA_AGENT_ID, "Code Change RCA Agent"),
                                new ScenarioAgentSpec(INCIDENT_SUMMARY_AGENT_ID, "Incident Summary Agent")),
                        List.of(logReaderTool(), cpuUsageTool(), recentlyChangedCodeTool(), ScenarioActionToolsSupport.restartContainerTool()),
                        new ScenarioHumanActionSpec(
                                "container restart", "containerId and namespace for the recovered workload"))
                .build();
        WorkflowValidator.validateOrThrow(workflow);
        return workflow;
    }

    public static WorkflowDefinition logFailureAgent() {
        return ScenarioPlannerSupport.childAgentPreset(
                LOG_FAILURE_AGENT_ID,
                QUEUE,
                "Log Failure Agent",
                "Identifies failure signatures, error bursts, and affected services from incident logs",
                "📋",
                "log",
                "failure",
                "triage");
    }

    public static WorkflowDefinition metricsRcaAgent() {
        return ScenarioPlannerSupport.childAgentPreset(
                METRICS_RCA_AGENT_ID,
                QUEUE,
                "Metrics RCA Agent",
                "Correlates CPU, memory, and latency metrics with the failure window to hypothesize root cause",
                "📈",
                "metrics",
                "rca",
                "observability");
    }

    public static WorkflowDefinition codeChangeRcaAgent() {
        return ScenarioPlannerSupport.childAgentPreset(
                CODE_CHANGE_RCA_AGENT_ID,
                QUEUE,
                "Code Change RCA Agent",
                "Analyzes recent deployments and code changes that may explain the incident",
                "🧩",
                "code",
                "deployment",
                "rca");
    }

    public static WorkflowDefinition incidentSummaryAgent() {
        return ScenarioPlannerSupport.childAgentPreset(
                INCIDENT_SUMMARY_AGENT_ID,
                QUEUE,
                "Incident Summary Agent",
                "Publishes the final incident summary with timeline, root cause, impact, and recommended actions",
                "📣",
                "summary",
                "incident",
                "report");
    }

    private static ScenarioToolSpec logReaderTool() {
        return new ScenarioToolSpec(
                LOG_READER_NODE_ID,
                LOG_READER_TOOL_ID,
                "Log Reader",
                "Reads application logs for errors and stack traces in a time window",
                "Find ERROR entries in payment-service logs between 14:30 and 14:35 UTC");
    }

    private static ScenarioToolSpec cpuUsageTool() {
        return new ScenarioToolSpec(
                CPU_USAGE_NODE_ID,
                CPU_USAGE_TOOL_ID,
                "CPU Usage",
                "Returns CPU utilization samples for observability correlation",
                "Check CPU spike during payment gateway outage");
    }

    private static ScenarioToolSpec recentlyChangedCodeTool() {
        return new ScenarioToolSpec(
                RECENT_CODE_NODE_ID,
                RECENT_CODE_TOOL_ID,
                "Recently Changed Code",
                "Lists recent commits and deployments near the incident window",
                "Show code changes in payment-service in the last 24 hours");
    }
}
