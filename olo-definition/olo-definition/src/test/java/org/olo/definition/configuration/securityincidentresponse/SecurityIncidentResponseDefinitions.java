/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.securityincidentresponse;

import org.olo.definition.configuration.scenario.ScenarioActionToolsSupport;
import org.olo.definition.configuration.scenario.ScenarioPlannerSupport;
import org.olo.definition.configuration.scenario.ScenarioPlannerSupport.ScenarioAgentSpec;
import org.olo.definition.configuration.scenario.ScenarioPlannerSupport.ScenarioHumanActionSpec;
import org.olo.definition.configuration.scenario.ScenarioPlannerSupport.ScenarioToolSpec;
import org.olo.definition.configuration.scenario.ScenarioPromptSupport;
import org.olo.definition.validation.WorkflowValidator;
import org.olo.definition.workflow.WorkflowDefinition;

import java.util.List;

/** Programmatic builders for the {@code security-incident-response} scenario collection. */
public final class SecurityIncidentResponseDefinitions {

    public static final String QUEUE = "oloQueue2";
    public static final String ORCHESTRATOR_ID = "security-incident-orchestrator";

    public static final String THREAT_AGENT_ID = "threat-detection-agent";
    public static final String FORENSICS_AGENT_ID = "forensics-agent";
    public static final String CONTAINMENT_AGENT_ID = "containment-agent";
    public static final String SECURITY_REPORT_AGENT_ID = "security-report-agent";

    public static final String LOG_READER_NODE_ID = "log-reader";
    public static final String LOG_READER_TOOL_ID = "olo-core:log-reader";
    public static final String RECENT_CODE_NODE_ID = "recently-changed-code";
    public static final String RECENT_CODE_TOOL_ID = "olo-core:recently-changed-code";
    public static final String WEB_SEARCH_NODE_ID = "web-search";
    public static final String WEB_SEARCH_TOOL_ID = "olo-core:web-search";

    static final String JSON_ONLY_PROMPT_TEMPLATE = ScenarioPromptSupport.plannerPromptHeader(
            "security incident response and breach triage",
            """
            1. Detection — scan logs for suspicious activity (olo-core:log-reader with ISO-8601 window).
               Optionally search threat intel stubs (olo-core:web-search with query argument).
               Delegate to threat-detection-agent for structured threat classification.
            2. Forensics — delegate to forensics-agent; use olo-core:recently-changed-code (pullRequestNumber optional)
               to inspect recent changes that may explain the breach vector.
            3. Containment — delegate to containment-agent with recommended immediate actions.
            4. Executive report — delegate to security-report-agent with timeline, blast radius, and remediation plan."""
                    + ScenarioPromptSupport.humanApprovedActionStep(
                            ScenarioActionToolsSupport.RESTART_CONTAINER_TOOL_ID,
                            "{ \"containerId\": \"compromised-api-pod\", \"namespace\": \"production\" }",
                            "restart or isolate the compromised workload after containment review"));

    private SecurityIncidentResponseDefinitions() {
    }

    public static WorkflowDefinition orchestrator() {
        WorkflowDefinition workflow = ScenarioPlannerSupport.orchestratorBuilder(
                        ORCHESTRATOR_ID,
                        QUEUE,
                        "Security Incident Orchestrator",
                        "Triages security incidents using mock logs, code changes, and specialist child workflows",
                        "🛡️",
                        JSON_ONLY_PROMPT_TEMPLATE,
                        List.of(
                                new ScenarioAgentSpec(THREAT_AGENT_ID, "Threat Detection Agent"),
                                new ScenarioAgentSpec(FORENSICS_AGENT_ID, "Forensics Agent"),
                                new ScenarioAgentSpec(CONTAINMENT_AGENT_ID, "Containment Agent"),
                                new ScenarioAgentSpec(SECURITY_REPORT_AGENT_ID, "Security Report Agent")),
                        List.of(logReaderTool(), recentCodeTool(), webSearchTool(), ScenarioActionToolsSupport.restartContainerTool()),
                        new ScenarioHumanActionSpec(
                                "container restart", "containerId and namespace for the workload to restart or isolate"))
                .build();
        WorkflowValidator.validateOrThrow(workflow);
        return workflow;
    }

    public static WorkflowDefinition threatDetectionAgent() {
        return childAgent(THREAT_AGENT_ID, "Threat Detection Agent",
                "Classifies suspicious log patterns and attack indicators", "🚨", "threat", "detection");
    }

    public static WorkflowDefinition forensicsAgent() {
        return childAgent(FORENSICS_AGENT_ID, "Forensics Agent",
                "Traces breach vector using logs and recent code changes", "🔍", "forensics", "investigation");
    }

    public static WorkflowDefinition containmentAgent() {
        return childAgent(CONTAINMENT_AGENT_ID, "Containment Agent",
                "Recommends immediate containment and isolation steps", "🧱", "containment", "response");
    }

    public static WorkflowDefinition securityReportAgent() {
        return childAgent(SECURITY_REPORT_AGENT_ID, "Security Report Agent",
                "Publishes security incident report for stakeholders", "📋", "security", "report");
    }

    private static WorkflowDefinition childAgent(
            String id, String name, String description, String emoji, String... keywords) {
        return ScenarioPlannerSupport.childAgentPreset(id, QUEUE, name, description, emoji, keywords);
    }

    private static ScenarioToolSpec logReaderTool() {
        return new ScenarioToolSpec(
                LOG_READER_NODE_ID, LOG_READER_TOOL_ID, "Log Reader",
                "Reads application logs for suspicious or error activity",
                "Find ERROR entries in payment-service logs during incident window");
    }

    private static ScenarioToolSpec recentCodeTool() {
        return new ScenarioToolSpec(
                RECENT_CODE_NODE_ID, RECENT_CODE_TOOL_ID, "Recently Changed Code",
                "Lists recent PR/diff stubs that may relate to the incident",
                "Show code changes in payment-service in the last 24 hours");
    }

    private static ScenarioToolSpec webSearchTool() {
        return new ScenarioToolSpec(
                WEB_SEARCH_NODE_ID, WEB_SEARCH_TOOL_ID, "Web Search",
                "Placeholder threat-intel / advisory search (stub results)",
                "Search for CVE advisories related to payment gateway timeouts");
    }
}
