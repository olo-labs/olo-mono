package org.olo.definition.configuration.scenario;

import org.olo.definition.capability.CapabilityDefinition;
import org.olo.definition.designer.StudioDesignerDefaults;
import org.olo.definition.execution.ExecutionKind;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.planner.PlannerContextDefinition;
import org.olo.definition.port.PortDirection;
import org.olo.definition.preset.WorkflowPresetInfrastructure;
import org.olo.definition.runtime.RuntimeBindingDefinition;
import org.olo.definition.tool.ToolDefinition;
import org.olo.definition.toolcall.ToolCallPlannerSupport;
import org.olo.definition.variable.VariableDefinition;
import org.olo.definition.variable.VariableScope;
import org.olo.definition.workflow.ChildWorkflowDefinition;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.definition.workflow.WorkflowReferenceDefinition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shared builders for scenario planner collections (orchestrator + child agent presets). */
public final class ScenarioPlannerSupport {

    private ScenarioPlannerSupport() {
    }

    public static WorkflowDefinition childAgentPreset(
            String workflowId,
            String queue,
            String name,
            String shortDescription,
            String emoji,
            String... searchKeywords) {
        return WorkflowBuilder.create(name)
                .id(workflowId)
                .enabled(true)
                .isDefault(false)
                .role(name)
                .shortDescription(shortDescription)
                .emoji(emoji)
                .designer(StudioDesignerDefaults.studioAgentDesigner(emoji, searchKeywords))
                .queue(queue)
                .workflowType("olo")
                .runAgain(true)
                .version("1.0.0")
                .executionModel(ExecutionModel.INLINE)
                .capability(CapabilityDefinition.builder()
                        .name(name)
                        .description(shortDescription)
                        .addTag(workflowId)
                        .addInput("input")
                        .addOutput("output")
                        .addRequiredContext(WorkflowPresetInfrastructure.MESSAGE_VARIABLE)
                        .build())
                .withMessageContract()
                .defaultLocalModelInfrastructure()
                .presetPlannerContext(workflowId)
                .agentParameters(workflowId)
                .localAgentCanvasPipeline(workflowId)
                .metadata("description", shortDescription)
                .metadata("role", workflowId)
                .withStandardReturnVariable()
                .build();
    }

    public static WorkflowBuilder orchestratorBuilder(
            String workflowId,
            String queue,
            String name,
            String shortDescription,
            String emoji,
            String promptTemplate,
            List<ScenarioAgentSpec> childAgents,
            List<ScenarioToolSpec> tools) {
        String plannerNodeId = ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID;
        WorkflowBuilder builder = WorkflowBuilder.create(name)
                .id(workflowId)
                .enabled(true)
                .isDefault(true)
                .role(name)
                .shortDescription(shortDescription)
                .emoji(emoji)
                .designer(StudioDesignerDefaults.studioAgentDesigner(emoji, workflowId, "planner", "scenario"))
                .queue(queue)
                .workflowType("olo")
                .runAgain(true)
                .version("1.0.0")
                .executionModel(ExecutionModel.INLINE)
                .debuggable()
                .replayable()
                .capability(orchestratorCapability(name, shortDescription))
                .withMessageContract()
                .variable(availableToolsVariable())
                .variable(availableAgentsVariable())
                .variable(agentResultsVariable())
                .variable(toolCallSequenceVariable())
                .variable(toolResultsVariable())
                .variable(retryCountVariable())
                .variable(validationErrorVariable())
                .defaultLocalModelInfrastructure()
                .agentParameters(workflowId)
                .agentPlannerMetadata()
                .agentDelegation()
                .startNodeWithMessageInput("start")
                .addNode(toolCallPlannerNode(plannerNodeId, promptTemplate))
                .endNode("end")
                .connect("start", "out", plannerNodeId, "in")
                .connect(plannerNodeId, "out", "end", "in")
                .nodeCanvasLayout("start", 0)
                .nodeCanvasLayout(plannerNodeId, 1)
                .nodeCanvasLayout("end", 2)
                .metadata("description", shortDescription)
                .metadata("role", workflowId)
                .metadata(
                        ToolCallPlannerSupport.METADATA_DYNAMIC_TOOL_EXECUTION,
                        Map.of(
                                ToolCallPlannerSupport.METADATA_PLANNER_NODE_ID,
                                plannerNodeId,
                                ToolCallPlannerSupport.CONFIG_OUTPUT_VARIABLE,
                                ToolCallPlannerSupport.DEFAULT_OUTPUT_VARIABLE,
                                ToolCallPlannerSupport.METADATA_AVAILABLE_TOOLS_VARIABLE,
                                ToolCallPlannerSupport.DEFAULT_AVAILABLE_TOOLS_VARIABLE,
                                ToolCallPlannerSupport.METADATA_AVAILABLE_AGENTS_VARIABLE,
                                ToolCallPlannerSupport.DEFAULT_AVAILABLE_AGENTS_VARIABLE,
                                ToolCallPlannerSupport.CONFIG_CONTINUE_NODE_ID,
                                "end"))
                .metadata(PlannerContextDefinition.METADATA_KEY, orchestratorPlannerContext(childAgents, tools))
                .withStandardReturnVariable();

        int agentColumn = 0;
        for (ScenarioAgentSpec childAgent : childAgents) {
            builder.childWorkflow(ChildWorkflowDefinition.builder()
                    .workflowId(childAgent.workflowId())
                    .workflowVersion("1.0.0")
                    .build());
            builder.availableAgent(childAgent.workflowId());
            builder.canvasChildAgentPluginNode(childAgent.workflowId(), childAgent.workflowId(), childAgent.label());
            builder.connect(childAgent.workflowId(), "agentPlug", plannerNodeId, "agentPlug");
            builder.nodeCanvasLayout(childAgent.workflowId(), agentColumn++);
        }

        int toolColumn = agentColumn;
        for (ScenarioToolSpec tool : tools) {
            builder.tool(scenarioTool(tool));
            builder.canvasToolNode(tool.nodeId());
            builder.putNodeConfiguration(tool.nodeId(), Map.of("toolId", tool.implementationId()));
            builder.connect(tool.nodeId(), "capabilities", plannerNodeId, "capabilities");
            builder.nodeCanvasLayout(tool.nodeId(), toolColumn++);
        }

        return builder;
    }

