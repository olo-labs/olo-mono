package org.olo.kernel.agent.prompt.impl;

import org.olo.definition.planner.WorkflowPlannerPromptDefinition;
import org.olo.definition.workflow.WorkflowDefinition;
import org.olo.kernel.context.variables.WorkflowRuntimeVariables;
import org.olo.kernel.exception.KernelException;
import org.olo.kernel.agent.prompt.PromptRenderer;

import java.util.Map;

public final class WorkflowPromptRenderer implements PromptRenderer {

    @Override
    public String render(WorkflowDefinition graph, WorkflowRuntimeVariables variables) {
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

        return renderTemplate(template, variables.toMap());
    }

    public static String renderTemplate(String template, Map<String, Object> variables) {
        String rendered = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
                continue;
            }
            rendered = rendered.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return rendered;
    }
}
