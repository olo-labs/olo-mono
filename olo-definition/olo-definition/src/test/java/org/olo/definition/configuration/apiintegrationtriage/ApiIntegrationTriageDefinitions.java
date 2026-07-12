/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.apiintegrationtriage;

import org.olo.definition.configuration.scenario.ScenarioActionToolsSupport;
import org.olo.definition.configuration.scenario.ScenarioPlannerSupport;
import org.olo.definition.configuration.scenario.ScenarioPlannerSupport.ScenarioAgentSpec;
import org.olo.definition.configuration.scenario.ScenarioPlannerSupport.ScenarioHumanActionSpec;
import org.olo.definition.configuration.scenario.ScenarioPlannerSupport.ScenarioToolSpec;
import org.olo.definition.configuration.scenario.ScenarioPromptSupport;
import org.olo.definition.validation.WorkflowValidator;
import org.olo.definition.workflow.WorkflowDefinition;

import java.util.List;

/** Programmatic builders for the {@code api-integration-triage} scenario collection. */
public final class ApiIntegrationTriageDefinitions {

    public static final String QUEUE = "oloQueue2";
    public static final String ORCHESTRATOR_ID = "api-integration-orchestrator";

    public static final String ENDPOINT_PROBE_AGENT_ID = "endpoint-probe-agent";
    public static final String DEPENDENCY_ANALYSIS_AGENT_ID = "dependency-analysis-agent";
    public static final String ERROR_CORRELATION_AGENT_ID = "error-correlation-agent";
    public static final String INTEGRATION_REPORT_AGENT_ID = "integration-report-agent";

    public static final String HTTP_NODE_ID = "http-tool";
    public static final String HTTP_TOOL_ID = "olo-core:http-tool";
    public static final String LOG_READER_NODE_ID = "log-reader";
    public static final String LOG_READER_TOOL_ID = "olo-core:log-reader";
    public static final String WEB_SEARCH_NODE_ID = "web-search";
    public static final String WEB_SEARCH_TOOL_ID = "olo-core:web-search";

    static final String JSON_ONLY_PROMPT_TEMPLATE = ScenarioPromptSupport.plannerPromptHeader(
            "API integration health and dependency triage",
            """
            1. Endpoint probe — call olo-core:http-tool (url argument, e.g. payment gateway health URL) to check upstream availability.
               Delegate to endpoint-probe-agent to interpret HTTP status, latency, and body hints.
            2. Log correlation — use olo-core:log-reader with ISO-8601 startTime/endTime around the failure window.
               Delegate to error-correlation-agent to map HTTP failures to log signatures.
            3. Dependency context — use olo-core:web-search (query argument) for vendor status or CVE stubs; delegate to dependency-analysis-agent.
            4. Integration report — delegate to integration-report-agent with timeline, root cause hypothesis, and remediation steps."""
                    + ScenarioPromptSupport.humanApprovedActionStep(
                            ScenarioActionToolsSupport.RESTART_CONTAINER_TOOL_ID,
                            "{ \"containerId\": \"integration-gateway\", \"namespace\": \"production\" }",
                            "restart the integration gateway container after remediation is approved"));

    private ApiIntegrationTriageDefinitions() {
    }

    public static WorkflowDefinition orchestrator() {
        WorkflowDefinition workflow = ScenarioPlannerSupport.orchestratorBuilder(
                        ORCHESTRATOR_ID,
                        QUEUE,
                        "API Integration Orchestrator",
                        "Probes HTTP endpoints, correlates logs, and delegates integration triage to child workflows",
                        "🌐",
                        JSON_ONLY_PROMPT_TEMPLATE,
                        List.of(
                                new ScenarioAgentSpec(ENDPOINT_PROBE_AGENT_ID, "Endpoint Probe Agent"),
                                new ScenarioAgentSpec(DEPENDENCY_ANALYSIS_AGENT_ID, "Dependency Analysis Agent"),
                                new ScenarioAgentSpec(ERROR_CORRELATION_AGENT_ID, "Error Correlation Agent"),
                                new ScenarioAgentSpec(INTEGRATION_REPORT_AGENT_ID, "Integration Report Agent")),
                        List.of(httpTool(), logReaderTool(), webSearchTool(), ScenarioActionToolsSupport.restartContainerTool()),
                        new ScenarioHumanActionSpec(
                                "container restart", "containerId and namespace for the integration workload to restart"))
                .build();
        WorkflowValidator.validateOrThrow(workflow);
        return workflow;
    }

    public static WorkflowDefinition endpointProbeAgent() {
        return childAgent(ENDPOINT_PROBE_AGENT_ID, "Endpoint Probe Agent",
                "Interprets HTTP probe results and upstream availability", "📡", "http", "probe");
    }

    public static WorkflowDefinition dependencyAnalysisAgent() {
        return childAgent(DEPENDENCY_ANALYSIS_AGENT_ID, "Dependency Analysis Agent",
                "Assesses third-party dependency and vendor outage impact", "🔗", "dependency", "vendor");
    }

    public static WorkflowDefinition errorCorrelationAgent() {
        return childAgent(ERROR_CORRELATION_AGENT_ID, "Error Correlation Agent",
                "Correlates HTTP failures with application log patterns", "🔀", "logs", "correlation");
    }

    public static WorkflowDefinition integrationReportAgent() {
        return childAgent(INTEGRATION_REPORT_AGENT_ID, "Integration Report Agent",
                "Publishes integration incident summary for engineering stakeholders", "📋", "integration", "report");
    }

    private static WorkflowDefinition childAgent(
            String id, String name, String description, String emoji, String... keywords) {
        return ScenarioPlannerSupport.childAgentPreset(id, QUEUE, name, description, emoji, keywords);
    }

    private static ScenarioToolSpec httpTool() {
        return new ScenarioToolSpec(
                HTTP_NODE_ID, HTTP_TOOL_ID, "HTTP",
                "HTTP GET/POST client for probing upstream endpoints",
                "GET https://api.example.com/health during outage window");
    }

    private static ScenarioToolSpec logReaderTool() {
        return new ScenarioToolSpec(
                LOG_READER_NODE_ID, LOG_READER_TOOL_ID, "Log Reader",
                "Reads application logs for errors correlated with HTTP failures",
                "Find ConnectionTimeout errors during payment gateway outage");
    }

    private static ScenarioToolSpec webSearchTool() {
        return new ScenarioToolSpec(
                WEB_SEARCH_NODE_ID, WEB_SEARCH_TOOL_ID, "Web Search",
                "Stub search for vendor advisories and dependency status pages",
                "Search payment gateway provider status page outage");
    }
}
