package org.olo.kernel.agent.prompt.impl;

import org.olo.definition.dynamicgraph.DynamicGraphPlannerSupport;
import org.olo.definition.node.NodeDefinition;
import org.olo.definition.planner.WorkflowPlannerPromptDefinition;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.kernel.context.variables.WorkflowRuntimeVariables;
import org.olo.kernel.exception.KernelException;
import org.olo.kernel.agent.prompt.PromptRenderer;

import java.util.Map;

public final class WorkflowPromptRenderer implements PromptRenderer {

    static final String CONFIG_PROMPT_TEMPLATE = "promptTemplate";

    static final String DEFAULT_INLINE_AGENT_PROMPT =
            """
            You are a helpful assistant.

            User message:
            {message}

            Respond in plain conversational text. Do not output JSON workflow graphs or code fences.""";

    @Override
    public String render(WorkflowDefinition graph, WorkflowRuntimeVariables variables) {
        return renderTemplate(resolveWorkflowPromptTemplate(graph), variables.toMap());
    }

    @Override
    public String renderForNode(
            WorkflowDefinition graph, NodeDefinition node, WorkflowRuntimeVariables variables) {
        if (DynamicGraphPlannerSupport.isDynamicGraphPlanner(node)) {
            return render(graph, variables);
        }
        return renderTemplate(resolveNodePromptTemplate(node), variables.toMap());
    }

    private static String resolveWorkflowPromptTemplate(WorkflowDefinition graph) {
        String promptId = graph.getDefaultPromptId();
        if (promptId == null || promptId.isBlank()) {
            throw new KernelException("workflow graph has no defaultPromptId: " + graph.getId());
        }

        WorkflowPlannerPromptDefinition promptDefinition = graph.getPrompts().stream()
                .filter(prompt -> prompt != null && promptId.equals(prompt.getId()))
                .findFirst()
                .orElseThrow(() -> new KernelException(
                        "workflow prompt '" + promptId + "' not found on graph: " + graph.getId()));

        String template = promptDefinition.getPromptTemplate();
        if (template == null || template.isBlank()) {
            throw new KernelException("workflow prompt '" + promptId + "' has empty template");
        }
        return template;
    }

    private static String resolveNodePromptTemplate(NodeDefinition node) {
        if (node != null && node.getConfiguration() != null) {
            Object configured = node.getConfiguration().get(CONFIG_PROMPT_TEMPLATE);
            if (configured instanceof String text && !text.isBlank()) {
                return text.trim();
            }
        }
        return DEFAULT_INLINE_AGENT_PROMPT;
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
