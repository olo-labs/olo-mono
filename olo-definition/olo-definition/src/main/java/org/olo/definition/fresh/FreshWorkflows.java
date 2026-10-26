/* Copyright (c) 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
package org.olo.definition.fresh;

import org.olo.definition.capability.CapabilityDefinition;
import org.olo.definition.execution.*;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.port.PortDirection;
import org.olo.definition.runtime.*;
import org.olo.definition.orchestration.*;
import org.olo.definition.variable.*;
import org.olo.definition.workflow.*;
import org.olo.definition.planner.WorkflowPlannerPromptDefinition;
import java.util.*;

/** New-install definitions. Platform memory/RAG belong to the governed lifecycle, outside the graph. */
public final class FreshWorkflows {
    public static final List<String> ROLES = List.of("agent", "planner", "architect", "reviewer", "ask",
            "fast", "detailed", "strict", "summary", "teacher", "debug");
    public static final List<String> INCIDENT_AGENTS = List.of("triage-agent", "remediation-agent", "incident-report-agent");
    private FreshWorkflows() {}

    private static WorkflowBuilder base(String id, String label) {
        return WorkflowBuilder.create(label).id(id).label(label).version("1.0.0")
                .queue("oloQueue2").workflowType("olo").enabled(true).isDefault(id.equals("agent"))
                .withMessageContract().withStandardReturnVariable().defaultLocalModelInfrastructure()
                .runtime(WorkflowRuntimeDefinition.builder().contractVersion("1.0")
                        .executionModel(ExecutionModel.INLINE).build())
                .capability(CapabilityDefinition.builder().id(id).name(label)
                        .description("User-authorized governed workflow")
                        .requiredInputs(List.of("message")).requiredOutputs(List.of("ReturnValue")).build())
                .metadata("returnVariable", "ReturnValue").metadata("returnOutputKey", "agent");
    }

    public static WorkflowDefinition chat(String id) {
        return base(id, id).governedAgentCanvasPipeline(id)
                .putNodeConfiguration("agent", Map.of("promptTemplate",
                        WorkflowPlannerPromptDefinition.forPreset(id).getPromptTemplate()))
                .build();
    }

    private static NodeDefinition inline(String id, String workflow, Map<String,Object> configuration) {
        return NodeDefinition.builder().id(id).label(id).type("AGENT")
                .executionKind(ExecutionKind.ACTIVITY).executionModel(ExecutionModel.INLINE)
                .workflow(WorkflowReferenceDefinition.builder().workflowId(workflow).version("1.0.0").build())
                .configuration(configuration)
                .addPort(WorkflowBuilder.messagePort("in",PortDirection.INPUT))
                .addPort(WorkflowBuilder.messagePort("out",PortDirection.OUTPUT))
                .addPort(WorkflowBuilder.agentPlugPort(PortDirection.INPUT)).build();
    }

    public static WorkflowDefinition incident() {
        String id="incident-recovery";
        var builder=base(id,"Incident recovery").startNodeWithMessageInput("start")
                .addNode(inline("commander",id,Map.of("agentCallPlanner",true,"maxToolRounds",0,
                        "outputVariable","agentCallSequenceJson","continueNodeId","summary",
                        "promptTemplate","Delegate incident triage, remediation and reporting in order. "
                                + "Select only available agent IDs and return agentCalls JSON.\n"
                                + "{availableAgentsJson}\n{message}")))
                .addNode(inline("summary",id,Map.of("maxToolRounds",0,"promptTemplate",
                        "Summarize the child-agent results and retain uncertainty. {agentResultsJson}")))
                .endNode("end").connect("start","out","commander","in")
                .connect("commander","out","summary","in")
                .connect("summary","out","end","in").metadata("returnOutputKey","summary")
                .metadata("agentSelectionStrategy","dynamic")
                .delegation(RuntimeDelegationDefinition.builder().enabled(true).parallelEnabled(false)
                        .maxDepth(3).maxDelegations(3).resultAggregation(ResultAggregation.MERGE)
                        .memoryScope(MemoryScope.SHARED).build());
        for(var role:INCIDENT_AGENTS) {
            builder.canvasChildAgentPluginNode("delegate-"+role,role,role)
                    .connect("delegate-"+role,"agentPlug","commander","agentPlug")
                    .childWorkflowRef(role).availableAgent(role);
        }
        for(var name:List.of("agentCallSequenceJson","availableAgentsJson","agentResultsJson",
                "agentCallSequenceJsonValidationError"))
            builder.variable(VariableDefinition.builder().name(name).type("string").scope(VariableScope.LOCAL).build());
        builder.variable(VariableDefinition.builder().name("agentCallSequenceJsonRetryCount").type("integer")
                .scope(VariableScope.LOCAL).defaultValue(0).build());
        return builder.build();
    }

    public static WorkflowDefinition incidentChild(String role) {
        if(!INCIDENT_AGENTS.contains(role)) throw new IllegalArgumentException("Unknown incident role");
        return base(role,role).isChildWorkflow(true).governedAgentCanvasPipeline(role)
                .putNodeConfiguration("agent",Map.of("agentName",role,"maxToolRounds",5,
                        "promptTemplate","Complete your delegated incident stage using only the authorized ToolGate catalog. {message}"))
                .build();
    }

    public static Map<String,WorkflowDefinition> installation() {
        var definitions=new LinkedHashMap<String,WorkflowDefinition>();
        for(var role:ROLES) definitions.put("default/"+role+".json",chat(role));
        definitions.put("governed/governed-chat.json",chat("governed-chat"));
        definitions.put("current-active/agent.json",chat("agent"));
        definitions.put("incident-recovery/incident-recovery.json",incident());
        for(var role:INCIDENT_AGENTS) definitions.put("incident-recovery/"+role+".json",incidentChild(role));
        return Collections.unmodifiableMap(definitions);
    }
}
