/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.performancetriage;

import org.olo.definition.configuration.scenario.ScenarioActionToolsSupport;
import org.olo.definition.configuration.scenario.ScenarioPlannerSupport;
import org.olo.definition.configuration.scenario.ScenarioPlannerSupport.ScenarioAgentSpec;
import org.olo.definition.configuration.scenario.ScenarioPlannerSupport.ScenarioHumanActionSpec;
import org.olo.definition.configuration.scenario.ScenarioPlannerSupport.ScenarioToolSpec;
import org.olo.definition.configuration.scenario.ScenarioPromptSupport;
import org.olo.definition.validation.WorkflowValidator;
import org.olo.definition.workflow.WorkflowDefinition;

import java.util.List;

/** Programmatic builders for the {@code performance-triage} scenario collection. */
public final class PerformanceTriageDefinitions {

    public static final String QUEUE = "oloQueue2";
    public static final String ORCHESTRATOR_ID = "performance-triage-orchestrator";

    public static final String LATENCY_AGENT_ID = "latency-analysis-agent";
    public static final String RESOURCE_AGENT_ID = "resource-pressure-agent";
    public static final String TUNING_AGENT_ID = "optimization-agent";
    public static final String REPORT_AGENT_ID = "performance-report-agent";

    public static final String CPU_NODE_ID = "cpu-usage";
    public static final String CPU_TOOL_ID = "olo-core:cpu-usage";
    public static final String MEMORY_NODE_ID = "memory-usage";
    public static final String MEMORY_TOOL_ID = "olo-core:memory-usage";
    public static final String LATENCY_NODE_ID = "latency-metric";
    public static final String LATENCY_TOOL_ID = "olo-core:numeric-metric";

    static final String JSON_ONLY_PROMPT_TEMPLATE = ScenarioPromptSupport.plannerPromptHeader(
            "performance triage and latency investigation",
            """
            1. Signal collection — query CPU, memory, and latency metrics for the incident window (ISO-8601 startTime/endTime).
               Use olo-core:cpu-usage, olo-core:memory-usage, and olo-core:numeric-metric.
            2. Specialist analysis — delegate to child workflows:
               - latency-analysis-agent: interpret p95/p99 latency spikes and user impact
               - resource-pressure-agent: correlate CPU and memory pressure with the latency window
               - optimization-agent: recommend tuning actions (timeouts, pool sizes, circuit breakers)
            3. Final report — delegate to performance-report-agent with findings from all prior agents."""
                    + ScenarioPromptSupport.humanApprovedActionStep(
                            ScenarioActionToolsSupport.RESTART_CONTAINER_TOOL_ID,
                            "{ \"containerId\": \"payment-api-7f8c9d\", \"namespace\": \"production\" }",
                            "restart the degraded service container after triage completes"));

    private PerformanceTriageDefinitions() {
    }

    public static WorkflowDefinition orchestrator() {
        WorkflowDefinition workflow = ScenarioPlannerSupport.orchestratorBuilder(
                        ORCHESTRATOR_ID,
                        QUEUE,
                        "Performance Triage Orchestrator",
                        "Investigates latency spikes using mock metrics and specialist child workflows",
                        "⚡",
                        JSON_ONLY_PROMPT_TEMPLATE,
                        List.of(
                                new ScenarioAgentSpec(LATENCY_AGENT_ID, "Latency Analysis Agent"),
                                new ScenarioAgentSpec(RESOURCE_AGENT_ID, "Resource Pressure Agent"),
                                new ScenarioAgentSpec(TUNING_AGENT_ID, "Optimization Agent"),
                                new ScenarioAgentSpec(REPORT_AGENT_ID, "Performance Report Agent")),
                        List.of(cpuUsageTool(), memoryUsageTool(), latencyMetricTool(), ScenarioActionToolsSupport.restartContainerTool()),
                        new ScenarioHumanActionSpec(
                                "container restart", "containerId and namespace (e.g. payment-api-7f8c9d / production)"))
                .build();
        WorkflowValidator.validateOrThrow(workflow);
        return workflow;
    }

    public static WorkflowDefinition latencyAnalysisAgent() {
        return childAgent(LATENCY_AGENT_ID, "Latency Analysis Agent",
                "Interprets latency spikes and customer-facing impact", "📉", "latency", "slo");
    }

    public static WorkflowDefinition resourcePressureAgent() {
        return childAgent(RESOURCE_AGENT_ID, "Resource Pressure Agent",
                "Correlates CPU and memory pressure with degraded performance", "💾", "memory", "cpu");
    }

    public static WorkflowDefinition optimizationAgent() {
        return childAgent(TUNING_AGENT_ID, "Optimization Agent",
                "Recommends concrete tuning and capacity actions", "🛠️", "tuning", "optimization");
    }

    public static WorkflowDefinition performanceReportAgent() {
        return childAgent(REPORT_AGENT_ID, "Performance Report Agent",
                "Publishes SRE performance incident report with timeline and actions", "📊", "report", "sre");
    }

    private static WorkflowDefinition childAgent(
            String id, String name, String description, String emoji, String... keywords) {
        return ScenarioPlannerSupport.childAgentPreset(id, QUEUE, name, description, emoji, keywords);
    }

    private static ScenarioToolSpec cpuUsageTool() {
        return new ScenarioToolSpec(
                CPU_NODE_ID, CPU_TOOL_ID, "CPU Usage",
                "Returns CPU utilization samples for a time window",
                "Check CPU spike during payment gateway outage");
    }

    private static ScenarioToolSpec memoryUsageTool() {
        return new ScenarioToolSpec(
                MEMORY_NODE_ID, MEMORY_TOOL_ID, "Memory Usage",
                "Returns heap/memory usage samples for a time window",
                "Detect memory pressure during gateway timeout incident");
    }

    private static ScenarioToolSpec latencyMetricTool() {
        return new ScenarioToolSpec(
                LATENCY_NODE_ID, LATENCY_TOOL_ID, "Latency Metric",
                "Returns upstream latency samples (milliseconds) for a time window",
                "Inspect p95 latency spike during outage");
    }
}
