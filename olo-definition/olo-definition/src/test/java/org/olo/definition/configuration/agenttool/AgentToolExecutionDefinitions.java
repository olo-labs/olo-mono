package org.olo.definition.configuration.agenttool;

import org.olo.definition.capability.CapabilityDefinition;
import org.olo.definition.designer.StudioDesignerDefaults;
import org.olo.definition.execution.ExecutionKind;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.planner.AgentAvailableAgents;
import org.olo.definition.planner.PlannerContextDefinition;
import org.olo.definition.port.PortDirection;
import org.olo.definition.preset.WorkflowPresetInfrastructure;
import org.olo.definition.runtime.AgentDelegationPolicy;
import org.olo.definition.runtime.RuntimeBindingDefinition;
import org.olo.definition.tool.ToolDefinition;
import org.olo.definition.toolcall.ToolCallPlannerSupport;
import org.olo.definition.variable.VariableDefinition;
import org.olo.definition.variable.VariableScope;
import org.olo.definition.workflow.ChildWorkflowDefinition;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowDefinition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Programmatic builder for the {@code agent} workflow preset with strict JSON tool-call planning.
 */
public final class AgentToolExecutionDefinitions {

    public static final String WORKFLOW_ID = "agent";
    public static final String CALCULATOR_NODE_ID = "calculator";
    public static final String CPU_USAGE_NODE_ID = "cpu-usage";
    public static final String CALCULATOR_TOOL_ID = "olo-core:calculator";
    public static final String CPU_USAGE_TOOL_ID = "olo-core:cpu-usage";

    static final String JSON_ONLY_PROMPT_TEMPLATE =
            """
            You are an OLO tool-call planner.

            User request:
            {message}

            Available tools (strict allow-list — use only these toolId values):
            {availableToolsJson}

            Output rules (strict):
            1. Return ONLY a single JSON object — no markdown, no code fences, no commentary, no trailing text.
            2. Do not wrap the JSON in ```json``` blocks.
            3. Schema:
            {
              "toolCalls": [
                { "toolId": "olo-core:calculator", "arguments": { } }
              ],
              "directResponse": null
            }
            4. If no tools are needed, set "toolCalls": [] and put the final answer in "directResponse".
            5. If tools are needed, set "directResponse": null and list toolCalls in execution order.
            6. Each toolId MUST appear in availableToolsJson.
            7. Include arguments when a tool needs structured input (for example ISO-8601 startTime/endTime for observability tools).

            If a previous attempt failed validation, fix it:
            {toolCallSequenceJsonValidationError}

            Respond with JSON only.""";

    private AgentToolExecutionDefinitions() {
    }

