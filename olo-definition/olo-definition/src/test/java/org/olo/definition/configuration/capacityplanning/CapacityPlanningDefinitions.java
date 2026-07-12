/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.capacityplanning;

import org.olo.definition.configuration.scenario.ScenarioActionToolsSupport;
import org.olo.definition.configuration.scenario.ScenarioPlannerSupport;
import org.olo.definition.configuration.scenario.ScenarioPlannerSupport.ScenarioAgentSpec;
import org.olo.definition.configuration.scenario.ScenarioPlannerSupport.ScenarioHumanActionSpec;
import org.olo.definition.configuration.scenario.ScenarioPlannerSupport.ScenarioToolSpec;
import org.olo.definition.configuration.scenario.ScenarioPromptSupport;
import org.olo.definition.validation.WorkflowValidator;
import org.olo.definition.workflow.WorkflowDefinition;

import java.util.List;

/** Programmatic builders for the {@code capacity-planning} scenario collection. */
public final class CapacityPlanningDefinitions {

    public static final String QUEUE = "oloQueue2";
    public static final String ORCHESTRATOR_ID = "capacity-planning-orchestrator";

    public static final String UTILIZATION_AGENT_ID = "resource-utilization-agent";
    public static final String COST_ESTIMATION_AGENT_ID = "cost-estimation-agent";
    public static final String SCALING_AGENT_ID = "scaling-recommendation-agent";
    public static final String CAPACITY_REPORT_AGENT_ID = "capacity-report-agent";

    public static final String CPU_NODE_ID = "cpu-usage";
    public static final String CPU_TOOL_ID = "olo-core:cpu-usage";
    public static final String MEMORY_NODE_ID = "memory-usage";
    public static final String MEMORY_TOOL_ID = "olo-core:memory-usage";
    public static final String CALCULATOR_NODE_ID = "calculator";
    public static final String CALCULATOR_TOOL_ID = "olo-core:calculator";

    static final String JSON_ONLY_PROMPT_TEMPLATE = ScenarioPromptSupport.plannerPromptHeader(
            "capacity planning and cost estimation after an incident",
            """
            1. Utilization signals — query olo-core:cpu-usage and olo-core:memory-usage for the incident window (ISO-8601 startTime/endTime).
               Delegate to resource-utilization-agent to summarize peak utilization and headroom.
            2. Cost math — use olo-core:calculator (expression or a/b/op arguments) to estimate extra instance-hours or over-provision cost.
               Delegate to cost-estimation-agent with assumptions stated clearly.
            3. Scaling plan — delegate to scaling-recommendation-agent for autoscaling thresholds, pool sizes, and circuit breaker tuning.
            4. Capacity report — delegate to capacity-report-agent with executive summary, numbers, and recommended capacity changes."""
                    + ScenarioPromptSupport.humanApprovedActionStep(
                            ScenarioActionToolsSupport.RESTART_CONTAINER_TOOL_ID,
                            "{ \"containerId\": \"payment-api\", \"namespace\": \"production\" }",
                            "restart containers to apply the approved scaling or pool-size changes"));

    private CapacityPlanningDefinitions() {
    }

    public static WorkflowDefinition orchestrator() {
        WorkflowDefinition workflow = ScenarioPlannerSupport.orchestratorBuilder(
                        ORCHESTRATOR_ID,
                        QUEUE,
                        "Capacity Planning Orchestrator",
                        "Plans post-incident capacity using mock metrics, calculator, and specialist child workflows",
                        "📈",
                        JSON_ONLY_PROMPT_TEMPLATE,
                        List.of(
                                new ScenarioAgentSpec(UTILIZATION_AGENT_ID, "Resource Utilization Agent"),
                                new ScenarioAgentSpec(COST_ESTIMATION_AGENT_ID, "Cost Estimation Agent"),
                                new ScenarioAgentSpec(SCALING_AGENT_ID, "Scaling Recommendation Agent"),
                                new ScenarioAgentSpec(CAPACITY_REPORT_AGENT_ID, "Capacity Report Agent")),
                        List.of(cpuUsageTool(), memoryUsageTool(), calculatorTool(), ScenarioActionToolsSupport.restartContainerTool()),
                        new ScenarioHumanActionSpec(
                                "container restart", "containerId and namespace to apply approved capacity changes"))
                .build();
        WorkflowValidator.validateOrThrow(workflow);
        return workflow;
    }

    public static WorkflowDefinition resourceUtilizationAgent() {
        return childAgent(UTILIZATION_AGENT_ID, "Resource Utilization Agent",
                "Summarizes CPU and memory peaks versus baseline headroom", "📊", "utilization", "metrics");
    }

    public static WorkflowDefinition costEstimationAgent() {
        return childAgent(COST_ESTIMATION_AGENT_ID, "Cost Estimation Agent",
                "Estimates incremental infrastructure cost from utilization findings", "💰", "cost", "finance");
    }

    public static WorkflowDefinition scalingRecommendationAgent() {
        return childAgent(SCALING_AGENT_ID, "Scaling Recommendation Agent",
                "Recommends autoscaling, pool sizing, and resilience tuning", "⚖️", "scaling", "capacity");
    }

    public static WorkflowDefinition capacityReportAgent() {
        return childAgent(CAPACITY_REPORT_AGENT_ID, "Capacity Report Agent",
                "Publishes capacity planning report for leadership review", "📑", "capacity", "report");
    }

    private static WorkflowDefinition childAgent(
            String id, String name, String description, String emoji, String... keywords) {
        return ScenarioPlannerSupport.childAgentPreset(id, QUEUE, name, description, emoji, keywords);
    }

    private static ScenarioToolSpec cpuUsageTool() {
        return new ScenarioToolSpec(
                CPU_NODE_ID, CPU_TOOL_ID, "CPU Usage",
                "Returns CPU utilization samples for a time window",
                "Peak CPU during payment-service outage");
    }

    private static ScenarioToolSpec memoryUsageTool() {
        return new ScenarioToolSpec(
                MEMORY_NODE_ID, MEMORY_TOOL_ID, "Memory Usage",
                "Returns heap/memory usage samples for a time window",
                "Memory spike during gateway timeout incident");
    }

    private static ScenarioToolSpec calculatorTool() {
        return new ScenarioToolSpec(
                CALCULATOR_NODE_ID, CALCULATOR_TOOL_ID, "Calculator",
                "Evaluates arithmetic expressions for cost and capacity estimates",
                "Compute extra instance-hours: 4 instances * 2.5 hours");
    }
}
