package org.olo.kernel.agent.prompt.impl;

import org.olo.definition.node.NodeDefinition;
import org.olo.definition.parameter.AgentWorkflowParameters;
import org.olo.definition.parameter.WorkflowParameterDefinition;
import org.olo.definition.planner.WorkflowPlannerPromptDefinition;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.kernel.context.variables.WorkflowRuntimeVariables;
import org.olo.kernel.exception.KernelException;
import org.olo.kernel.agent.prompt.PromptRenderer;

import java.util.Map;

public final class WorkflowPromptRenderer implements PromptRenderer {

    static final String CONFIG_PROMPT_TEMPLATE = "promptTemplate";

    static final String DEFAULT_INLINE_AGENT_PROMPT =
            WorkflowPlannerPromptDefinition.forPreset("agent").getPromptTemplate();

    @Override
    public String render(WorkflowDefinition graph, WorkflowRuntimeVariables variables) {
        NodeDefinition node = graph.getNodes().stream()
                .filter(candidate -> candidate != null && "AGENT".equals(candidate.getType()))
                .findFirst()
                .orElseThrow(() -> new KernelException("workflow graph has no AGENT node: " + graph.getId()));
        return renderForNode(graph, node, variables);
    }

    @Override
    public String renderForNode(
            WorkflowDefinition graph, NodeDefinition node, WorkflowRuntimeVariables variables) {
        return renderTemplate(resolveNodePromptTemplate(graph, node), variables.toMap());
    }

    private static String resolveNodePromptTemplate(WorkflowDefinition graph, NodeDefinition node) {
        if (node != null && node.getConfiguration() != null) {
            Object configured = node.getConfiguration().get(CONFIG_PROMPT_TEMPLATE);
            if (configured instanceof String text && !text.isBlank()) {
                return text.trim();
            }
            Object nodeSystemPrompt = node.getConfiguration().get(AgentWorkflowParameters.SYSTEM_PROMPT);
            if (nodeSystemPrompt instanceof String text && !text.isBlank()) {
                return text.trim();
            }
        }

        String workflowSystemPrompt = readWorkflowParameterString(graph, AgentWorkflowParameters.SYSTEM_PROMPT);
        if (!workflowSystemPrompt.isBlank()) {
            return workflowSystemPrompt;
        }

        return DEFAULT_INLINE_AGENT_PROMPT;
    }

    private static String readWorkflowParameterString(WorkflowDefinition graph, String parameterId) {
        if (graph.getParameters() == null || graph.getParameters().isEmpty()) {
            return "";
        }
        WorkflowParameterDefinition parameter = graph.getParameters().get(parameterId);
        if (parameter == null || parameter.getDefaultValue() == null) {
            return "";
        }
        String value = String.valueOf(parameter.getDefaultValue()).trim();
        return value.isBlank() ? "" : value;
    }

    public static String renderTemplate(String template, Map<String, Object> variables) {
        String rendered = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            String replacement = entry.getValue() == null ? "" : String.valueOf(entry.getValue());
            rendered = rendered.replace("{" + entry.getKey() + "}", replacement);
        }
        return rendered;
    }
}
