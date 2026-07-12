/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.dynamicgraphcreation;

import org.olo.definition.OloProductTerminology;
import org.olo.definition.capability.CapabilityDefinition;
import org.olo.definition.designer.StudioDesignerDefaults;
import org.olo.definition.dynamicgraph.DynamicGraphPlannerSupport;
import org.olo.definition.execution.ExecutionKind;
import org.olo.definition.execution.ExecutionModel;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.port.PortDirection;
import org.olo.definition.preset.WorkflowPresetInfrastructure;
import org.olo.definition.variable.VariableDefinition;
import org.olo.definition.variable.VariableScope;
import org.olo.definition.workflow.WorkflowBuilder;
import org.olo.definition.workflow.WorkflowDefinition;

import java.util.Map;

/**
 * Programmatic builder for the {@code dynamic-graph-creation} workflow preset.
 */
public final class DynamicGraphCreationDefinitions {

    public static final String WORKFLOW_ID = "dynamic-graph-creation";
    public static final String FILE_NAME = WORKFLOW_ID;
    public static final String QUEUE = "oloQueue2";

    static final String JSON_ONLY_PROMPT_TEMPLATE =
            "You are an "
                    + OloProductTerminology.PRODUCT
                    + """
                     workflow graph generator.

                    Goal: produce a valid WorkflowDefinition graph for this request:
            {message}

            Output rules (strict):
            1. Return ONLY a single JSON object — no markdown, no code fences, no commentary, no trailing text.
            2. Do not wrap the JSON in ```json``` blocks.
            3. Every graph MUST include exactly one START node, one END node, and edges connecting START -> steps -> END.
            4. Node type must be one of: START, END, AGENT, TOOL, MODEL, PARALLEL, HUMAN, CONDITION, ROUTER, PLANNER, REFLECTION, EVALUATOR, VECTOR_SEARCH, MEMORY, WORKFLOW_REF.
               Never use pipe-separated placeholders.
            5. AGENT nodes answer the user in plain conversational text. Only this graph-planner step returns workflow JSON.
            6. Node ids must be unique kebab-case strings.
            7. Prefer the smallest valid graph that satisfies the request.
            8. TOOL nodes require configuration.toolId using olo-core tool ids, for example:
               olo-core:web-search, olo-core:log-reader, olo-core:cpu-usage, olo-core:memory-usage,
               olo-core:numeric-metric, olo-core:recently-changed-code, olo-core:calculator, olo-core:http-tool.
               For news or web lookup requests, prefer olo-core:web-search.
               For observability tools (log-reader, cpu-usage, memory-usage, numeric-metric), include
               configuration.arguments with ISO-8601 startTime and endTime when the user mentions a time window.
               Example: "arguments": { "startTime": "2026-06-14T14:30:00Z", "endTime": "2026-06-14T14:31:00Z" }

            Minimal valid example (follow this shape):
            {
              "id": "hello-workflow",
              "label": "Hello Workflow",
              "nodes": [
                { "id": "start", "type": "START" },
                { "id": "greet", "type": "AGENT" },
                { "id": "end", "type": "END" }
              ],
              "edges": [
                {
                  "sourceNodeId": "start",
                  "sourcePortId": "out",
                  "targetNodeId": "greet",
                  "targetPortId": "in"
                },
                {
                  "sourceNodeId": "greet",
                  "sourcePortId": "out",
                  "targetNodeId": "end",
                  "targetPortId": "in"
                }
              ]
            }

            If a previous attempt failed validation, fix it:
            {generatedGraphJsonValidationError}

            Respond with JSON only.""";

    private DynamicGraphCreationDefinitions() {
    }

