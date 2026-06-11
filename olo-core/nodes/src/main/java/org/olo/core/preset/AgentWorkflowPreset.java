package org.olo.core.preset;

import org.olo.annotation.OloDesigner;
import org.olo.annotation.OloNodeShape;
import org.olo.annotation.OloWorkflowParameter;
import org.olo.spi.catalog.ParameterWidget;
import org.olo.annotation.OloWorkflowPreset;

/**
 * Authoritative {@code agent} workflow preset parameters.
 * <p>
 * Compiled into {@link org.olo.annotation.OloCatalogLocations#WORKFLOW_PRESETS_CATALOG} for Studio;
 * {@code agent.json} {@code parameters} inherit the same schema via
 * {@code org.olo.definition.parameter.WorkflowPresetParameters} in olo-definition (no second copy).
 */
@OloWorkflowPreset(
        id = "agent",
        designer = @OloDesigner(
                paletteGroup = "Agents",
                searchKeywords = {"planning", "task", "agent"},
                width = 300,
                height = 120,
                canvasShape = OloNodeShape.AGENT),
        parameters = {
            @OloWorkflowParameter(
                    name = "systemPrompt",
                    label = "System Prompt",
                    type = "string",
                    description = "System prompt for the agent",
                    help = "Optional instructions that shape agent behavior.",
                    placeholder = "You are a helpful assistant…",
                    group = "Model Settings",
                    order = 0,
                    defaultValue = "",
                    widget = ParameterWidget.TEXTAREA),
            @OloWorkflowParameter(
                    name = "maxIterations",
                    label = "Max Iterations",
                    type = "integer",
                    description = "Maximum agent reasoning iterations",
                    help = "Caps how many planner/tool loops the agent may run.",
                    group = "Model Settings",
                    order = 1,
                    defaultValue = "10",
                    minimum = 1,
                    maximum = 100,
                    step = 1,
                    widget = ParameterWidget.NUMBER),
            @OloWorkflowParameter(
                    name = "model",
                    label = "Model",
                    type = "string",
                    description = "Model identifier",
                    help = "LLM used for agent reasoning.",
                    group = "Model Settings",
                    order = 2,
                    required = true,
                    minLength = 1,
                    defaultValue = "",
                    widget = ParameterWidget.MODEL_SELECTOR),
            @OloWorkflowParameter(
                    name = "temperature",
                    label = "Temperature",
                    type = "number",
                    description = "Sampling temperature",
                    help = "Higher values increase response creativity.",
                    group = "Model Settings",
                    order = 3,
                    defaultValue = "0.2",
                    minimum = 0,
                    maximum = 2,
                    step = 0.1,
                    widget = ParameterWidget.SLIDER)
        })
public final class AgentWorkflowPreset {

    private AgentWorkflowPreset() {
    }
}