    public static WorkflowDefinition agent() {
        String description = "Autonomous tool-using agent with strict JSON tool-call planning";
        String plannerNodeId = ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID;
        return WorkflowBuilder.create("Agent")
                .id(WORKFLOW_ID)
                .enabled(true)
                .isDefault(true)
                .role("Agent")
                .shortDescription(description)
                .emoji("🤖")
                .designer(StudioDesignerDefaults.studioAgentDesigner("🤖", "planning", "task", "agent"))
                .queue("oloQueue2")
                .workflowType("olo")
                .runAgain(true)
                .version("1.0.0")
                .executionModel(ExecutionModel.INLINE)
                .debuggable()
                .replayable()
                .capability(agentCapability(description))
                .withMessageContract()
                .variable(availableToolsVariable())
                .variable(toolCallSequenceVariable())
                .variable(toolResultsVariable())
                .variable(retryCountVariable())
                .variable(validationErrorVariable())
                .defaultLocalModelInfrastructure()
                .agentParameters()
                .agentPlannerMetadata()
                .agentAvailableAgents()
                .agentDelegation()
                .childWorkflow(ChildWorkflowDefinition.builder()
                        .workflowId("planner")
                        .workflowVersion("1.0.0")
                        .build())
                .childWorkflow(ChildWorkflowDefinition.builder()
                        .workflowId("fast")
                        .workflowVersion("1.0.0")
                        .build())
                .childWorkflow(ChildWorkflowDefinition.builder()
                        .workflowId("detailed")
                        .workflowVersion("1.0.0")
                        .build())
                .childWorkflow(ChildWorkflowDefinition.builder()
                        .workflowId("reviewer")
                        .workflowVersion("1.0.0")
                        .build())
                .tool(calculatorTool())
                .tool(cpuUsageTool())
                .startNodeWithMessageInput("start")
                .canvasToolNode(CALCULATOR_NODE_ID)
                .putNodeConfiguration(CALCULATOR_NODE_ID, Map.of("toolId", CALCULATOR_TOOL_ID))
                .canvasToolNode(CPU_USAGE_NODE_ID)
                .putNodeConfiguration(CPU_USAGE_NODE_ID, Map.of("toolId", CPU_USAGE_TOOL_ID))
                .addNode(toolCallPlannerNode(plannerNodeId))
                .endNode("end")
                .connect("start", "out", plannerNodeId, "in")
                .connect(plannerNodeId, "out", "end", "in")
                .connect(CALCULATOR_NODE_ID, "capabilities", plannerNodeId, "capabilities")
                .connect(CPU_USAGE_NODE_ID, "capabilities", plannerNodeId, "capabilities")
                .nodeCanvasLayout("start", 0)
                .nodeCanvasLayout(plannerNodeId, 1)
                .nodeCanvasLayout("end", 2)
                .metadata("description", description)
                .metadata("role", WORKFLOW_ID)
                .metadata(
                        ToolCallPlannerSupport.METADATA_DYNAMIC_TOOL_EXECUTION,
                        Map.of(
                                ToolCallPlannerSupport.METADATA_PLANNER_NODE_ID,
                                plannerNodeId,
                                ToolCallPlannerSupport.CONFIG_OUTPUT_VARIABLE,
                                ToolCallPlannerSupport.DEFAULT_OUTPUT_VARIABLE,
                                ToolCallPlannerSupport.METADATA_AVAILABLE_TOOLS_VARIABLE,
                                ToolCallPlannerSupport.DEFAULT_AVAILABLE_TOOLS_VARIABLE,
                                ToolCallPlannerSupport.CONFIG_CONTINUE_NODE_ID,
                                "end"))
                .metadata(PlannerContextDefinition.METADATA_KEY, agentPlannerContext())
                .withStandardReturnVariable()
                .build();
    }

    private static CapabilityDefinition agentCapability(String description) {
        return CapabilityDefinition.builder()
                .name("Agent")
                .description(description)
                .addTag("agent")
                .addInput("input")
                .addOutput("output")
                .addRequiredContext(WorkflowPresetInfrastructure.MESSAGE_VARIABLE)
                .addRequiredContext(ToolCallPlannerSupport.DEFAULT_AVAILABLE_TOOLS_VARIABLE)
                .addRequiredContext(ToolCallPlannerSupport.DEFAULT_OUTPUT_VARIABLE)
                .build();
    }

    private static Map<String, Object> agentPlannerContext() {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put(
                PlannerContextDefinition.SELECTED_VARIABLES,
                List.of(
                        WorkflowPresetInfrastructure.MESSAGE_VARIABLE,
                        ToolCallPlannerSupport.DEFAULT_AVAILABLE_TOOLS_VARIABLE,
                        ToolCallPlannerSupport.DEFAULT_OUTPUT_VARIABLE));
        context.put(PlannerContextDefinition.SELECTED_TOOLS, List.of(CALCULATOR_TOOL_ID, CPU_USAGE_TOOL_ID));
        context.put(PlannerContextDefinition.SELECTED_AGENTS, AgentAvailableAgents.agentPresetDefaults().stream()
                .map(ref -> ref.getId())
                .toList());
        context.put(PlannerContextDefinition.INJECT_CAPABILITIES, false);
        return Map.copyOf(context);
    }

