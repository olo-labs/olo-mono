package org.olo.definition.parameter;

import org.olo.definition.planner.WorkflowPlannerPromptDefinition;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Agent workflow preset parameters ({@code agent.json} {@code parameters} block).
 * <p>
 * <strong>Single source:</strong> {@code org.olo.core.preset.AgentWorkflowPreset} ({@code @OloWorkflowPreset})
 * compiles to {@link org.olo.annotation.OloCatalogLocations#WORKFLOW_PRESETS_CATALOG}; this type materializes
 * the same schema for workflow JSON via {@link WorkflowPresetParameters}.
 */
public final class AgentWorkflowParameters {

    public static final String MAX_ITERATIONS = "maxIterations";
    public static final String SYSTEM_PROMPT = "systemPrompt";
    public static final String MODEL = "model";
    public static final String TEMPERATURE = "temperature";

    /** Default system prompt — {@code {message}} is expanded to the workflow input at runtime. */
    public static final String DEFAULT_SYSTEM_PROMPT = "{message}";

    private AgentWorkflowParameters() {}

    public static Map<String, WorkflowParameterDefinition> defaults() {
        return WorkflowPresetParameters.materialize(WorkflowPresetParameters.AGENT_PRESET_ID);
    }

    public static Map<String, WorkflowParameterDefinition> defaults(ClassLoader classLoader) {
        return WorkflowPresetParameters.materialize(WorkflowPresetParameters.AGENT_PRESET_ID, classLoader);
    }

    /** Preset parameters with a role-specific {@link #SYSTEM_PROMPT} default aligned to planner prompts. */
    public static Map<String, WorkflowParameterDefinition> forPreset(String presetId) {
        Map<String, WorkflowParameterDefinition> parameters = new LinkedHashMap<>(defaults());
        WorkflowParameterDefinition systemPrompt = parameters.get(SYSTEM_PROMPT);
        if (systemPrompt == null) {
            return Map.copyOf(parameters);
        }
        parameters.put(
                SYSTEM_PROMPT,
                WorkflowParameterDefinition.builder()
                        .type(systemPrompt.getType())
                        .label(systemPrompt.getLabel())
                        .description(systemPrompt.getDescription())
                        .defaultValue(WorkflowPlannerPromptDefinition.forPreset(presetId).getPromptTemplate())
                        .required(systemPrompt.getRequired())
                        .validation(systemPrompt.getValidation())
                        .visibleWhen(systemPrompt.getVisibleWhen())
                        .ui(systemPrompt.getUi())
                        .build());
        return Map.copyOf(parameters);
    }
}
