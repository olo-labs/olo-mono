package org.olo.kernel.agent.executor.impl;

import org.olo.definition.node.NodeDefinition;
import org.olo.definition.node.NodeType;
import org.olo.definition.toolcall.ToolCallPlannerSupport;
import org.olo.kernel.agent.executor.AgentExecutor;
import org.olo.kernel.childworkflow.ChildWorkflowInputs;
import org.olo.kernel.childworkflow.ChildWorkflowRunGateway;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.exception.KernelException;
import org.olo.kernel.toolcall.AgentCallResultsSupport;
import org.olo.kernel.toolcall.AvailableAgentsJsonResolver;
import org.olo.kernel.toolcall.ToolCallSubgraphMerger;
import org.olo.spi.node.NodeResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Executes planner-produced {@code agentCalls} as blocking child workflow runs.
 * <p>
 * Each child workflow is started from the Temporal parent workflow thread when available;
 * {@link ChildWorkflowRunGateway} blocks until every child completes before this node returns.
 * The injected dispatch node uses {@link org.olo.definition.execution.ExecutionModel#INLINE}
 * so child workflows are not started from a detached activity.
 */
public final class AgentCallDispatchExecutor implements AgentExecutor {

    public static final String EXECUTOR_ID = "agent-call-dispatch";
    public static final String CONFIG_AGENT_CALL_DISPATCH = "agentCallDispatch";
    public static final String CONFIG_PLANNER_NODE_ID = "plannerNodeId";
    public static final String CONFIG_AGENT_CALLS = "agentCalls";
    public static final String DEFAULT_AGENT_RESULTS_VARIABLE = ToolCallPlannerSupport.DEFAULT_AGENT_RESULTS_VARIABLE;

    @Override
    public String id() {
        return EXECUTOR_ID;
    }

    @Override
    public boolean supports(NodeDefinition node) {
        return node != null
                && NodeType.AGENT.name().equals(node.getType())
                && node.getConfiguration() != null
                && Boolean.TRUE.equals(node.getConfiguration().get(CONFIG_AGENT_CALL_DISPATCH));
    }

    @Override
    public NodeResult execute(KernelRuntimeContext context, NodeDefinition node) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(node, "node");
        if (context.getGraph() == null) {
            throw new KernelException("agent call dispatch requires an active workflow graph");
        }

        String plannerNodeId = readPlannerNodeId(node);
        List<ToolCallSubgraphMerger.ParsedAgentCall> agentCalls = readAgentCalls(node);
        List<Map<String, Object>> results = new ArrayList<>();

        for (ToolCallSubgraphMerger.ParsedAgentCall call : agentCalls) {
            if (AgentCallResultsSupport.completedAgentIds(context.getVariables()).contains(call.agentId())) {
                continue;
            }
            if (context.getGraph() != null && call.agentId().equals(context.getGraph().getId())) {
                throw new KernelException(
                        "agentCalls must not target the active workflow id: " + call.agentId());
            }
            if (!AvailableAgentsJsonResolver.isAllowedAgent(context.getGraph(), plannerNodeId, call.agentId())) {
                throw new KernelException("agentId is not in availableAgentsJson allow-list: " + call.agentId());
            }
            var childInput = ChildWorkflowInputs.forChildAgent(context, call.agentId(), call.message());
            String childResult = ChildWorkflowRunGateway.execute(context.getQueue(), call.agentId(), childInput);
            AgentCallResultsSupport.appendResult(
                    context.getVariables(), call.agentId(), call.message(), childResult);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("agentId", call.agentId());
            entry.put("message", call.message());
            entry.put("response", childResult);
            results.add(entry);
        }

        if (results.isEmpty()) {
            throw new KernelException("no pending agentCalls to dispatch for node: " + node.getId());
        }

        List<Map<String, Object>> mergedResults = AgentCallResultsSupport.readResults(context.getVariables());

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("agentResults", mergedResults);
        output.put("agentExecutor", EXECUTOR_ID);
        return NodeResult.completed("dispatched " + results.size() + " child workflow(s)", output);
    }

    private static String readPlannerNodeId(NodeDefinition node) {
        Object configured = node.getConfiguration().get(CONFIG_PLANNER_NODE_ID);
        if (configured != null) {
            String plannerNodeId = String.valueOf(configured).trim();
            if (!plannerNodeId.isBlank()) {
                return plannerNodeId;
            }
        }
        return ToolCallPlannerSupport.DEFAULT_PLANNER_NODE_ID;
    }

    @SuppressWarnings("unchecked")
    private static List<ToolCallSubgraphMerger.ParsedAgentCall> readAgentCalls(NodeDefinition node) {
        Object configured = node.getConfiguration().get(CONFIG_AGENT_CALLS);
        if (configured instanceof List<?> list) {
            List<ToolCallSubgraphMerger.ParsedAgentCall> calls = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof ToolCallSubgraphMerger.ParsedAgentCall call) {
                    calls.add(call);
                } else if (item instanceof Map<?, ?> map) {
                    String agentId = String.valueOf(map.get("agentId")).trim();
                    String message = map.containsKey("message") ? String.valueOf(map.get("message")) : null;
                    calls.add(new ToolCallSubgraphMerger.ParsedAgentCall(agentId, message));
                }
            }
            if (!calls.isEmpty()) {
                return calls;
            }
        }
        throw new KernelException("agent call dispatch node requires agentCalls configuration");
    }
}