    public static ToolDefinition scenarioTool(ScenarioToolSpec spec) {
        return ToolDefinition.builder()
                .id(spec.nodeId())
                .capability(CapabilityDefinition.builder()
                        .name(spec.name())
                        .description(spec.description())
                        .addExample(spec.example())
                        .build())
                .runtimeBinding(RuntimeBindingDefinition.builder()
                        .implementationId(spec.implementationId())
                        .build())
                .build();
    }

    private static CapabilityDefinition orchestratorCapability(String name, String description) {
        return CapabilityDefinition.builder()
                .name(name)
                .description(description)
                .addTag("agent")
                .addTag("scenario")
                .addInput("input")
                .addOutput("output")
                .addRequiredContext(WorkflowPresetInfrastructure.MESSAGE_VARIABLE)
                .addRequiredContext(ToolCallPlannerSupport.DEFAULT_AVAILABLE_TOOLS_VARIABLE)
                .addRequiredContext(ToolCallPlannerSupport.DEFAULT_OUTPUT_VARIABLE)
                .build();
    }

    private static Map<String, Object> orchestratorPlannerContext(
            List<ScenarioAgentSpec> childAgents, List<ScenarioToolSpec> tools) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put(
                PlannerContextDefinition.SELECTED_VARIABLES,
                List.of(
                        WorkflowPresetInfrastructure.MESSAGE_VARIABLE,
                        ToolCallPlannerSupport.DEFAULT_AVAILABLE_TOOLS_VARIABLE,
                        ToolCallPlannerSupport.DEFAULT_AVAILABLE_AGENTS_VARIABLE,
                        ToolCallPlannerSupport.DEFAULT_AGENT_RESULTS_VARIABLE,
                        ToolCallPlannerSupport.DEFAULT_OUTPUT_VARIABLE));
        context.put(PlannerContextDefinition.SELECTED_TOOLS, tools.stream().map(ScenarioToolSpec::implementationId).toList());
        context.put(
                PlannerContextDefinition.SELECTED_AGENTS,
                childAgents.stream().map(ScenarioAgentSpec::workflowId).toList());
        context.put(PlannerContextDefinition.INJECT_CAPABILITIES, false);
        context.put(PlannerContextDefinition.INJECT_AGENTS, true);
        return Map.copyOf(context);
    }

    private static NodeDefinition toolCallPlannerNode(String nodeId, String promptTemplate) {
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
                .putConfiguration("promptTemplate", promptTemplate)
                .build();
    }

    private static VariableDefinition availableAgentsVariable() {
        return VariableDefinition.builder()
                .name(ToolCallPlannerSupport.DEFAULT_AVAILABLE_AGENTS_VARIABLE)
                .type("string")
                .description("JSON array of child agents connected on the canvas agentPlug port")
                .scope(VariableScope.LOCAL)
                .metadata(Map.of("role", "available-agents"))
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

    private static VariableDefinition agentResultsVariable() {
        return VariableDefinition.builder()
                .name(ToolCallPlannerSupport.DEFAULT_AGENT_RESULTS_VARIABLE)
                .type("string")
                .description("Aggregated JSON results from child agent workflows delegated by the planner")
                .scope(VariableScope.LOCAL)
                .metadata(Map.of("role", "agent-results"))
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

    public record ScenarioAgentSpec(String workflowId, String label) {
    }

    public record ScenarioToolSpec(
            String nodeId,
            String implementationId,
            String name,
            String description,
            String example) {
    }
}