    public static WorkflowDefinition dynamicGraphCreation() {
        String description =
                "Generates " + OloProductTerminology.WORKFLOW + " graphs as JSON-only structured output and expands them inline at runtime";
        String plannerNodeId = DynamicGraphPlannerSupport.DEFAULT_PLANNER_NODE_ID;
        return WorkflowBuilder.create("Dynamic Graph Creation")
                .id(WORKFLOW_ID)
                .enabled(true)
                .isDefault(true)
                .role("Dynamic Graph Creator")
                .shortDescription(description)
                .emoji("🧩")
                .designer(StudioDesignerDefaults.studioAgentDesigner(
                        "🧩", "dynamic", "graph", "workflow", "json"))
                .queue(QUEUE)
                .workflowType("olo")
                .runAgain(true)
                .version("1.0.0")
                .executionModel(ExecutionModel.INLINE)
                .capability(CapabilityDefinition.builder()
                        .name("Dynamic Graph Creation")
                        .description(description)
                        .addTag("dynamic-graph")
                        .addTag("json")
                        .addInput("input")
                        .addOutput("output")
                        .addRequiredContext(WorkflowPresetInfrastructure.MESSAGE_VARIABLE)
                        .addRequiredContext(DynamicGraphPlannerSupport.DEFAULT_OUTPUT_VARIABLE)
                        .build())
                .withMessageContract()
                .variable(VariableDefinition.builder()
                        .name(DynamicGraphPlannerSupport.DEFAULT_OUTPUT_VARIABLE)
                        .type("string")
                        .description("Model-produced workflow graph JSON")
                        .scope(VariableScope.LOCAL)
                        .metadata(Map.of("role", "generated-graph"))
                        .build())
                .variable(VariableDefinition.builder()
                        .name(DynamicGraphPlannerSupport.DEFAULT_RETRY_VARIABLE)
                        .type("number")
                        .description("Invalid JSON retry counter for the dynamic graph planner")
                        .scope(VariableScope.LOCAL)
                        .build())
                .variable(VariableDefinition.builder()
                        .name(DynamicGraphPlannerSupport.DEFAULT_VALIDATION_ERROR_VARIABLE)
                        .type("string")
                        .description("Last validation error from generatedGraphJson, injected into planner retries")
                        .scope(VariableScope.LOCAL)
                        .build())
                .defaultLocalModelInfrastructure()
                .baselineAgentParameters()
                .startNodeWithMessageInput("start")
                .addNode(dynamicGraphPlannerNode(plannerNodeId))
                .endNode("end")
                .connect("start", "out", plannerNodeId, "in")
                .connect(plannerNodeId, "out", "end", "in")
                .nodeCanvasLayout("start", 0)
                .nodeCanvasLayout(plannerNodeId, 1)
                .nodeCanvasLayout("end", 2)
                .metadata("description", description)
                .metadata("role", WORKFLOW_ID)
                .metadata(
                        "dynamicGraphCreation",
                        Map.of(
                                "plannerNodeId", plannerNodeId,
                                "outputVariable", DynamicGraphPlannerSupport.DEFAULT_OUTPUT_VARIABLE,
                                "continueNodeId", "end"))
                .metadata(
                        org.olo.definition.planner.PlannerContextDefinition.METADATA_KEY,
                        org.olo.definition.planner.PlannerContextDefinition.presetDefaults(WORKFLOW_ID))
                .withStandardReturnVariable()
                .build();
    }

    private static NodeDefinition dynamicGraphPlannerNode(String nodeId) {
        return NodeDefinition.builder()
                .id(nodeId)
                .type(NodeType.AGENT.name())
                .executionKind(ExecutionKind.ACTIVITY)
                .executionModel(ExecutionModel.INLINE)
                .addPort(WorkflowBuilder.messagePort("in", PortDirection.INPUT))
                .addPort(WorkflowBuilder.capabilitiesPort(PortDirection.INPUT))
                .addPort(WorkflowBuilder.agentPlugPort(PortDirection.INPUT))
                .addPort(WorkflowBuilder.messagePort("out", PortDirection.OUTPUT))
                .putConfiguration(DynamicGraphPlannerSupport.CONFIG_DYNAMIC_GRAPH_PLANNER, true)
                .putConfiguration(
                        DynamicGraphPlannerSupport.CONFIG_OUTPUT_VARIABLE,
                        DynamicGraphPlannerSupport.DEFAULT_OUTPUT_VARIABLE)
                .putConfiguration(
                        DynamicGraphPlannerSupport.CONFIG_MAX_INVALID_JSON_RETRIES,
                        DynamicGraphPlannerSupport.DEFAULT_MAX_INVALID_JSON_RETRIES)
                .putConfiguration(DynamicGraphPlannerSupport.CONFIG_CONTINUE_NODE_ID, "end")
                .putConfiguration("promptTemplate", JSON_ONLY_PROMPT_TEMPLATE)
                .build();
    }
}