    private static NodeDefinition toolCallPlannerNode(String nodeId) {
        return NodeDefinition.builder()
                .id(nodeId)
                .type(NodeType.AGENT.name())
                .label("Agent")
                .executionKind(ExecutionKind.ACTIVITY)
                .executionModel(ExecutionModel.INLINE)
                .addPort(WorkflowBuilder.messagePort("in", PortDirection.INPUT))
                .addPort(WorkflowBuilder.capabilitiesPort(PortDirection.INPUT))
                .addPort(WorkflowBuilder.agentPlugPort(PortDirection.INPUT))
                .addPort(WorkflowBuilder.messagePort("out", PortDirection.OUTPUT))
                .putConfiguration(ToolCallPlannerSupport.CONFIG_TOOL_CALL_PLANNER, true)
                .putConfiguration(
                        ToolCallPlannerSupport.CONFIG_OUTPUT_VARIABLE,
                        ToolCallPlannerSupport.DEFAULT_OUTPUT_VARIABLE)
                .putConfiguration(
                        ToolCallPlannerSupport.CONFIG_MAX_INVALID_JSON_RETRIES,
                        ToolCallPlannerSupport.DEFAULT_MAX_INVALID_JSON_RETRIES)
                .putConfiguration(ToolCallPlannerSupport.CONFIG_CONTINUE_NODE_ID, "end")
                .putConfiguration("promptTemplate", JSON_ONLY_PROMPT_TEMPLATE)
                .build();
    }

    private static ToolDefinition calculatorTool() {
        return ToolDefinition.builder()
                .id(CALCULATOR_NODE_ID)
                .capability(CapabilityDefinition.builder()
                        .name("Calculator")
                        .description("Basic arithmetic on two numbers")
                        .addExample("Compute order totals")
                        .build())
                .runtimeBinding(RuntimeBindingDefinition.builder()
                        .implementationId(CALCULATOR_TOOL_ID)
                        .build())
                .build();
    }

    private static ToolDefinition cpuUsageTool() {
        return ToolDefinition.builder()
                .id(CPU_USAGE_NODE_ID)
                .capability(CapabilityDefinition.builder()
                        .name("CPU Usage")
                        .description(
                                "Reads CPU usage metrics from CSV files in a configured folder filtered by time range")
                        .addExample("Check CPU spike during payment gateway outage")
                        .build())
                .runtimeBinding(RuntimeBindingDefinition.builder()
                        .implementationId(CPU_USAGE_TOOL_ID)
                        .build())
                .build();
    }

    private static VariableDefinition availableToolsVariable() {
        return VariableDefinition.builder()
                .name(ToolCallPlannerSupport.DEFAULT_AVAILABLE_TOOLS_VARIABLE)
                .type("string")
                .description("JSON array of tools connected on the canvas and registered in tools[]")
                .scope(VariableScope.LOCAL)
                .metadata(Map.of("role", "available-tools"))
                .build();
    }

    private static VariableDefinition toolCallSequenceVariable() {
        return VariableDefinition.builder()
                .name(ToolCallPlannerSupport.DEFAULT_OUTPUT_VARIABLE)
                .type("string")
                .description("Model-produced strict JSON tool call sequence")
                .scope(VariableScope.LOCAL)
                .metadata(Map.of("role", "tool-call-sequence"))
                .build();
    }

    private static VariableDefinition toolResultsVariable() {
        return VariableDefinition.builder()
                .name(ToolCallPlannerSupport.DEFAULT_TOOL_RESULTS_VARIABLE)
                .type("string")
                .description("Aggregated JSON results from executed tool nodes for synthesis")
                .scope(VariableScope.LOCAL)
                .build();
    }

    private static VariableDefinition retryCountVariable() {
        return VariableDefinition.builder()
                .name(ToolCallPlannerSupport.DEFAULT_RETRY_VARIABLE)
                .type("number")
                .description("Invalid JSON retry counter for the tool-call planner")
                .scope(VariableScope.LOCAL)
                .build();
    }

    private static VariableDefinition validationErrorVariable() {
        return VariableDefinition.builder()
                .name(ToolCallPlannerSupport.DEFAULT_VALIDATION_ERROR_VARIABLE)
                .type("string")
                .description("Last validation error from toolCallSequenceJson, injected into planner retries")
                .scope(VariableScope.LOCAL)
                .build();
    }
}
